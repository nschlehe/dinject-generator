package io.kanuka.generator;

import io.kanuka.ContextModule;
import io.kanuka.Factory;
import io.kanuka.core.DependencyMeta;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Processor extends AbstractProcessor {

  private ProcessingContext processingContext;

  private Elements elementUtils;

  private Map<String, MetaData> metaData = new LinkedHashMap<>();

  private List<BeanReader> beanReaders = new ArrayList<>();

  public Processor() {
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.processingContext = new ProcessingContext(processingEnv);
    this.elementUtils = processingEnv.getElementUtils();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {

    Set<String> annotations = new LinkedHashSet<>();
    annotations.add(ContextModule.class.getCanonicalName());
    annotations.add(Factory.class.getCanonicalName());
    annotations.add(Singleton.class.getCanonicalName());
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    Set<? extends Element> factoryBeans = roundEnv.getElementsAnnotatedWith(Factory.class);
    Set<? extends Element> beans = roundEnv.getElementsAnnotatedWith(Singleton.class);
    if (beans.isEmpty() && factoryBeans.isEmpty()) {
      processingContext.logDebug("skip, no changed beans ...");
      return false;
    }

    readModule(roundEnv);
    readChangedBeans(factoryBeans, true);
    readChangedBeans(beans, false);

    mergeMetaData();

    writeBeanHelpers();
    writeBeanFactory();

    return false;
  }


  private void writeBeanHelpers() {
    for (BeanReader beanReader : beanReaders) {
      try {
        SimpleBeanWriter writer = new SimpleBeanWriter(beanReader, processingContext);
        writer.write();
      } catch (IOException e) {
        processingContext.logError(beanReader.getBeanType(), "Failed to write $di class");
      }
    }
  }

  private void writeBeanFactory() {

    MetaDataOrdering ordering = new MetaDataOrdering(metaData.values(), processingContext);
    int remaining = ordering.processQueue();
    if (remaining > 0) {
      processingContext.logWarn("there are " + remaining + " beans with unsatisfied dependencies (assuming external dependencies)");
      ordering.warnOnDependencies();
    }

    try {
      SimpleFactoryWriter factoryWriter = new SimpleFactoryWriter(ordering, processingContext);
      factoryWriter.write();
    } catch (IOException e) {
      processingContext.logError("Failed to write factory " + e.getMessage());
    }
  }

  /**
   * Read the beans that have changed.
   */
  private void readChangedBeans(Set<? extends Element> beans, boolean factory) {
    for (Element element : beans) {
      if (!(element instanceof TypeElement)) {
        processingContext.logError("unexpected type [" + element + "]");
      } else {
        readBeanMeta((TypeElement) element, factory);
      }
    }
  }

  /**
   * Merge the changed bean meta data into the existing (factory) metaData.
   */
  private void mergeMetaData() {

    for (BeanReader beanReader : beanReaders) {
      String simpleName = beanReader.getSimpleName();
      MetaData metaData = this.metaData.get(simpleName);
      if (metaData == null) {
        addMeta(beanReader);

      } else {
        updateMeta(metaData, beanReader);
      }
    }
  }

  /**
   * Add a new previously unknown bean.
   */
  private void addMeta(BeanReader beanReader) {
    MetaData meta = beanReader.createMeta();
    metaData.put(meta.getType(), meta);
    for (MetaData methodMeta : beanReader.createFactoryMethodMeta()) {
      metaData.put(methodMeta.getType(), methodMeta);
    }
  }

  /**
   * Update the meta data on a previously known bean.
   */
  private void updateMeta(MetaData metaData, BeanReader beanReader) {
    metaData.update(beanReader);
  }

  /**
   * Read the dependency injection meta data for the given bean.
   */
  private void readBeanMeta(TypeElement typeElement, boolean factory) {
    BeanReader beanReader = new BeanReader(typeElement, processingContext);
    beanReader.read(factory);
    beanReaders.add(beanReader);
  }

  /**
   * Read the existing meta data from ContextModule (if found) and the factory bean (if exists).
   */
  private void readModule(RoundEnvironment roundEnv) {

    String factory = processingContext.loadMetaInfServices();
    if (factory != null) {
      TypeElement factoryType = elementUtils.getTypeElement(factory);
      if (factoryType != null) {
        readFactory(factoryType);
      }
    }

    Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(ContextModule.class);
    if (!elementsAnnotatedWith.isEmpty()) {
      Iterator<? extends Element> iterator = elementsAnnotatedWith.iterator();
      if (iterator.hasNext()) {
        Element element = iterator.next();
        ContextModule annotation = element.getAnnotation(ContextModule.class);
        if (annotation != null) {
          processingContext.setContextDetails(annotation.name(), annotation.dependsOn());
        }
      }
    }
  }


  /**
   * Read the existing factory bean. Each of the build methods is annotated with <code>@DependencyMeta</code>
   * which holds the information we need (to regenerate the factory with any changes).
   */
  private void readFactory(TypeElement factoryType) {

    ContextModule module = factoryType.getAnnotation(ContextModule.class);
    processingContext.setContextDetails(module.name(), module.dependsOn());

    List<? extends Element> elements = factoryType.getEnclosedElements();
    if (elements != null) {
      for (Element element : elements) {
        ElementKind kind = element.getKind();
        if (ElementKind.METHOD == kind) {

          Name simpleName = element.getSimpleName();
          if (simpleName.toString().startsWith("build")) {
            // read a build method - DependencyMeta
            DependencyMeta meta = element.getAnnotation(DependencyMeta.class);
            if (meta == null) {
              processingContext.logError("Missing @DependencyMeta on method " + simpleName.toString());
            } else {
              metaData.put(meta.type(), new MetaData(meta));
            }
          }
        }
      }
    }
  }
}