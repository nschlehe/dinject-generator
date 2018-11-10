package io.kanuka.generator;

class Util {

  static String extractOptionalType(String rawType) {
    return rawType.substring(19, rawType.length() - 1);
  }

  static boolean isOptional(String rawType) {
    return rawType.startsWith("java.util.Optional<");
  }
}
