package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * A codec for variable-length values where the byte count is encoded as a prefix.
 *
 * <p>The {@code lengthCodec} encodes and decodes the byte count of the value. On encode, the value
 * is first written to a buffer to determine its byte size, then the byte count prefix is written
 * followed by the buffered value bytes. On decode, the byte count prefix is decoded, that many
 * bytes are read into a bounded buffer, and the value is decoded from the buffer.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Codec<String> varString = Codecs.prefixed(Codecs.uint16(), stringCodec);
 * Codec<byte[]> varBinary = Codecs.prefixed(Codecs.uint16(), binaryCodec);
 * }</pre>
 *
 * @param <V> the type of value this codec handles
 */
public class VariableByteLengthCodec<V> implements Codec<V> {
  private final Codec<Integer> lengthCodec;
  private final Codec<V> valueCodec;

  /**
   * Creates a new variable byte-length codec.
   *
   * @param lengthCodec the codec for the byte count prefix
   * @param valueCodec the codec for encoding and decoding the value
   * @throws NullPointerException if any argument is null
   */
  VariableByteLengthCodec(Codec<Integer> lengthCodec, Codec<V> valueCodec) {
    this.lengthCodec = Objects.requireNonNull(lengthCodec, "lengthCodec");
    this.valueCodec = Objects.requireNonNull(valueCodec, "valueCodec");
  }

  /**
   * {@inheritDoc}
   *
   * <p>Encodes the value to a buffer first, then writes the byte count prefix followed by the
   * buffered value bytes.
   */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    EncodeResult valueResult = valueCodec.encode(value, buffer);
    EncodeResult prefixResult = lengthCodec.encode(valueResult.bytes(), output);
    buffer.writeTo(output);
    return new EncodeResult(valueResult.count(), prefixResult.bytes() + valueResult.bytes());
  }

  /**
   * {@inheritDoc}
   *
   * <p>First decodes the byte count prefix, then reads that many bytes from the input and decodes
   * the value from the bounded byte array.
   *
   * @throws java.io.EOFException if the stream ends before the length or value is fully read
   */
  @Override
  public V decode(InputStream input) throws IOException {
    int length = lengthCodec.decode(input);
    byte[] data = InputStreams.readFully(input, length);
    return valueCodec.decode(new ByteArrayInputStream(data));
  }
}
