package io.dinject.generator;

public class Constants {

  public static final String KOTLIN_METADATA = "kotlin.Metadata";
  public static final String GENERATED = "javax.annotation.Generated";

  public static final String PATH = "io.dinject.controller.Path";
  public static final String CONTROLLER = "io.dinject.controller.Controller";

  public static final String AT_GENERATED = "@Generated(\"io.dinject\")";
  public static final String META_INF_FACTORY = "META-INF/services/io.dinject.core.BeanContextFactory";

  public static final String BEAN_LIFECYCLE = "io.dinject.core.BeanLifecycle";
  public static final String BUILDER = "io.dinject.core.Builder";

  public static final String IMPORT_BEANCONTEXT = "import io.dinject.BeanContext;";
  public static final String IMPORT_CONTEXTMODULE = "import io.dinject.ContextModule;";
  public static final String IMPORT_BEANCONTEXTFACTORY = "import io.dinject.core.BeanContextFactory;";
  public static final String IMPORT_BUILDERFACTORY = "import io.dinject.core.BuilderFactory;";
  public static final String IMPORT_BUILDER = "import io.dinject.core.Builder;";
  public static final String IMPORT_DEPENDENCYMETA = "import io.dinject.core.DependencyMeta;";

}
