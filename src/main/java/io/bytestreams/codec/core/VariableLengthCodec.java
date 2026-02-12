package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * A codec for variable-length values where the length is encoded as a prefix.
 *
 * <p>The {@code lengthCodec} handles the length prefix: on encode it receives the value codec's
 * {@link EncodeResult} and encodes the corresponding length prefix; on decode it decodes the length
 * prefix and returns an {@link EncodeResult} whose {@link EncodeResult#bytes() bytes()} is the byte
 * size of the value that follows. The {@code valueCodec} handles both encoding and decoding the
 * value. On decode, the value bytes are read into a bounded byte array and passed to the {@code
 * valueCodec}.
 *
 * @param <V> the type of value this codec handles
 */
public class VariableLengthCodec<V> implements Codec<V> {
  private final Codec<EncodeResult> lengthCodec;
  private final Codec<V> valueCodec;

  /**
   * Creates a new variable-length codec.
   *
   * @param lengthCodec the codec for the length prefix; {@code encode} receives the value codec's
   *     {@link EncodeResult} and encodes the corresponding length prefix, {@code decode} decodes
   *     the length prefix and returns an {@link EncodeResult} whose {@link EncodeResult#bytes()
   *     bytes()} is the byte size of the value that follows
   * @param valueCodec the codec for encoding and decoding the value
   * @throws NullPointerException if any argument is null
   */
  public VariableLengthCodec(Codec<EncodeResult> lengthCodec, Codec<V> valueCodec) {
    this.lengthCodec = Objects.requireNonNull(lengthCodec, "lengthCodec");
    this.valueCodec = Objects.requireNonNull(valueCodec, "valueCodec");
  }

  /**
   * {@inheritDoc}
   *
   * <p>Encodes the value to a buffer first, then passes the buffer size to the {@code lengthCodec}
   * to write the length prefix, followed by the buffered value bytes.
   */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    EncodeResult valueResult = valueCodec.encode(value, buffer);
    EncodeResult prefixResult = lengthCodec.encode(valueResult, output);
    buffer.writeTo(output);
    return new EncodeResult(valueResult.length(), prefixResult.bytes() + valueResult.bytes());
  }

  /**
   * {@inheritDoc}
   *
   * <p>First decodes the length prefix, then reads that many bytes from the input and decodes the
   * value from the bounded byte array.
   *
   * @throws java.io.EOFException if the stream ends before the length or value is fully read
   */
  @Override
  public V decode(InputStream input) throws IOException {
    EncodeResult lengthResult = lengthCodec.decode(input);
    byte[] data = InputStreams.readFully(input, lengthResult.bytes());
    return valueCodec.decode(new ByteArrayInputStream(data));
  }
}
