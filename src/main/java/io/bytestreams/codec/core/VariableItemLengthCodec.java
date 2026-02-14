package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * A codec for variable-length values where the item count is encoded as a prefix.
 *
 * <p>The {@code lengthCodec} encodes and decodes the item count. The {@code codecFactory} creates a
 * codec for the given item count, handling both encoding and decoding. Unlike {@link
 * VariableByteLengthCodec}, this codec does not buffer the encoded value; it writes directly to the
 * output stream.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // String with code point count prefix
 * VariableItemLengthCodec<String> codec = VariableItemLengthCodec
 *     .builder(BinaryNumberCodec.ofUnsignedByte())
 *     .of(Strings::codePointCount,
 *         length -> FixedCodePointStringCodec.builder(length).build());
 *
 * // List with item count prefix
 * VariableItemLengthCodec<List<Foo>> codec = VariableItemLengthCodec
 *     .builder(BinaryNumberCodec.ofUnsignedShort())
 *     .of(List::size,
 *         length -> new FixedListCodec<>(fooCodec, length));
 * }</pre>
 *
 * @param <V> the type of value this codec handles
 */
public class VariableItemLengthCodec<V> implements Codec<V> {
  private final Codec<Integer> lengthCodec;
  private final ToIntFunction<V> lengthOf;
  private final IntFunction<Codec<V>> codecFactory;

  VariableItemLengthCodec(
      Codec<Integer> lengthCodec, ToIntFunction<V> lengthOf, IntFunction<Codec<V>> codecFactory) {
    this.lengthCodec = lengthCodec;
    this.lengthOf = lengthOf;
    this.codecFactory = codecFactory;
  }

  /**
   * Returns a new builder for creating {@link VariableItemLengthCodec} instances with the specified
   * length codec.
   *
   * @param lengthCodec the codec for the item count prefix
   * @return a new builder
   * @throws NullPointerException if lengthCodec is null
   */
  public static Builder builder(Codec<Integer> lengthCodec) {
    return new Builder(lengthCodec);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Computes the item count using {@code lengthOf}, writes the count prefix, then encodes the
   * value directly using a codec created by {@code codecFactory}.
   */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    int length = lengthOf.applyAsInt(value);
    EncodeResult prefixResult = lengthCodec.encode(length, output);
    EncodeResult valueResult = codecFactory.apply(length).encode(value, output);
    return new EncodeResult(valueResult.length(), prefixResult.bytes() + valueResult.bytes());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Decodes the item count prefix, then decodes the value using a codec created by {@code
   * codecFactory}.
   *
   * @throws java.io.EOFException if the stream ends before the length or value is fully read
   */
  @Override
  public V decode(InputStream input) throws IOException {
    int length = lengthCodec.decode(input);
    return codecFactory.apply(length).decode(input);
  }

  /** A builder for creating {@link VariableItemLengthCodec} instances. */
  public static class Builder {
    private final Codec<Integer> lengthCodec;

    private Builder(Codec<Integer> lengthCodec) {
      this.lengthCodec = Objects.requireNonNull(lengthCodec, "lengthCodec");
    }

    /**
     * Builds a new {@link VariableItemLengthCodec} with the specified length function and codec
     * factory.
     *
     * @param <V> the type of value the codec handles
     * @param lengthOf a function that returns the item count for a given value
     * @param codecFactory a function that creates a codec for the given item count
     * @return a new codec instance
     * @throws NullPointerException if any argument is null
     */
    public <V> VariableItemLengthCodec<V> of(
        ToIntFunction<V> lengthOf, IntFunction<Codec<V>> codecFactory) {
      return new VariableItemLengthCodec<>(
          lengthCodec,
          Objects.requireNonNull(lengthOf, "lengthOf"),
          Objects.requireNonNull(codecFactory, "codecFactory"));
    }
  }
}
