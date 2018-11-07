package io.kanuka.generator;

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

  private final ProcessingContext processingContext;

  private Append writer;

  private String originName;
  private String shortName;
  private String packageName;

  SimpleBeanWriter(BeanReader beanReader, ProcessingContext processingContext) {
    this.beanReader = beanReader;
    this.processingContext = processingContext;
  }

  private Writer createFileWriter() throws IOException {

    TypeElement origin = beanReader.getBeanType();
    originName = origin.getQualifiedName().toString();
    shortName = origin.getSimpleName().toString();
    String fullName = originName + "$di";

    processingContext.logNote("write " + fullName);

    int dp = originName.lastIndexOf('.');
    if (dp > -1) {
      packageName = originName.substring(0, dp);
    }

    JavaFileObject jfo = processingContext.createWriter(fullName, origin);
    return jfo.openWriter();
  }

  void write() throws IOException {

    writer = new Append(createFileWriter());
    writePackage();
    writeImports();
    writeClassStart();
    writeStaticFactoryMethod();

    writeConstructor();
    writeLifecycleMethods();
    writeClassEnd();

    writer.close();
  }

  private void writeStaticFactoryMethod() {

    MethodReader constructor = beanReader.obtainConstructor();
    if (constructor == null) {
      processingContext.logError(beanReader.getBeanType(), "Unable to determine constructor to use?");
      return;
    }

    writer.append("  public static void build(Builder builder) {").eol();
    writer.append("    builder.currentBean(\"%s\");", originName).eol();

    // CoffeeMaker bean = new CoffeeMaker(builder.get(Heater.class, "electric"), builder.get(Pump.class));
    writer.append("    %s bean = new %s(", originName, originName);

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
    writer.append("    builder.addBean(bean, ");
    String name = beanReader.getName();
    if (name == null) {
      writer.append("null");
    } else {
      writer.append("\"%s\"", name);
    }

    // add interfaces and annotations
    writer.append(beanReader.getInterfacesAndAnnotations()).append(");").eol();

    if (beanReader.isLifecycleRequired()) {
      //builder.addLifecycle(new CoffeeMaker$di(bean));
      writer.append("    builder.addLifecycle(new %s$di(bean));", shortName).eol();
    }
    writer.append("  }").eol().eol();
  }

  private void writeImports() {
    if (beanReader.isLifecycleRequired()) {
      writer.append("import io.kanuka.core.BeanLifeCycle;").eol();
    }
    writer.append("import io.kanuka.core.Builder;").eol().eol();
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
    writer.append("public class ").append(shortName).append("$di ");
    if (beanReader.isLifecycleRequired()) {
      writer.append("implements BeanLifeCycle ");
    }
    writer.append(" {").eol().eol();
  }

  private void writePackage() {
    if (packageName != null) {
      writer.append("package %s;", packageName).eol().eol();
    }
  }

//  package coffee;
//
//import io.kanuka.core.BeanLifeCycle;
//
//  public class CoffeeMaker$k implements BeanLifeCycle {
//
//    private final CoffeeMaker bean;
//
//    public CoffeeMaker$k(CoffeeMaker bean) {
//      this.bean = bean;
//    }
//
//    @Override
//    public void postConstruct() {
//      bean.postConstruct();
//    }
//
//    @Override
//    public void preDestroy() {
//      bean.destroy();
//    }
//  }
}
