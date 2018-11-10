package io.kanuka.generator;

import javax.inject.Named;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

class MethodReader {

  private final ExecutableElement element;

  private final List<MethodParam> params = new ArrayList<>();

  MethodReader(ExecutableElement element) {
    this.element = element;
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

  static class MethodParam {

    private final String rawType;
    private final String named;
    private final boolean optional;
    private final String paramType;

    MethodParam(TypeMirror type, String named) {
      this.rawType = type.toString();
      this.named = named;
      this.optional = Util.isOptional(rawType);
      if (optional) {
        paramType = Util.extractOptionalType(rawType);
      } else {
        paramType = rawType;
      }
    }

    String builderGetDependency() {
      StringBuilder sb = new StringBuilder();
      if (optional) {
        sb.append("builder.getOptional(");
      } else {
        sb.append("builder.get(");
      }

      //coffee.Heater.class
      sb.append(paramType).append(".class");

      if (named != null) {
        // , "electric")
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
