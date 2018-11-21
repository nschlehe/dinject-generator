package io.dinject.generator;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Write the source code for the bean.
 */
class SimpleBeanWriter {

  private final BeanReader beanReader;

  private final ProcessingContext ctx;

  private Append writer;

  private String originName;
  private String shortName;
  private String packageName;

  SimpleBeanWriter(BeanReader beanReader, ProcessingContext ctx) {
    this.beanReader = beanReader;
    this.ctx = ctx;
    TypeElement origin = beanReader.getBeanType();
    this.originName = origin.getQualifiedName().toString();
    this.shortName = origin.getSimpleName().toString();
    this.packageName = Util.packageOf(originName);
  }

  private Writer createFileWriter() throws IOException {
    JavaFileObject jfo = ctx.createWriter(originName + "$di", beanReader.getBeanType());
    return jfo.openWriter();
  }

  void write() throws IOException {

    writer = new Append(createFileWriter());
    writePackage();
    writeImports();
    writeClassStart();
    writeStaticFactoryMethod();
    writeStaticFactoryBeanMethods();

    writeConstructor();
    writeLifecycleMethods();
    writeClassEnd();

    writer.close();
  }

  private void writeStaticFactoryBeanMethods() {
    for (MethodReader factoryMethod : beanReader.getFactoryMethods()) {
      writeFactoryBeanMethod(factoryMethod);
    }
  }

  private void writeFactoryBeanMethod(MethodReader method) {
    writer.append("  public static void build_%s(Builder builder) {", method.getName()).eol();

    method.buildAddFor(writer);
    writer.append(method.builderGetFactory()).eol();
    writer.append(method.builderBuildBean()).eol();
    method.builderBuildAddBean(writer);
    writer.append("    }").eol();
    writer.append("  }").eol().eol();
  }

  private void writeStaticFactoryMethod() {

    MethodReader constructor = beanReader.getConstructor();
    if (constructor == null) {
      ctx.logError(beanReader.getBeanType(), "Unable to determine constructor to use?");
      return;
    }

    writer.append("  public static void build(Builder builder) {").eol();

    beanReader.buildAddFor(writer);
    writer.append("      %s bean = new %s(", shortName, shortName);

    // add constructor dependencies
    List<MethodReader.MethodParam> params = constructor.getParams();
    for (int i = 0; i < params.size(); i++) {
      if (i > 0) {
        writer.append(",");
      }
      writer.append(params.get(i).builderGetDependency());
    }
    writer.append(");").eol();

    //builder.addBean(bean, null, "coffee.Controller");
    writer.append("      builder.register(bean, ");
    String name = beanReader.getName();
    if (name == null) {
      writer.append("null");
    } else {
      writer.append("\"%s\"", name);
    }

    // add interfaces and annotations
    writer.append(beanReader.getInterfacesAndAnnotations()).append(");").eol();

    if (beanReader.isLifecycleRequired()) {
      writer.append("      builder.addLifecycle(new %s$di(bean));", shortName).eol();
    }
    if (beanReader.isFieldInjectionRequired()) {
      writer.append("      builder.addInjector(b -> {").eol();
      for (FieldReader fieldReader : beanReader.getInjectFields()) {
        String fieldName = fieldReader.getFieldName();
        String getDependency = fieldReader.builderGetDependency();
        writer.append("        bean.%s = %s;", fieldName, getDependency).eol();
      }
      writer.append("      });").eol();
    }
    writer.append("    }").eol();
    writer.append("  }").eol().eol();
  }

  private void writeImports() {
    beanReader.writeImports(writer);
  }

  private void writeLifecycleMethods() {
    if (beanReader.isLifecycleRequired()) {
      lifecycleMethod("postConstruct", beanReader.getPostConstructMethod());
      lifecycleMethod("preDestroy", beanReader.getPreDestroyMethod());
    }
  }

  private void lifecycleMethod(String method, Element methodElement) {
    writer.append("  @Override").eol();
    writer.append("  public void %s() {", method).eol();
    if (methodElement == null) {
      writer.append("    // do nothing for %s", method).eol();
    } else {
      String methodName = methodElement.getSimpleName().toString();
      writer.append("    bean.%s();", methodName).eol();
    }
    writer.append("  }").eol().eol();
  }

  private void writeConstructor() {
    if (beanReader.isLifecycleRequired()) {
      writer.append("  private final %s bean;", shortName).eol().eol();
      writer.append("  public %s$di(%s bean) {", shortName, shortName).eol();
      writer.append("    this.bean = bean;").eol();
      writer.append("  }").eol().eol();
    }
  }

  private void writeClassEnd() {
    writer.append("}").eol();
  }

  private void writeClassStart() {
    writer.append(Constants.AT_GENERATED).eol();
    writer.append("public class ").append(shortName).append("$di ");
    if (beanReader.isLifecycleRequired()) {
      writer.append("implements BeanLifecycle ");
    }
    writer.append(" {").eol().eol();
  }

  private void writePackage() {
    if (packageName != null) {
      writer.append("package %s;", packageName).eol().eol();
    }
  }
}
