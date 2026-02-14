package io.bytestreams.codec.core;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Factory for creating number codecs.
 *
 * <p>No-arg methods create binary codecs via {@link BinaryNumberCodec}. Overloads accepting a
 * {@link Codec Codec&lt;String&gt;} create string-encoded codecs via {@link StringNumberCodec}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Binary integer codec (4 bytes big-endian)
 * FixedLengthCodec<Integer> binary = NumberCodecs.ofInt();
 *
 * // String integer codec (radix 10)
 * Codec<Integer> decimal = NumberCodecs.ofInt(stringCodec);
 *
 * // String integer codec (radix 16)
 * Codec<Integer> hex = NumberCodecs.ofInt(stringCodec, 16);
 *
 * // Unsigned byte codec
 * FixedLengthCodec<Integer> unsigned = NumberCodecs.ofUnsignedByte();
 * }</pre>
 */
public class NumberCodecs {
  private NumberCodecs() {}

  /**
   * Creates a codec for signed integer values encoded as 4-byte big-endian binary.
   *
   * @return a new binary integer codec
   */
  public static BinaryNumberCodec<Integer> ofInt() {
    return BinaryNumberCodec.ofInt();
  }

  /**
   * Creates a codec for integer values encoded as decimal strings.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @return a new string integer codec
   */
  public static StringNumberCodec<Integer> ofInt(Codec<String> stringCodec) {
    return StringNumberCodec.builder(stringCodec).ofInt();
  }

  /**
   * Creates a codec for integer values encoded as strings with the specified radix.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @param radix the radix for number-to-string conversion
   * @return a new string integer codec
   */
  public static StringNumberCodec<Integer> ofInt(Codec<String> stringCodec, int radix) {
    return StringNumberCodec.builder(stringCodec).ofInt(radix);
  }

  /**
   * Creates a codec for signed long values encoded as 8-byte big-endian binary.
   *
   * @return a new binary long codec
   */
  public static BinaryNumberCodec<Long> ofLong() {
    return BinaryNumberCodec.ofLong();
  }

  /**
   * Creates a codec for long values encoded as decimal strings.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @return a new string long codec
   */
  public static StringNumberCodec<Long> ofLong(Codec<String> stringCodec) {
    return StringNumberCodec.builder(stringCodec).ofLong();
  }

  /**
   * Creates a codec for long values encoded as strings with the specified radix.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @param radix the radix for number-to-string conversion
   * @return a new string long codec
   */
  public static StringNumberCodec<Long> ofLong(Codec<String> stringCodec, int radix) {
    return StringNumberCodec.builder(stringCodec).ofLong(radix);
  }

  /**
   * Creates a codec for signed short values encoded as 2-byte big-endian binary.
   *
   * @return a new binary short codec
   */
  public static BinaryNumberCodec<Short> ofShort() {
    return BinaryNumberCodec.ofShort();
  }

  /**
   * Creates a codec for short values encoded as decimal strings.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @return a new string short codec
   */
  public static StringNumberCodec<Short> ofShort(Codec<String> stringCodec) {
    return StringNumberCodec.builder(stringCodec).ofShort();
  }

  /**
   * Creates a codec for short values encoded as strings with the specified radix.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @param radix the radix for number-to-string conversion
   * @return a new string short codec
   */
  public static StringNumberCodec<Short> ofShort(Codec<String> stringCodec, int radix) {
    return StringNumberCodec.builder(stringCodec).ofShort(radix);
  }

  /**
   * Creates a codec for double values encoded as 8-byte IEEE 754 binary.
   *
   * @return a new binary double codec
   */
  public static BinaryNumberCodec<Double> ofDouble() {
    return BinaryNumberCodec.ofDouble();
  }

  /**
   * Creates a codec for double values encoded as decimal strings.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @return a new string double codec
   */
  public static StringNumberCodec<Double> ofDouble(Codec<String> stringCodec) {
    return StringNumberCodec.builder(stringCodec).ofDouble();
  }

  /**
   * Creates a codec for float values encoded as 4-byte IEEE 754 binary.
   *
   * @return a new binary float codec
   */
  public static BinaryNumberCodec<Float> ofFloat() {
    return BinaryNumberCodec.ofFloat();
  }

  /**
   * Creates a codec for float values encoded as decimal strings.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @return a new string float codec
   */
  public static StringNumberCodec<Float> ofFloat(Codec<String> stringCodec) {
    return StringNumberCodec.builder(stringCodec).ofFloat();
  }

  /**
   * Creates a codec for unsigned byte values (0 to 255) encoded as 1-byte binary.
   *
   * @return a new unsigned byte codec
   */
  public static BinaryNumberCodec<Integer> ofUnsignedByte() {
    return BinaryNumberCodec.ofUnsignedByte();
  }

  /**
   * Creates a codec for unsigned short values (0 to 65535) encoded as 2-byte binary.
   *
   * @return a new unsigned short codec
   */
  public static BinaryNumberCodec<Integer> ofUnsignedShort() {
    return BinaryNumberCodec.ofUnsignedShort();
  }

  /**
   * Creates a codec for unsigned integer values (0 to 4294967295) encoded as 4-byte binary.
   *
   * @return a new unsigned integer codec
   */
  public static BinaryNumberCodec<Long> ofUnsignedInt() {
    return BinaryNumberCodec.ofUnsignedInt();
  }

  /**
   * Creates a codec for {@link BigInteger} values encoded as decimal strings.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @return a new string big integer codec
   */
  public static StringNumberCodec<BigInteger> ofBigInt(Codec<String> stringCodec) {
    return StringNumberCodec.builder(stringCodec).ofBigInt();
  }

  /**
   * Creates a codec for {@link BigInteger} values encoded as strings with the specified radix.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @param radix the radix for number-to-string conversion
   * @return a new string big integer codec
   */
  public static StringNumberCodec<BigInteger> ofBigInt(Codec<String> stringCodec, int radix) {
    return StringNumberCodec.builder(stringCodec).ofBigInt(radix);
  }

  /**
   * Creates a codec for {@link BigDecimal} values encoded as decimal strings.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @return a new string big decimal codec
   */
  public static StringNumberCodec<BigDecimal> ofBigDecimal(Codec<String> stringCodec) {
    return StringNumberCodec.builder(stringCodec).ofBigDecimal();
  }
}
