package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;

/**
 * A codec for {@link Integer}s encoded as {@link String}s.
 *
 * <p>The integer is converted to a string using the specified radix. The underlying string codec is
 * responsible for handling any length or padding requirements.
 */
public class StringIntegerCodec extends StringNumberCodec<Integer> {
  private final int radix;

  /**
   * Creates a new string integer codec with radix 10 (decimal).
   *
   * @param stringCodec the string codec to use for encoding/decoding
   */
  public StringIntegerCodec(Codec<String> stringCodec) {
    this(stringCodec, 10);
  }

  /**
   * Creates a new string integer codec with the specified parameters.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @param radix the radix to use for conversion (must be between {@link Character#MIN_RADIX} and
   *     {@link Character#MAX_RADIX})
   * @throws IllegalArgumentException if the radix is out of range
   */
  public StringIntegerCodec(Codec<String> stringCodec, int radix) {
    super(stringCodec);
    Preconditions.check(
        radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX,
        "radix must be between %d and %d, but was [%d]",
        Character.MIN_RADIX,
        Character.MAX_RADIX,
        radix);
    this.radix = radix;
  }

  @Override
  protected String fromNumber(Integer value) {
    return Integer.toString(value, radix);
  }

  @Override
  protected Integer toNumber(String value) {
    return Integer.valueOf(value, radix);
  }
}
