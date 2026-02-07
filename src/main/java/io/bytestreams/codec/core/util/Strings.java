package io.bytestreams.codec.core.util;

/**
 * Utility methods for string operations.
 */
public final class Strings {
  private Strings() {}

  /**
   * Pads the start of a string with the specified character until it reaches the desired length.
   *
   * @param value the string to pad
   * @param length the target length
   * @param padding the character to use for padding
   * @return the padded string, or the original string if already at or above the target length
   */
  public static String padStart(String value, int length, char padding) {
    if (value.length() >= length) {
      return value;
    } else {
      return String.valueOf(padding).repeat(length - value.length()) + value;
    }
  }

  /**
   * Pads the end of a string with the specified character until it reaches the desired length.
   *
   * @param value the string to pad
   * @param length the target length
   * @param padding the character to use for padding
   * @return the padded string, or the original string if already at or above the target length
   */
  public static String padEnd(String value, int length, char padding) {
    if (value.length() >= length) {
      return value;
    } else {
      return value + String.valueOf(padding).repeat(length - value.length());
    }
  }
}
