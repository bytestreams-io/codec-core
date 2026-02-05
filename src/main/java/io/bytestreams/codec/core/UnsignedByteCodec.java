package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A codec for unsigned byte values (0-255).
 */
public class UnsignedByteCodec implements Codec<Integer> {
  private static final String ERROR_MESSAGE = "value must be between 0 and 255, but was [%d]";

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the value is not between 0 and 255
   */
  @Override
  public void encode(Integer value, OutputStream output) throws IOException {
    Preconditions.check(value <= 0xFF, ERROR_MESSAGE, value);
    Preconditions.check(value >= 0x00, ERROR_MESSAGE, value);
    output.write(value.byteValue());
  }

  /**
   * {@inheritDoc}
   *
   * @throws java.io.EOFException if the stream is empty
   */
  @Override
  public Integer decode(InputStream input) throws IOException {
    return InputStreams.readFully(input, 1)[0] & 0xFF;
  }
}
