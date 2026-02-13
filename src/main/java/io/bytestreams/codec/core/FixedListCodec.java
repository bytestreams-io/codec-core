package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A codec for encoding and decoding fixed-length lists of values.
 *
 * <p>This codec encodes and decodes exactly {@code length} items. Unlike {@link StreamListCodec},
 * which reads items until EOF, this codec reads a fixed number of items from the stream.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Decode exactly 3 items
 * FixedListCodec<String> codec = new FixedListCodec<>(
 *     FixedCodePointStringCodec.builder(5).build(), 3);
 * codec.encode(List.of("hello", "world", "abcde"), output);
 * List<String> values = codec.decode(input);
 * }</pre>
 *
 * @param <V> the type of values in the list
 */
public class FixedListCodec<V> implements FixedLengthCodec<List<V>> {
  private final Codec<V> itemCodec;
  private final int length;
  private final Supplier<List<V>> listFactory;

  /**
   * Creates a new fixed list codec that uses {@link ArrayList} for decoded lists.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @param length the exact number of items to encode/decode (must be non-negative)
   * @throws NullPointerException if itemCodec is null
   * @throws IllegalArgumentException if length is negative
   */
  public FixedListCodec(Codec<V> itemCodec, int length) {
    this(itemCodec, length, ArrayList::new);
  }

  /**
   * Creates a new fixed list codec with a custom list factory.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @param length the exact number of items to encode/decode (must be non-negative)
   * @param listFactory a factory that creates new list instances for decoding
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if length is negative
   */
  public FixedListCodec(Codec<V> itemCodec, int length, Supplier<List<V>> listFactory) {
    this.itemCodec = Objects.requireNonNull(itemCodec, "itemCodec");
    Preconditions.check(length >= 0, "length must be non-negative, but was [%d]", length);
    this.length = length;
    this.listFactory = Objects.requireNonNull(listFactory, "listFactory");
  }

  /**
   * {@inheritDoc}
   *
   * @return the number of items
   */
  @Override
  public int getLength() {
    return length;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Encodes each item in the list sequentially using the item codec.
   *
   * @throws IllegalArgumentException if the list size does not equal {@code length}
   */
  @Override
  public EncodeResult encode(List<V> values, OutputStream output) throws IOException {
    Preconditions.check(
        values.size() == length, "list must have %d items, but had [%d]", length, values.size());
    int totalBytes = 0;
    for (V value : values) {
      totalBytes += itemCodec.encode(value, output).bytes();
    }
    return new EncodeResult(length, totalBytes);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Decodes exactly {@code length} items from the stream.
   *
   * @throws java.io.EOFException if the stream ends before all items are read
   */
  @Override
  public List<V> decode(InputStream input) throws IOException {
    List<V> values = Objects.requireNonNull(listFactory.get(), "listFactory.get() returned null");
    for (int i = 0; i < length; i++) {
      values.add(itemCodec.decode(input));
    }
    return values;
  }
}
