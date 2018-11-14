package io.kanuka.generator;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

class ProcessingContext {

  private static final String META_INF_FACTORY = "META-INF/services/io.kanuka.core.BeanContextFactory";

  private final ProcessingEnvironment processingEnv;

  private final Messager messager;

  private final Filer filer;
  private final Elements elementUtils;
  private final Types typeUtils;

  private String contextName;

  private String[] contextDependsOn;

  private String contextPackage;

  ProcessingContext(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.messager = processingEnv.getMessager();
    this.filer = processingEnv.getFiler();
    this.elementUtils = processingEnv.getElementUtils();
    this.typeUtils = processingEnv.getTypeUtils();
  }

  /**
   * Log an error message.
   */
  void logError(Element e, String msg, Object... args) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
  }

  void logError(String msg, Object... args) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args));
  }

  void logWarn(String msg, Object... args) {
    messager.printMessage(Diagnostic.Kind.WARNING, String.format(msg, args));
  }

  void logDebug(String msg, Object... args) {
    messager.printMessage(Diagnostic.Kind.NOTE, String.format(msg, args));
  }

  String loadMetaInfServices() {
    try {
      FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", META_INF_FACTORY);
      if (fileObject != null) {
        Reader reader = fileObject.openReader(true);
        LineNumberReader lineReader = new LineNumberReader(reader);
        String line = lineReader.readLine();
        if (line != null) {
          return line.trim();
        }
      }

    } catch (FileNotFoundException e) {
      logDebug("no services file yet");

    } catch (IOException e) {
      e.printStackTrace();
      logError("Error reading services file: " + e.getMessage());
    }
    return null;
  }

  /**
   * Create a file writer for the given class name.
   */
  JavaFileObject createWriter(String cls, Element origin) throws IOException {
    return filer.createSourceFile(cls, origin);
  }

  /**
   * Create a file writer for the given class name.
   */
  FileObject createMetaInfWriter() throws IOException {
    return filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/io.kanuka.core.BeanContextFactory");
  }

  void setContextDetails(String name, String[] dependsOn, Element contextElement) {
    this.contextName = name;
    this.contextDependsOn = dependsOn;

    // determine the context package (that we put the $diFactory class into)
    PackageElement pkg = elementUtils.getPackageOf(contextElement);
    logWarn("using package from element " + pkg);
    this.contextPackage = (pkg == null) ? null : pkg.getQualifiedName().toString();
  }

  String getContextName() {
    return contextName;
  }

  String[] getContextDependsOn() {
    return contextDependsOn;
  }

  String getContextPackage() {
    return contextPackage;
  }

  Element asElement(TypeMirror returnType) {
    return typeUtils.asElement(returnType);
  }
}
