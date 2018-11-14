package io.kanuka.generator;

class Util {

  static String extractOptionalType(String rawType) {
    return rawType.substring(19, rawType.length() - 1);
  }

  static boolean isOptional(String rawType) {
    return rawType.startsWith("java.util.Optional<");
  }

  static String extractList(String rawType) {
    return rawType.substring(15, rawType.length() - 1);
  }

  static boolean isList(String rawType) {
    return rawType.startsWith("java.util.List<");
  }

  static boolean isProvider(String rawType) {
    return rawType.startsWith("javax.inject.Provider<");
  }

  static String extractProviderType(String rawType) {
    return rawType.substring(22, rawType.length() - 1);
  }

  /**
   * Return the common parent package.
   */
  static String commonParent(String currentTop, String aPackage) {

    if (aPackage == null) return currentTop;
    if (currentTop == null) return aPackage;
    if (aPackage.startsWith(currentTop)) {
      return currentTop;
    }
    int next;
    do {
      next = currentTop.lastIndexOf('.');
      if (next > -1) {
        currentTop = currentTop.substring(0, next);
        if (aPackage.startsWith(currentTop)) {
          return currentTop;
        }
      }
    } while (next > -1);

    return currentTop;
  }
}
