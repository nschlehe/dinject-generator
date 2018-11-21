package io.dinject.generator;

import javax.inject.Named;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class MethodReader {

  private final ProcessingContext processingContext;
  private final ExecutableElement element;
  private final String factoryType;
  private final TypeMirror returnType;
  private final String returnTypeRaw;
  private final boolean isVoid;
  private final List<MethodParam> params = new ArrayList<>();

  private final List<String> interfaceTypes = new ArrayList<>();
  private final String factoryShortName;
  private final boolean isFactory;
  private String addForType;

  MethodReader(ProcessingContext processingContext, ExecutableElement element, TypeElement beanType, boolean isFactory) {
    this.isFactory = isFactory;
    this.processingContext = processingContext;
    this.element = element;
    this.returnType = element.getReturnType();
    this.returnTypeRaw = returnType.toString();
    this.factoryType = beanType.getQualifiedName().toString();
    this.factoryShortName = Util.shortName(factoryType);
    this.isVoid = returnTypeRaw.equals("void");

    initInterfaces();
  }

  private void initInterfaces() {
    Element element = processingContext.asElement(returnType);
    if (element instanceof TypeElement) {
      TypeElement te = (TypeElement) element;
      if (te.getKind() == ElementKind.INTERFACE) {
        interfaceTypes.add(te.getQualifiedName().toString());
      }
      for (TypeMirror anInterface : te.getInterfaces()) {
        interfaceTypes.add(anInterface.toString());
      }
      if (interfaceTypes.size() == 1) {
        addForType = interfaceTypes.get(0);
      }
    }
  }

  void read() {
    List<? extends VariableElement> ps = element.getParameters();
    for (VariableElement p : ps) {
      params.add(new MethodParam(p));
    }
  }

  List<MethodParam> getParams() {
    return params;
  }

  String getName() {
    return element.getSimpleName().toString();
  }

  MetaData createMeta() {

    MetaData metaData = new MetaData(returnTypeRaw);
    metaData.setMethod(fullBuildMethod());

    List<String> dependsOn = new ArrayList<>(params.size() + 1);
    dependsOn.add(factoryType);
    for (MethodParam param : params) {
      dependsOn.add(param.paramType);
    }
    metaData.setDependsOn(dependsOn);
    metaData.setProvides(new ArrayList<>());
    return metaData;
  }

  private String fullBuildMethod() {
    return factoryType + "$di.build_" + element.getSimpleName().toString();
  }

  String builderGetFactory() {
    return String.format("      %s factory = builder.get(%s.class);", factoryShortName, factoryShortName);
  }

  String builderBuildBean() {

    String methodName = element.getSimpleName().toString();
    StringBuilder sb = new StringBuilder();
    if (isVoid) {
      sb.append(String.format("      factory.%s(", methodName));
    } else {
      sb.append(String.format("      %s bean = factory.%s(", Util.shortName(returnTypeRaw), methodName));
    }

    for (int i = 0; i < params.size(); i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(params.get(i).builderGetDependency());
    }
    sb.append(");");
    return sb.toString();
  }

  void builderBuildAddBean(Append writer) {
    if (!isVoid) {
      writer.append("      builder.register(bean, null");
      for (String anInterface : interfaceTypes) {
        writer.append(", ").append(Util.shortName(anInterface)).append(".class");
      }
      writer.append(");").eol();
    }
  }

  void addImports(Set<String> importTypes) {
    for (MethodParam param : params) {
      param.addImports(importTypes);
    }
    if (isFactory) {
      importTypes.add(returnTypeRaw);
    }
  }

  void buildAddFor(Append writer) {

    writer.append("    if (builder.isAddBeanFor(");
    if (addForType != null) {
      writer.append(addForType).append(".class, ");
    }
    if (isVoid) {
      writer.append("Void.class)) {").eol();
    } else {
      writer.append(Util.shortName(returnTypeRaw)).append(".class)) {").eol();
    }
  }

  static class MethodParam {

    private final String rawType;
    private final String named;
    private final boolean listType;
    private final boolean optionalType;
    private final String paramType;

    MethodParam(VariableElement p) {
      TypeMirror type = p.asType();
      this.rawType = type.toString();
      this.named = readNamed(p);
      this.listType = Util.isList(rawType);
      this.optionalType = !listType && Util.isOptional(rawType);
      if (optionalType) {
        paramType = Util.extractOptionalType(rawType);
      } else if (listType) {
        paramType = Util.extractList(rawType);
      } else {
        paramType = rawType;
      }
    }

    private String readNamed(VariableElement p) {
      Named named = p.getAnnotation(Named.class);
      return (named == null) ? null : named.value();
    }

    String builderGetDependency() {
      StringBuilder sb = new StringBuilder();
      if (listType) {
        sb.append("builder.getList(");
      } else if (optionalType) {
        sb.append("builder.getOptional(");
      } else {
        sb.append("builder.get(");
      }
      sb.append(Util.shortName(paramType)).append(".class");
      if (named != null) {
        sb.append(",\"").append(named).append("\"");
      }
      sb.append(")");
      return sb.toString();
    }

    String getDependsOn() {
      return paramType;
    }

    void addImports(Set<String> importTypes) {
      importTypes.add(paramType);
    }
  }
}
