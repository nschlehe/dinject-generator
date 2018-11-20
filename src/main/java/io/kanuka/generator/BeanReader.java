package io.kanuka.generator;

import io.kanuka.Bean;
import io.kanuka.Factory;

import javax.annotation.Generated;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

class BeanReader {

  /**
   * Annotations that we don't bother registering lists for.
   */
  private static Set<String> EXCLUDED_ANNOTATIONS = new HashSet<>();

  static {
    EXCLUDED_ANNOTATIONS.add(Singleton.class.getName());
    EXCLUDED_ANNOTATIONS.add(Named.class.getName());
    EXCLUDED_ANNOTATIONS.add(Factory.class.getName());
    EXCLUDED_ANNOTATIONS.add(Generated.class.getName());
    EXCLUDED_ANNOTATIONS.add(Constants.KOTLIN_METADATA);
    EXCLUDED_ANNOTATIONS.add(Constants.PATH);
  }

  private final TypeElement beanType;

  private final ProcessingContext processingContext;

  private final String shortName;

  private String name;

  private MethodReader injectConstructor;

  private final List<MethodReader> otherConstructors = new ArrayList<>();

  private List<MethodReader> factoryMethods = new ArrayList<>();

  private Element postConstructMethod;

  private Element preDestroyMethod;

  private final List<FieldReader> injectFields = new ArrayList<>();

  private final List<String> interfaceTypes = new ArrayList<>();

  private String addForType;

  private Set<String> importTypes = new TreeSet<>();

  private MethodReader constructor;

  private String registrationTypes;

  private boolean writtenToFile;

  BeanReader(TypeElement beanType, ProcessingContext processingContext) {
    this.beanType = beanType;
    this.shortName = beanType.getSimpleName().toString();
    this.processingContext = processingContext;
    initInterfaces();
    initAddFor();
  }

  private void initAddFor() {
    if (interfaceTypes.size() == 1) {
      addForType = Util.shortName(Util.unwrapProvider(interfaceTypes.get(0)));
    }
  }

  private void initInterfaces() {

    StringBuilder sb = new StringBuilder();

    for (TypeMirror anInterface : beanType.getInterfaces()) {
      String type = Util.unwrapProvider(anInterface.toString());
      interfaceTypes.add(type);
      importTypes.add(type);
      sb.append(", ").append(Util.shortName(type)).append(".class");
    }

    // get class level annotations (that are not Named and Singleton)
    for (AnnotationMirror annotationMirror : beanType.getAnnotationMirrors()) {
      String annotationType = annotationMirror.getAnnotationType().toString();
      if (includeAnnotation(annotationType)) {
        importTypes.add(annotationType);
        sb.append(", ").append(Util.shortName(annotationType)).append(".class");
      }
    }

    registrationTypes = sb.toString();
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

  void read(boolean factory) {

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
          readMethod(element, factory);
          break;
      }
    }
    constructor = findConstructor();
    if (constructor != null) {
      constructor.addImports(importTypes);
    }
    for (MethodReader factoryMethod : factoryMethods) {
      factoryMethod.addImports(importTypes);
    }
  }

  private MethodReader findConstructor() {
    if (injectConstructor != null) {
      return injectConstructor;
    }
    if (otherConstructors.size() == 1) {
      return otherConstructors.get(0);
    }
    return null;
  }

  List<String> getDependsOn() {

    List<String> list = new ArrayList<>();
    if (constructor != null) {
      for (MethodReader.MethodParam param : constructor.getParams()) {
        list.add(param.getDependsOn());
      }
    }
    return list;
  }

  List<MethodReader> getFactoryMethods() {
    return factoryMethods;
  }

  List<String> getInterfaces() {
    return interfaceTypes;
  }

  /**
   * Return all the interfaces and annotations associated with this bean.
   * <p>
   * The bean is made a 'member' of the list of beans that implement the interface or have the
   * annotation.
   * </p>
   */
  String getInterfacesAndAnnotations() {
    return registrationTypes;
  }

  private boolean includeAnnotation(String annotationType) {
    return !EXCLUDED_ANNOTATIONS.contains(annotationType);
  }

  private void readNamed() {
    Named named = beanType.getAnnotation(Named.class);
    if (named != null) {
      this.name = named.value();
    }
  }

  private void readConstructor(Element element) {

    ExecutableElement ex = (ExecutableElement) element;

    MethodReader methodReader = new MethodReader(processingContext, ex, beanType, false);
    methodReader.read();

    Inject inject = element.getAnnotation(Inject.class);
    if (inject != null) {
      injectConstructor = methodReader;
    } else {
      otherConstructors.add(methodReader);
    }
  }

  private void readField(Element element) {
    Inject inject = element.getAnnotation(Inject.class);
    if (inject != null) {
      injectFields.add(new FieldReader(element));
    }
  }

  private void readMethod(Element element, boolean factory) {

    ExecutableElement methodElement = (ExecutableElement) element;
    if (factory) {
      Bean beanMarker = element.getAnnotation(Bean.class);
      if (beanMarker != null) {
        addFactoryMethod(methodElement);
      }
    }

    PostConstruct pcMarker = element.getAnnotation(PostConstruct.class);
    if (pcMarker != null) {
      postConstructMethod = element;
    }

    PreDestroy pdMarker = element.getAnnotation(PreDestroy.class);
    if (pdMarker != null) {
      preDestroyMethod = element;
    }
  }

  private void addFactoryMethod(ExecutableElement methodElement) {
    MethodReader methodReader = new MethodReader(processingContext, methodElement, beanType, true);
    methodReader.read();
    factoryMethods.add(methodReader);
  }

  String getSimpleName() {
    return beanType.getSimpleName().toString();
  }

  boolean isLifecycleRequired() {
    return postConstructMethod != null || preDestroyMethod != null;
  }

  List<MetaData> createFactoryMethodMeta() {
    if (factoryMethods.isEmpty()) {
      return Collections.emptyList();
    }

    List<MetaData> metaList = new ArrayList<>(factoryMethods.size());
    for (MethodReader factoryMethod : factoryMethods) {
      metaList.add(factoryMethod.createMeta());
    }
    return metaList;
  }

  MetaData createMeta() {
    MetaData metaData = new MetaData(beanType.getQualifiedName().toString());
    metaData.update(this);
    return metaData;
  }

  boolean isFieldInjectionRequired() {
    return !injectFields.isEmpty();
  }

  void buildAddFor(Append writer) {
    writer.append("    if (builder.isAddBeanFor(");
    if (addForType != null) {
      writer.append(addForType).append(".class, ");
    }
    writer.append(shortName).append(".class)) {").eol();
  }

  private Set<String> importTypes() {
    if (isLifecycleRequired()) {
      importTypes.add(Constants.BEAN_LIFECYCLE);
    }
    importTypes.add(Constants.GENERATED);
    importTypes.add(Constants.BUILDER);
    importTypes.add(beanType.getQualifiedName().toString());
    return importTypes;
  }

  void writeImports(Append writer) {
    for (String importType : importTypes()) {
      writer.append("import %s;", importType).eol();
    }
    writer.eol();
  }

  MethodReader getConstructor() {
    return constructor;
  }

  boolean isWrittenToFile() {
    return writtenToFile;
  }

  void setWrittenToFile() {
    this.writtenToFile = true;
  }
}
