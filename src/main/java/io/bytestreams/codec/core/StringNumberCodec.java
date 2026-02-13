package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Function;

/**
 * Codec for {@link Number}s that are encoded as {@link String}s.
 *
 * <p>This codec wraps a string codec and converts between numbers and their string representation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * StringNumberCodec<Integer> codec = StringNumberCodec.builder(stringCodec).ofInt(16);
 * }</pre>
 *
 * @param <V> the {@link Number} type this codec handles
 */
public class StringNumberCodec<V extends Number> implements Codec<V> {
  private final Codec<String> stringCodec;
  private final Function<V, String> fromNumber;
  private final Function<String, V> toNumber;

  StringNumberCodec(
      Codec<String> stringCodec, Function<V, String> fromNumber, Function<String, V> toNumber) {
    this.stringCodec = stringCodec;
    this.fromNumber = fromNumber;
    this.toNumber = toNumber;
  }

  /**
   * Returns a new builder for creating a {@link StringNumberCodec} with the specified string codec.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   * @return a new builder
   * @throws NullPointerException if stringCodec is null
   */
  public static Builder builder(Codec<String> stringCodec) {
    return new Builder(stringCodec);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the string representation is rejected by the underlying
   *     codec
   */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    return stringCodec.encode(fromNumber.apply(value), output);
  }

  @Override
  public V decode(InputStream input) throws IOException {
    String string = stringCodec.decode(input);
    try {
      return toNumber.apply(string);
    } catch (NumberFormatException e) {
      throw new CodecException("failed to parse number from string [%s]".formatted(string), e);
    }
  }

  /** A builder for creating {@link StringNumberCodec} instances. */
  public static class Builder {
    private final Codec<String> stringCodec;

    private Builder(Codec<String> stringCodec) {
      this.stringCodec = Objects.requireNonNull(stringCodec, "stringCodec");
    }

    /**
     * Builds a codec for {@link Integer} values with the default radix (10).
     *
     * @return a new {@link StringNumberCodec} for integers
     */
    public StringNumberCodec<Integer> ofInt() {
      return ofInt(10);
    }

    /**
     * Builds a codec for {@link Integer} values with the specified radix.
     *
     * @param radix the radix for number-to-string conversion
     * @return a new {@link StringNumberCodec} for integers
     * @throws IllegalArgumentException if the radix is out of range
     */
    public StringNumberCodec<Integer> ofInt(int radix) {
      validateRadix(radix);
      return new StringNumberCodec<>(
          stringCodec, v -> Integer.toString(v, radix), v -> Integer.valueOf(v, radix));
    }

    /**
     * Builds a codec for {@link Long} values with the default radix (10).
     *
     * @return a new {@link StringNumberCodec} for longs
     */
    public StringNumberCodec<Long> ofLong() {
      return ofLong(10);
    }

    /**
     * Builds a codec for {@link Long} values with the specified radix.
     *
     * @param radix the radix for number-to-string conversion
     * @return a new {@link StringNumberCodec} for longs
     * @throws IllegalArgumentException if the radix is out of range
     */
    public StringNumberCodec<Long> ofLong(int radix) {
      validateRadix(radix);
      return new StringNumberCodec<>(
          stringCodec, v -> Long.toString(v, radix), v -> Long.valueOf(v, radix));
    }

    /**
     * Builds a codec for {@link Short} values with the default radix (10).
     *
     * @return a new {@link StringNumberCodec} for shorts
     */
    public StringNumberCodec<Short> ofShort() {
      return ofShort(10);
    }

    /**
     * Builds a codec for {@link Short} values with the specified radix.
     *
     * @param radix the radix for number-to-string conversion
     * @return a new {@link StringNumberCodec} for shorts
     * @throws IllegalArgumentException if the radix is out of range
     */
    public StringNumberCodec<Short> ofShort(int radix) {
      validateRadix(radix);
      return new StringNumberCodec<>(
          stringCodec, v -> Integer.toString(v, radix), v -> Short.valueOf(v, radix));
    }

    /**
     * Builds a codec for {@link BigInteger} values with the default radix (10).
     *
     * @return a new {@link StringNumberCodec} for big integers
     */
    public StringNumberCodec<BigInteger> ofBigInt() {
      return ofBigInt(10);
    }

    /**
     * Builds a codec for {@link BigInteger} values with the specified radix.
     *
     * @param radix the radix for number-to-string conversion
     * @return a new {@link StringNumberCodec} for big integers
     * @throws IllegalArgumentException if the radix is out of range
     */
    public StringNumberCodec<BigInteger> ofBigInt(int radix) {
      validateRadix(radix);
      return new StringNumberCodec<>(
          stringCodec, v -> v.toString(radix), v -> new BigInteger(v, radix));
    }

    /**
     * Builds a codec for {@link Double} values.
     *
     * @return a new {@link StringNumberCodec} for doubles
     */
    public StringNumberCodec<Double> ofDouble() {
      return new StringNumberCodec<>(stringCodec, Object::toString, Double::valueOf);
    }

    /**
     * Builds a codec for {@link Float} values.
     *
     * @return a new {@link StringNumberCodec} for floats
     */
    public StringNumberCodec<Float> ofFloat() {
      return new StringNumberCodec<>(stringCodec, Object::toString, Float::valueOf);
    }

    /**
     * Builds a codec for {@link BigDecimal} values.
     *
     * @return a new {@link StringNumberCodec} for big decimals
     */
    public StringNumberCodec<BigDecimal> ofBigDecimal() {
      return new StringNumberCodec<>(stringCodec, BigDecimal::toPlainString, BigDecimal::new);
    }

    private static void validateRadix(int radix) {
      Preconditions.check(
          radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX,
          "radix must be between %d and %d, but was [%d]",
          Character.MIN_RADIX,
          Character.MAX_RADIX,
          radix);
    }
  }
}
