package io.bytestreams.codec.core;

/**
 * Factory for creating list codecs.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Stream list codec (reads items until EOF)
 * Codec<List<String>> stream = ListCodecs.of(stringCodec);
 *
 * // Fixed-length list codec (reads exactly 3 items)
 * FixedLengthCodec<List<String>> fixed = ListCodecs.of(stringCodec, 3);
 * }</pre>
 */
public class ListCodecs {
  private ListCodecs() {}

  /**
   * Creates a stream list codec that reads items until EOF.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @param <V> the type of values in the list
   * @return a new stream list codec
   */
  public static <V> StreamListCodec<V> of(Codec<V> itemCodec) {
    return new StreamListCodec<>(itemCodec);
  }

  /**
   * Creates a fixed-length list codec that reads exactly {@code length} items.
   *
   * @param itemCodec the codec for encoding/decoding individual list items
   * @param length the exact number of items to encode/decode
   * @param <V> the type of values in the list
   * @return a new fixed list codec
   */
  public static <V> FixedListCodec<V> of(Codec<V> itemCodec, int length) {
    return new FixedListCodec<>(itemCodec, length);
  }
}
