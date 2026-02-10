package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A codec for unsigned byte values (0-255).
 */
public class UnsignedByteCodec extends BinaryNumberCodec<Integer> {
  private static final String ERROR_MESSAGE = "value must be between 0 and 255, but was [%d]";

  public UnsignedByteCodec() {
    super(Byte.BYTES);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the value is not between 0 and 255
   */
  @Override
  public void encode(Integer value, OutputStream output) throws IOException {
    Preconditions.check(value >= 0x00, ERROR_MESSAGE, value);
    Preconditions.check(value <= 0xFF, ERROR_MESSAGE, value);
    super.encode(value, output);
  }

  @Override
  protected byte[] toBytes(Integer value) {
    return new byte[] {value.byteValue()};
  }

  @Override
  protected Integer fromBytes(byte[] bytes) {
    return bytes[0] & 0xFF;
  }
}
