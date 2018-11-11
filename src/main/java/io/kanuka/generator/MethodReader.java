package io.kanuka.generator;

import javax.inject.Named;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

class MethodReader {

  private final ExecutableElement element;
  private final String factoryType;
  private final String returnTypeRaw;

  private final List<MethodParam> params = new ArrayList<>();

  MethodReader(ExecutableElement element, TypeElement beanType) {
    this.element = element;
    this.returnTypeRaw = element.getReturnType().toString();
    this.factoryType = beanType.getQualifiedName().toString();
  }

  void read() {
    List<? extends VariableElement> ps = element.getParameters();
    for (VariableElement p : ps) {
      params.add(new MethodParam(p.asType(), readNamed(p)));
    }
  }

  private String readNamed(VariableElement p) {
    Named named = p.getAnnotation(Named.class);
    return (named == null) ? null : named.value();
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
    return String.format("      %s factory = builder.get(%s.class);", factoryType, factoryType);
  }

  String builderBuildBean() {

    String methodName = element.getSimpleName().toString();
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("      %s bean = factory.%s(", returnTypeRaw, methodName));

    for (int i = 0; i < params.size(); i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(params.get(i).builderGetDependency());
    }
    sb.append(");");
    return sb.toString();
  }

  String builderDebugCurrentMethod() {

    String methodName = element.toString();
    return String.format("      builder.currentBean(\"%s\");", returnTypeRaw + " via " + methodName);
  }

  /**
   * Return the type we check to see if we skip building the bean due to supplied beans.
   */
  String getIsAddBeanFor() {
    return returnTypeRaw;
  }

  static class MethodParam {

    private final String rawType;
    private final String named;
    private final boolean listType;
    private final boolean optionalType;
    private final String paramType;

    MethodParam(TypeMirror type, String named) {
      this.rawType = type.toString();
      this.named = named;
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

    String builderGetDependency() {
      StringBuilder sb = new StringBuilder();
      if (listType) {
        sb.append("builder.getList(");
      } else if (optionalType) {
        sb.append("builder.getOptional(");
      } else {
        sb.append("builder.get(");
      }

      sb.append(paramType).append(".class");
      if (named != null) {
        sb.append(",\"").append(named).append("\"");
      }
      sb.append(")");
      return sb.toString();
    }

    String getDependsOn() {
      return paramType;
    }
  }
}
