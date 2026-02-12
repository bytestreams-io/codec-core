package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A codec for encoding and decoding lists of values by reading items until EOF.
 *
 * <p>This codec encodes each item in the list sequentially using the item codec. When decoding, it
 * reads items from the stream until EOF is reached.
 *
 * <p>Unlike {@link FixedListCodec}, which decodes a fixed number of items, this codec reads all
 * available items. This makes it suitable for use as a value codec inside {@link
 * VariableLengthCodec}, where the stream is bounded by the length prefix.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Using default ArrayList
 * StreamListCodec<String> codec = new StreamListCodec<>(new FixedCodePointStringCodec(5, UTF_8));
 * codec.encode(List.of("hello", "world"), output);
 * List<String> values = codec.decode(input);
 *
 * // Using custom list factory
 * StreamListCodec<String> linkedCodec = new StreamListCodec<>(
 *     new FixedCodePointStringCodec(5, UTF_8),
 *     LinkedList::new);
 * }</pre>
 *
 * @param <V> the type of values in the list
 */
public class StreamListCodec<V> implements Codec<List<V>> {
  private final Codec<V> itemCodec;
  private final Supplier<List<V>> listFactory;

  /**
   * Creates a new stream list codec that uses {@link ArrayList} for decoded lists.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @throws NullPointerException if itemCodec is null
   */
  public StreamListCodec(Codec<V> itemCodec) {
    this(itemCodec, ArrayList::new);
  }

  /**
   * Creates a new stream list codec with a custom list factory.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @param listFactory a factory that creates new list instances for decoding
   * @throws NullPointerException if any argument is null
   */
  public StreamListCodec(Codec<V> itemCodec, Supplier<List<V>> listFactory) {
    this.itemCodec = Objects.requireNonNull(itemCodec, "itemCodec");
    this.listFactory = Objects.requireNonNull(listFactory, "listFactory");
  }

  /**
   * {@inheritDoc}
   *
   * <p>Encodes each item in the list sequentially using the item codec.
   */
  @Override
  public EncodeResult encode(List<V> values, OutputStream output) throws IOException {
    int totalBytes = 0;
    for (V value : values) {
      totalBytes += itemCodec.encode(value, output).bytes();
    }
    return new EncodeResult(values.size(), totalBytes);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Decodes items from the stream until EOF is reached.
   *
   * @throws java.io.EOFException if the stream ends mid-item
   */
  @Override
  public List<V> decode(InputStream input) throws IOException {
    List<V> values = Objects.requireNonNull(listFactory.get(), "listFactory.get() returned null");
    PushbackInputStream pushback = new PushbackInputStream(input);
    int next;
    while ((next = pushback.read()) != -1) {
      pushback.unread(next);
      values.add(itemCodec.decode(pushback));
    }
    return values;
  }
}
