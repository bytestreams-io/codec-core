package io.bytestreams.codec.core.util;

/**
 * Utility methods for checking preconditions.
 */
public final class Preconditions {
  private Preconditions() {}

  /**
   * Validates that a condition is true.
   *
   * @param condition the condition to check
   * @param errorTemplate the error message template
   * @param args the arguments for the error message template
   * @throws IllegalArgumentException if the condition is false
   */
  public static void check(boolean condition, String errorTemplate, Object... args) {
    if (!condition) {
      throw new IllegalArgumentException(String.format(errorTemplate, args));
    }
  }
}
