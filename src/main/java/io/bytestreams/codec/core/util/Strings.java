package io.bytestreams.codec.core.util;

import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Utility methods for string operations.
 */
public final class Strings {
  private Strings() {}

  /**
   * Pads the start of a string with the specified character until it reaches the desired length.
   *
   * @param value the string to pad
   * @param padding the character to use for padding
   * @param length the target length
   * @return the padded string, or the original string if already at or above the target length
   */
  public static String padStart(String value, char padding, int length) {
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
   * @param padding the character to use for padding
   * @param length the target length
   * @return the padded string, or the original string if already at or above the target length
   */
  public static String padEnd(String value, char padding, int length) {
    if (value.length() >= length) {
      return value;
    } else {
      return value + String.valueOf(padding).repeat(length - value.length());
    }
  }

  /**
   * Returns a function that pads the start of a string with the specified character until it
   * reaches the desired length.
   *
   * @param padding the character to use for padding
   * @param length the target length
   * @return a function that pads strings on the left
   */
  public static Function<String, String> padStart(char padding, int length) {
    return value -> padStart(value, padding, length);
  }

  /**
   * Returns a function that pads the end of a string with the specified character until it reaches
   * the desired length.
   *
   * @param padding the character to use for padding
   * @param length the target length
   * @return a function that pads strings on the right
   */
  public static Function<String, String> padEnd(char padding, int length) {
    return value -> padEnd(value, padding, length);
  }

  /**
   * Strips leading occurrences of the specified character from the string.
   *
   * @param value the string to strip
   * @param padding the character to strip
   * @return the stripped string
   */
  public static String stripStart(String value, char padding) {
    int start = 0;
    while (start < value.length() && value.charAt(start) == padding) {
      start++;
    }
    return value.substring(start);
  }

  /**
   * Strips trailing occurrences of the specified character from the string.
   *
   * @param value the string to strip
   * @param padding the character to strip
   * @return the stripped string
   */
  public static String stripEnd(String value, char padding) {
    int end = value.length();
    while (end > 0 && value.charAt(end - 1) == padding) {
      end--;
    }
    return value.substring(0, end);
  }

  /**
   * Returns whether the charset uses at most one byte per character.
   *
   * @param charset the charset to check
   * @return true if the charset is single-byte
   */
  public static boolean isSingleByte(Charset charset) {
    return charset.newEncoder().maxBytesPerChar() <= 1.0f;
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

  /**
   * Returns the number of bytes represented by the hex string (two hex digits per byte).
   *
   * @param value the hex string to count bytes in
   * @return the number of bytes
   */
  public static int hexByteCount(String value) {
    return hexByteCount(value.length());
  }

  /**
   * Returns the number of bytes represented by the given number of hex digits (two digits per
   * byte).
   *
   * @param digits the number of hex digits
   * @return the number of bytes
   */
  public static int hexByteCount(int digits) {
    return (digits + 1) / 2;
  }
}
