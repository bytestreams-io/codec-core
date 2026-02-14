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
 * <p>The builder can be reused to create multiple codecs with different value codecs:
 *
 * <pre>{@code
 * VariableByteLengthCodec.Builder llvar = VariableByteLengthCodec.builder(twoDigitLengthCodec);
 * VariableByteLengthCodec<String> stringCodec = llvar.of(stringCodec);
 * VariableByteLengthCodec<byte[]> binaryCodec = llvar.of(binaryCodec);
 * }</pre>
 *
 * @param <V> the type of value this codec handles
 */
public class VariableByteLengthCodec<V> implements Codec<V> {
  private final Codec<Integer> lengthCodec;
  private final Codec<V> valueCodec;

  VariableByteLengthCodec(Codec<Integer> lengthCodec, Codec<V> valueCodec) {
    this.lengthCodec = lengthCodec;
    this.valueCodec = valueCodec;
  }

  /**
   * Returns a new builder for creating {@link VariableByteLengthCodec} instances with the specified
   * length codec. The builder can be reused to create multiple codecs with different value codecs.
   *
   * @param lengthCodec the codec for the byte count prefix
   * @return a new builder
   * @throws NullPointerException if lengthCodec is null
   */
  public static Builder builder(Codec<Integer> lengthCodec) {
    return new Builder(lengthCodec);
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
    return new EncodeResult(valueResult.length(), prefixResult.bytes() + valueResult.bytes());
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

  /** A builder for creating {@link VariableByteLengthCodec} instances. */
  public static class Builder {
    private final Codec<Integer> lengthCodec;

    private Builder(Codec<Integer> lengthCodec) {
      this.lengthCodec = Objects.requireNonNull(lengthCodec, "lengthCodec");
    }

    /**
     * Builds a new {@link VariableByteLengthCodec} with the specified value codec.
     *
     * @param <V> the type of value the codec handles
     * @param valueCodec the codec for encoding and decoding the value
     * @return a new codec instance
     * @throws NullPointerException if valueCodec is null
     */
    public <V> VariableByteLengthCodec<V> of(Codec<V> valueCodec) {
      return new VariableByteLengthCodec<>(
          lengthCodec, Objects.requireNonNull(valueCodec, "valueCodec"));
    }
  }
}
