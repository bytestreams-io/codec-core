package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;

/**
 * Factory for creating string codec builders.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Fixed-length code point string
 * Codec<String> fixed = StringCodecs.ofCodePoint(5).build();
 *
 * // Variable-length code point string
 * Codec<String> stream = StringCodecs.ofCodePoint().build();
 *
 * // Charset convenience methods
 * FixedCodePointStringCodec asciiCodec = StringCodecs.ascii(5);
 * StreamCodePointStringCodec utf8Codec = StringCodecs.utf8();
 *
 * // Fixed-length hex string
 * Codec<String> hex = StringCodecs.ofHex(4).padRight('f').build();
 *
 * // Variable-length hex string
 * Codec<String> hexStream = StringCodecs.ofHex().padRight('f').build();
 *
 * // Formatted string with delegate
 * Codec<String> formatted = StringCodecs.ofFormatted(delegate).padRight('0').build();
 * }</pre>
 */
public class StringCodecs {
  private static final Charset EBCDIC = Charset.forName("IBM1047");

  private StringCodecs() {}

  /**
   * Creates a new builder for a fixed-length code point string codec.
   *
   * @param length the number of code points
   * @return a new builder
   */
  public static FixedCodePointStringCodec.Builder ofCodePoint(int length) {
    return FixedCodePointStringCodec.builder(length);
  }

  /**
   * Creates a new builder for a variable-length code point string codec.
   *
   * @return a new builder
   */
  public static StreamCodePointStringCodec.Builder ofCodePoint() {
    return StreamCodePointStringCodec.builder();
  }

  /**
   * Creates a new builder for a fixed-length hex string codec.
   *
   * @param length the number of hex digits
   * @return a new builder
   */
  public static FixedHexStringCodec.Builder ofHex(int length) {
    return FixedHexStringCodec.builder(length);
  }

  /**
   * Creates a new builder for a variable-length hex string codec.
   *
   * @return a new builder
   */
  public static StreamHexStringCodec.Builder ofHex() {
    return StreamHexStringCodec.builder();
  }

  /**
   * Creates a new builder for a formatted string codec.
   *
   * @param delegate the fixed-length string codec to delegate to
   * @return a new builder
   */
  public static FormattedStringCodec.Builder ofFormatted(FixedLengthCodec<String> delegate) {
    return FormattedStringCodec.builder(delegate);
  }

  /**
   * Creates a fixed-length US-ASCII string codec.
   *
   * @param length the number of code points
   * @return a new codec
   */
  public static FixedCodePointStringCodec ascii(int length) {
    return ofCodePoint(length).charset(US_ASCII).build();
  }

  /**
   * Creates a variable-length US-ASCII string codec.
   *
   * @return a new codec
   */
  public static StreamCodePointStringCodec ascii() {
    return ofCodePoint().charset(US_ASCII).build();
  }

  /**
   * Creates a fixed-length UTF-8 string codec.
   *
   * @param length the number of code points
   * @return a new codec
   */
  public static FixedCodePointStringCodec utf8(int length) {
    return ofCodePoint(length).charset(UTF_8).build();
  }

  /**
   * Creates a variable-length UTF-8 string codec.
   *
   * @return a new codec
   */
  public static StreamCodePointStringCodec utf8() {
    return ofCodePoint().charset(UTF_8).build();
  }

  /**
   * Creates a fixed-length ISO-8859-1 (Latin-1) string codec.
   *
   * @param length the number of code points
   * @return a new codec
   */
  public static FixedCodePointStringCodec latin1(int length) {
    return ofCodePoint(length).charset(ISO_8859_1).build();
  }

  /**
   * Creates a variable-length ISO-8859-1 (Latin-1) string codec.
   *
   * @return a new codec
   */
  public static StreamCodePointStringCodec latin1() {
    return ofCodePoint().charset(ISO_8859_1).build();
  }

  /**
   * Creates a fixed-length EBCDIC (IBM1047) string codec.
   *
   * @param length the number of code points
   * @return a new codec
   */
  public static FixedCodePointStringCodec ebcdic(int length) {
    return ofCodePoint(length).charset(EBCDIC).build();
  }

  /**
   * Creates a variable-length EBCDIC (IBM1047) string codec.
   *
   * @return a new codec
   */
  public static StreamCodePointStringCodec ebcdic() {
    return ofCodePoint().charset(EBCDIC).build();
  }
}
