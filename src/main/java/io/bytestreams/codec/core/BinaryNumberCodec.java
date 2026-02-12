package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract codec for {@link Number}s encoded as fixed-length big-endian binary.
 *
 * @param <V> the {@link Number} type this codec handles
 */
public abstract class BinaryNumberCodec<V extends Number> implements FixedLengthCodec<V> {
  private final int byteLength;

  /**
   * Creates a new binary number codec with the specified byte length.
   *
   * @param byteLength the number of bytes used to encode the value
   */
  protected BinaryNumberCodec(int byteLength) {
    this.byteLength = byteLength;
  }

  @Override
  public int getLength() {
    return byteLength;
  }

  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    output.write(toBytes(value));
    return EncodeResult.ofBytes(byteLength);
  }

  @Override
  public V decode(InputStream input) throws IOException {
    return fromBytes(InputStreams.readFully(input, byteLength));
  }

  /**
   * Converts a number to its big-endian byte representation.
   *
   * @param value the number to convert
   * @return the byte array representation
   */
  protected abstract byte[] toBytes(V value);

  /**
   * Converts a big-endian byte array to a number.
   *
   * @param bytes the byte array to convert
   * @return the number representation
   */
  protected abstract V fromBytes(byte[] bytes);
}
