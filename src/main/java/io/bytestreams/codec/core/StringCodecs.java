package io.bytestreams.codec.core;

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
}
