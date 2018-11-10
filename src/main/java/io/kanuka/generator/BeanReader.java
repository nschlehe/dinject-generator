package io.kanuka.generator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

class BeanReader {

  private final TypeElement beanType;

  private final ProcessingContext processingContext;

  private String name;

  private MethodReader injectConstructor;

  private final List<MethodReader> otherConstructors = new ArrayList<>();

  private Element postConstructMethod;

  private Element preDestroyMethod;

  private final List<FieldReader> injectFields = new ArrayList<>();

  BeanReader(TypeElement beanType, ProcessingContext processingContext) {
    this.beanType = beanType;
    this.processingContext = processingContext;
  }

  TypeElement getBeanType() {
    return beanType;
  }

  String getName() {
    return name;
  }


  Element getPostConstructMethod() {
    return postConstructMethod;
  }

  Element getPreDestroyMethod() {
    return preDestroyMethod;
  }

  List<FieldReader> getInjectFields() {
    return injectFields;
  }

  void read() {

    String beanFullName = beanType.getQualifiedName().toString();
    processingContext.logNote("read bean [" + beanFullName + "]");

    readNamed();

    for (Element element : beanType.getEnclosedElements()) {
      ElementKind kind = element.getKind();
      switch (kind) {
        case CONSTRUCTOR:
          readConstructor(element);
          break;
        case FIELD:
          readField(element);
          break;
        case METHOD:
          readMethod(element);
          break;
      }
    }

  }

  List<String> getDependsOn() {

    List<String> list = new ArrayList<>();

    MethodReader methodReader = obtainConstructor();
    if (methodReader != null) {
      List<MethodReader.MethodParam> params = methodReader.getParams();
      for (MethodReader.MethodParam param : params) {
        list.add(param.getDependsOn());
      }
    }

    return list;
  }

  List<String> getInterfaces() {

    List<? extends TypeMirror> interfaces = beanType.getInterfaces();

    List<String> list = new ArrayList<>(interfaces.size());
    for (TypeMirror anInterface : interfaces) {
      list.add(anInterface.toString());
    }
    return list;
  }

  String getInterfacesAndAnnotations() {

    StringBuilder sb = new StringBuilder();

    List<? extends TypeMirror> interfaces = beanType.getInterfaces();
    for (TypeMirror anInterface : interfaces) {
      sb.append(",\"").append(anInterface.toString()).append("\"");
    }

    // get class level annotations (that are not Named and Singleton)
    for (AnnotationMirror annotationMirror : beanType.getAnnotationMirrors()) {
      String annotationType = annotationMirror.getAnnotationType().toString();
      if (includeAnnotation(annotationType)) {
        sb.append(",\"").append(annotationType).append("\"");
      }
    }

    return sb.toString();
  }

  private boolean includeAnnotation(String annotationType) {
    return !"javax.inject.Singleton".equals(annotationType)
      && !"javax.inject.Named".equals(annotationType);
  }

  private void readNamed() {
    Named named = beanType.getAnnotation(Named.class);
    if (named != null) {
      this.name = named.value();
    }
  }

  private void readConstructor(Element element) {

    try {

      ExecutableElement ex = (ExecutableElement) element;

      MethodReader methodReader = new MethodReader(ex);
      methodReader.read();

      Inject inject = element.getAnnotation(Inject.class);
      processingContext.logNote("readConstructor " + element.getSimpleName() + " " + element + " inject:" + (inject != null));
      if (inject != null) {
        injectConstructor = methodReader;
      } else {
        otherConstructors.add(methodReader);
      }

    } catch (Exception e) {
      processingContext.logNote("readConstructor Exception: " + e);
    }
  }

  private void readField(Element element) {
    Inject inject = element.getAnnotation(Inject.class);
    if (inject != null) {
      injectFields.add(new FieldReader(element));
    }
  }

  private void readMethod(Element element) {

    PostConstruct pcMarker = element.getAnnotation(PostConstruct.class);
    if (pcMarker != null) {
      postConstructMethod = element;
    }

    PreDestroy pdMarker = element.getAnnotation(PreDestroy.class);
    if (pdMarker != null) {
      preDestroyMethod = element;
    }

  }

  String getSimpleName() {
    return beanType.getSimpleName().toString();
  }

  boolean isLifecycleRequired() {
    return postConstructMethod != null || preDestroyMethod != null;
  }

  MetaData createMeta() {
    MetaData metaData = new MetaData(beanType.getQualifiedName().toString());
    metaData.update(this);
    return metaData;
  }

  MethodReader obtainConstructor() {
    if (injectConstructor != null) {
      return injectConstructor;
    }
    if (otherConstructors.size() == 1) {
      return otherConstructors.get(0);
    }
    return null;
  }

  boolean isFieldInjectionRequired() {
    return !injectFields.isEmpty();
  }
}
