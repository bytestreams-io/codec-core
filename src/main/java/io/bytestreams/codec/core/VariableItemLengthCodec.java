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
 * Codec<String> string = Codecs.prefixed(Codecs.uint8(),
 *     Strings::codePointCount,
 *     length -> Codecs.ofCharset(charset, length));
 *
 * // List with item count prefix
 * Codec<List<Foo>> list = Codecs.prefixed(Codecs.uint16(),
 *     List::size,
 *     length -> Codecs.listOf(fooCodec, length));
 * }</pre>
 *
 * @param <V> the type of value this codec handles
 */
public class VariableItemLengthCodec<V> implements Codec<V> {
  private final Codec<Integer> lengthCodec;
  private final ToIntFunction<V> lengthOf;
  private final IntFunction<Codec<V>> codecFactory;

  /**
   * Creates a new variable item-length codec.
   *
   * @param lengthCodec the codec for the item count prefix
   * @param lengthOf a function that returns the item count for a given value
   * @param codecFactory a function that creates a codec for the given item count
   * @throws NullPointerException if any argument is null
   */
  VariableItemLengthCodec(
      Codec<Integer> lengthCodec, ToIntFunction<V> lengthOf, IntFunction<Codec<V>> codecFactory) {
    this.lengthCodec = Objects.requireNonNull(lengthCodec, "lengthCodec");
    this.lengthOf = Objects.requireNonNull(lengthOf, "lengthOf");
    this.codecFactory = Objects.requireNonNull(codecFactory, "codecFactory");
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
    return new EncodeResult(valueResult.count(), prefixResult.bytes() + valueResult.bytes());
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
}
