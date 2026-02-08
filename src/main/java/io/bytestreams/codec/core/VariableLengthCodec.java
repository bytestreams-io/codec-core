package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * A codec for variable-length values where the length is encoded as a prefix.
 *
 * <p>This codec first encodes/decodes a length value using the length codec, then uses that length
 * to obtain the appropriate value codec from the provider. This enables encoding values of varying
 * sizes where the length is not known until runtime.
 *
 * <p>Example usage with a string codec:
 *
 * <pre>{@code
 * VariableLengthCodec<String> codec = new VariableLengthCodec<>(
 *     new UnsignedByteCodec(),
 *     length -> new CodePointStringCodec(length, UTF_8),
 *     value -> (int) value.codePoints().count());
 * }</pre>
 *
 * @param <V> the type of value this codec handles
 */
public class VariableLengthCodec<V> implements Codec<V> {
  private final Codec<Integer> lengthCodec;
  private final Function<Integer, Codec<V>> valueCodecProvider;
  private final Function<V, Integer> lengthProvider;

  /**
   * Creates a new variable-length codec.
   *
   * @param lengthCodec the codec for encoding/decoding the length prefix
   * @param valueCodecProvider a function that provides the value codec based on the length
   * @param lengthProvider a function that calculates the length of a value for encoding
   * @throws NullPointerException if any argument is null
   */
  public VariableLengthCodec(
      Codec<Integer> lengthCodec,
      Function<Integer, Codec<V>> valueCodecProvider,
      Function<V, Integer> lengthProvider) {
    this.lengthCodec = Objects.requireNonNull(lengthCodec, "lengthCodec");
    this.valueCodecProvider = Objects.requireNonNull(valueCodecProvider, "valueCodecProvider");
    this.lengthProvider = Objects.requireNonNull(lengthProvider, "lengthProvider");
  }

  /**
   * {@inheritDoc}
   *
   * <p>First encodes the length (computed via the length provider), then encodes the value using a
   * codec obtained from the value codec provider.
   */
  @Override
  public void encode(V value, OutputStream output) throws IOException {
    int length = lengthProvider.apply(value);
    lengthCodec.encode(length, output);
    valueCodecProvider.apply(length).encode(value, output);
  }

  /**
   * {@inheritDoc}
   *
   * <p>First decodes the length prefix, then decodes the value using a codec obtained from the
   * value codec provider.
   *
   * @throws java.io.EOFException if the stream ends before the length or value is fully read
   */
  @Override
  public V decode(InputStream input) throws IOException {
    int length = lengthCodec.decode(input);
    return valueCodecProvider.apply(length).decode(input);
  }
}
