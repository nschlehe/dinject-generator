package io.kanuka.generator;

import javax.inject.Named;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

class FieldReader {

  private final Element element;

  private final String name;

  FieldReader(Element element) {
    this.element = element;
    this.name = readName();
  }

  private String readName() {
    Named named = element.getAnnotation(Named.class);
    return (named == null) ? null : named.value();
  }

  String getFieldName() {
    return element.getSimpleName().toString();
  }

  String builderGetDependency() {

    TypeMirror type = element.asType();
    String rawType = type.toString();
    boolean optional = Util.isOptional(rawType);
    if (optional) {
      rawType = Util.extractOptionalType(rawType);
    }

    StringBuilder sb = new StringBuilder();
    if (optional) {
      sb.append("b.getOptional(");
    } else {
      sb.append("b.get(");
    }

    sb.append(rawType).append(".class");
    if (name != null) {
      sb.append(",\"").append(name).append("\"");
    }
    sb.append(")");
    return sb.toString();
  }
}
