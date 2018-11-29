package io.dinject.generator;

import io.dinject.core.DependencyMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Holds the data as per <code>@DependencyMeta</code>
 */
class MetaData {

  private static final String NEWLINE = "\n";

  private final String type;

  private String method;

  private boolean wired;

  /**
   * The interfaces and class annotations the bean has (to register into lists).
   */
  private List<String> provides;

  /**
   * The list of dependencies with optional and named.
   */
  private List<String> dependsOn;

  MetaData(DependencyMeta meta) {
    this.type = meta.type();
    this.method = meta.method();
    this.provides = asList(meta.provides());
    this.dependsOn = asList(meta.dependsOn());
  }

  MetaData(String type) {
    this.type = type;
    this.provides = new ArrayList<>();
    this.dependsOn = new ArrayList<>();
  }

  boolean noDepends() {
    return dependsOn == null || dependsOn.isEmpty();
  }

  boolean isWired() {
    return wired;
  }

  void setWired() {
    this.wired = true;
  }

  private List<String> asList(String[] content) {
    if (content == null || content.length == 0) {
      return new ArrayList<>();
    }
    return Arrays.asList(content);
  }

  void update(BeanReader beanReader) {
    this.provides = beanReader.getInterfaces();
    this.dependsOn = beanReader.getDependsOn();
  }

  String getType() {
    return type;
  }

  List<String> getProvides() {
    return provides;
  }

  List<String> getDependsOn() {
    return dependsOn;
  }

  /**
   * Return the top level package for the bean and the interfaces it implements.
   */
  String getTopPackage() {
    if (method == null || method.isEmpty()) {
      return Util.packageOf(type);
    }
    // ignore Beans from @Bean factory methods
    return null;
  }

  void addImportTypes(Set<String> importTypes) {
    if (hasMethod()) {
      importTypes.add(Util.classOfMethod(method));

    } else {
      importTypes.add(type + "$di");
    }
  }

  String buildMethod() {

    StringBuilder sb = new StringBuilder(200);
    sb.append("  @DependencyMeta(type=\"").append(type).append("\"");
    if (hasMethod()) {
      sb.append(", method=\"").append(method).append("\"");
    }
    if (!provides.isEmpty()) {
      appendProvides(sb, "provides", provides);
    }
    if (!dependsOn.isEmpty()) {
      appendProvides(sb, "dependsOn", dependsOn);
    }
    sb.append(")").append(NEWLINE);

    String shortName = Util.shortName(type);
    sb.append("  protected void build_").append(shortName).append("() {").append(NEWLINE);
    if (hasMethod()) {
      sb.append("    ").append(Util.shortMethod(method)).append("(builder);").append(NEWLINE);
    } else {
      sb.append("    ").append(shortName).append("$di.build(builder);").append(NEWLINE);
    }
    sb.append("  }").append(NEWLINE);

    return sb.toString();
  }

  private boolean hasMethod() {
    return method != null && !method.isEmpty();
  }

  private void appendProvides(StringBuilder sb, String attribute, List<String> types) {
    sb.append(",").append(attribute).append("={");
    for (int i = 0; i < types.size(); i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append("\"");
      sb.append(types.get(i));
      sb.append("\"");
    }
    sb.append("}");
  }

  void setProvides(List<String> provides) {
    this.provides = provides;
  }

  void setDependsOn(List<String> dependsOn) {
    this.dependsOn = dependsOn;
  }

  void setMethod(String method) {
    this.method = method;
  }

  String getShortType() {
    return Util.shortName(type);
  }
}
