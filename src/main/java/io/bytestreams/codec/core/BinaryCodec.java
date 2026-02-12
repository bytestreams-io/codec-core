package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A codec for fixed-length binary data.
 */
public class BinaryCodec implements FixedLengthCodec<byte[]> {
  private static final String ERROR_MESSAGE = "value must be of length %d, but was [%d]";
  private final int length;

  /**
   * Creates a new binary codec with the specified fixed length.
   *
   * @param length the expected length of byte arrays (must be non-negative)
   * @throws IllegalArgumentException if length is negative
   */
  public BinaryCodec(int length) {
    Preconditions.check(length >= 0, "length must be non-negative, but was [%d]", length);
    this.length = length;
  }

  /**
   * {@inheritDoc}
   *
   * @return the number of bytes
   */
  @Override
  public int getLength() {
    return length;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the byte array length does not match the expected length
   */
  @Override
  public EncodeResult encode(byte[] value, OutputStream output) throws IOException {
    Preconditions.check(value.length == length, ERROR_MESSAGE, length, value.length);
    output.write(value);
    return EncodeResult.ofBytes(length);
  }

  /**
   * {@inheritDoc}
   *
   * @throws java.io.EOFException if the stream ends before the required bytes are read
   */
  @Override
  public byte[] decode(InputStream input) throws IOException {
    return InputStreams.readFully(input, length);
  }
}
