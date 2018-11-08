package io.kanuka.generator;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
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

  private String contextName;

  private String[] contextDependsOn;

  ProcessingContext(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.messager = processingEnv.getMessager();
    filer = processingEnv.getFiler();
  }

  /**
   * Log an error message.
   */
  void logError(Element e, String msg, Object... args) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
  }

  /**
   * Log a info message.
   */
  void logNote(String msg, Object... args) {
    //System.out.println("kanuka>> " + String.format(msg, args));
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
          logNote("read meta-inf services: " + line);
          return line.trim();
        }
      }

    } catch (FileNotFoundException e) {
      logNote("no services file yet");

    } catch (IOException e) {
      e.printStackTrace();
      logError(null, "Error reading services file: "+e.getMessage());
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

  void setContextDetails(String name, String[] dependsOn) {
    this.contextName = name;
    this.contextDependsOn = dependsOn;
  }

  public String getContextName() {
    return contextName;
  }

  public String[] getContextDependsOn() {
    return contextDependsOn;
  }
}
