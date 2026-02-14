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
  /**
   * Strips the specified character from the start of a string.
   *
   * @param value the string to strip
   * @param ch the character to remove
   * @return the stripped string
   */
  public static String trimStart(String value, char ch) {
    int start = 0;
    while (start < value.length() && value.charAt(start) == ch) {
      start++;
    }
    return value.substring(start);
  }

  /**
   * Strips the specified character from the end of a string.
   *
   * @param value the string to strip
   * @param ch the character to remove
   * @return the stripped string
   */
  public static String trimEnd(String value, char ch) {
    int end = value.length();
    while (end > 0 && value.charAt(end - 1) == ch) {
      end--;
    }
    return value.substring(0, end);
  }

  public static String padEnd(String value, int length, char padding) {
    if (value.length() >= length) {
      return value;
    } else {
      return value + String.valueOf(padding).repeat(length - value.length());
    }
  }

  /**
   * Returns the number of Unicode code points in the string.
   *
   * @param value the string to count code points in
   * @return the number of code points
   */
  public static int codePointCount(String value) {
    return value.codePointCount(0, value.length());
  }
}
