package io.kanuka.generator;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

/**
 * Write the source code for the factory.
 */
class SimpleFactoryWriter {

  private final MetaDataOrdering ordering;
  private final ProcessingContext processingContext;

  private final String factoryPackage;
  private final String factoryShortName;
  private final String factoryFullName;

  private Append writer;

  SimpleFactoryWriter(MetaDataOrdering ordering, ProcessingContext processingContext) {
    this.ordering = ordering;
    this.processingContext = processingContext;

    this.factoryPackage = ordering.getTopPackage();
    this.factoryShortName = "_di$Factory";
    this.factoryFullName = factoryPackage + "." + factoryShortName;
  }

  void write() throws IOException {

    writer = new Append(createFileWriter());
    writePackage();
    writeStartClass();

    writeCreateMethod();
    writeBuildMethods();

    writeEndClass();
    writer.close();

    writeServicesFile();
  }

  private void writeServicesFile() {

    try {
      FileObject jfo = processingContext.createMetaInfWriter();
      if (jfo != null) {
        Writer writer = jfo.openWriter();
        processingContext.logNote("writing services file for " + factoryFullName);
        writer.write(factoryFullName);
        writer.close();
      }

    } catch (IOException e) {
      e.printStackTrace();
      processingContext.logError(null, "Failed to write services file " + e.getMessage());
    }

  }

  private void writeBuildMethods() {
    for (MetaData metaData : ordering.getOrdered()) {
      writer.append(metaData.buildMethod()).eol();
    }
  }

  private void writeCreateMethod() {

    writer.append("  @Override").eol();
    writer.append("  public BeanContext createContext(Builder parent) {").eol();
    writer.append("    builder.setParent(parent);").eol();
    for (MetaData metaData : ordering.getOrdered()) {
      writer.append("    build%s();", metaData.getShortType()).eol();
    }
    writer.append("    return builder.build();").eol();
    writer.append("  }").eol();
    writer.eol();
  }

  private void writePackage() {

    writer.append("package %s;", factoryPackage).eol().eol();

    writer.append("import io.kanuka.BeanContext;").eol();
    writer.append("import io.kanuka.core.BeanContextFactory;").eol();
    writer.append("import io.kanuka.core.BuilderFactory;").eol();
    writer.append("import io.kanuka.core.Builder;").eol();
    writer.append("import io.kanuka.core.DependencyMeta;").eol();
    writer.eol();
  }

  private void writeStartClass() {

    writer.append("public class %s implements BeanContextFactory {", factoryShortName).eol().eol();
    writer.append("  private final Builder builder;").eol().eol();

    writer.append("  public %s() {", factoryShortName).eol();
    writer.append("    this.builder = BuilderFactory.newBuilder(\"coffee\");").eol();
    writer.append("  }").eol().eol();

    writer.append("  @Override").eol();
    writer.append("  public String name() {").eol();
    writer.append("    return builder.getName();").eol();
    writer.append("  }").eol().eol();
  }

  private void writeEndClass() {
    writer.append("}").eol();
  }

  private Writer createFileWriter() throws IOException {

    processingContext.logNote("write " + factoryFullName);
    JavaFileObject jfo = processingContext.createWriter(factoryFullName, null);
    return jfo.openWriter();
  }
}
