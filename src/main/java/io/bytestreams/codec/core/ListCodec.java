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
 * A codec for encoding and decoding lists of values.
 *
 * <p>This codec encodes each item in the list sequentially using the item codec. When decoding, it
 * reads items from the stream until EOF is reached or the maximum number of items is decoded.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Using default ArrayList
 * ListCodec<String> codec = new ListCodec<>(new CodePointStringCodec(5, UTF_8));
 * codec.encode(List.of("hello", "world"), output);
 * List<String> values = codec.decode(input);
 *
 * // Using custom list supplier
 * ListCodec<String> linkedCodec = new ListCodec<>(
 *     new CodePointStringCodec(5, UTF_8),
 *     LinkedList::new);
 *
 * // With maximum items limit
 * ListCodec<String> limitedCodec = new ListCodec<>(
 *     new CodePointStringCodec(5, UTF_8),
 *     100);
 * }</pre>
 *
 * @param <V> the type of values in the list
 */
public class ListCodec<V> implements Codec<List<V>> {
  private final Codec<V> itemCodec;
  private final Supplier<List<V>> listSupplier;
  private final int maxItems;

  /**
   * Creates a new list codec that uses {@link ArrayList} for decoded lists.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @throws NullPointerException if itemCodec is null
   */
  public ListCodec(Codec<V> itemCodec) {
    this(itemCodec, ArrayList::new, Integer.MAX_VALUE);
  }

  /**
   * Creates a new list codec with a custom list supplier.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @param listSupplier a supplier that creates new list instances for decoding
   * @throws NullPointerException if any argument is null
   */
  public ListCodec(Codec<V> itemCodec, Supplier<List<V>> listSupplier) {
    this(itemCodec, listSupplier, Integer.MAX_VALUE);
  }

  /**
   * Creates a new list codec with a maximum items limit.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @param maxItems the maximum number of items to decode
   * @throws NullPointerException if itemCodec is null
   * @throws IllegalArgumentException if maxItems is negative
   */
  public ListCodec(Codec<V> itemCodec, int maxItems) {
    this(itemCodec, ArrayList::new, maxItems);
  }

  /**
   * Creates a new list codec with a custom list supplier and maximum items limit.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @param listSupplier a supplier that creates new list instances for decoding
   * @param maxItems the maximum number of items to decode
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if maxItems is negative
   */
  public ListCodec(Codec<V> itemCodec, Supplier<List<V>> listSupplier, int maxItems) {
    this.itemCodec = Objects.requireNonNull(itemCodec, "itemCodec");
    this.listSupplier = Objects.requireNonNull(listSupplier, "listSupplier");
    if (maxItems < 0) {
      throw new IllegalArgumentException(
          "maxItems must be non-negative, but was [%d]".formatted(maxItems));
    }
    this.maxItems = maxItems;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Encodes each item in the list sequentially using the item codec.
   */
  @Override
  public void encode(List<V> values, OutputStream output) throws IOException {
    for (V value : values) {
      itemCodec.encode(value, output);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Decodes items from the stream until EOF is reached or the maximum number of items is
   * decoded.
   *
   * @throws java.io.EOFException if the stream ends mid-item
   */
  @Override
  public List<V> decode(InputStream input) throws IOException {
    List<V> values = Objects.requireNonNull(listSupplier.get(), "listSupplier.get() returned null");
    PushbackInputStream pushback = new PushbackInputStream(input);
    int next;
    while (values.size() < maxItems && (next = pushback.read()) != -1) {
      pushback.unread(next);
      values.add(itemCodec.decode(pushback));
    }
    return values;
  }
}
