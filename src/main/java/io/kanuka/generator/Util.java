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
}
