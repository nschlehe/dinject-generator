package io.kanuka.generator;

import io.kanuka.core.DependencyMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
      return trimPackage(type);
    }
    // ignore Beans from @Bean factory methods
    return null;
  }

  private boolean higherPackage(String interfacePackage, String topPackage) {
    if (interfacePackage.length() > topPackage.length()){
      return false;
    }
    return topPackage.startsWith(interfacePackage);
  }

  private String trimPackage(String cls) {
    int pos = cls.lastIndexOf('.');
    return (pos == -1) ? "" : cls.substring(0, pos);
  }

  String getShortType() {
    int pos = type.lastIndexOf('.');
    return (pos == -1) ? type : type.substring(pos + 1);
  }

  String buildMethod() {

    StringBuilder sb = new StringBuilder(200);
    sb.append("  @DependencyMeta(type=\"").append(type).append("\"");
    if (method != null && !method.isEmpty()) {
      sb.append(", method=\"").append(method).append("\"");
    }
    if (!provides.isEmpty()) {
      appendProvides(sb, "provides", provides);
    }
    if (!dependsOn.isEmpty()) {
      appendProvides(sb, "dependsOn", dependsOn);
    }
    sb.append(")").append(NEWLINE);

    sb.append("  protected void build").append(getShortType()).append("() {").append(NEWLINE);
    if (method == null || method.isEmpty()) {
      sb.append("    ").append(type).append("$di.build(builder);").append(NEWLINE);
    } else {
      sb.append("    ").append(method).append("(builder);").append(NEWLINE);
    }
    sb.append("  }").append(NEWLINE);

    return sb.toString();

//    @DependencyMeta(type = "coffee.sub.Widget")
//    protected void buildWidget() {
//      coffee.sub.Widget$di.build(builder);
//    }
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

}
