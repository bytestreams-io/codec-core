package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * A codec for unsigned short values (0-65535).
 */
public class UnsignedShortCodec extends BinaryNumberCodec<Integer> {
  private static final String ERROR_MESSAGE = "value must be between 0 and 65535, but was [%d]";

  public UnsignedShortCodec() {
    super(Short.BYTES);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the value is not between 0 and 65535
   */
  @Override
  public void encode(Integer value, OutputStream output) throws IOException {
    Preconditions.check(value >= 0x0000, ERROR_MESSAGE, value);
    Preconditions.check(value <= 0xFFFF, ERROR_MESSAGE, value);
    super.encode(value, output);
  }

  @Override
  protected byte[] toBytes(Integer value) {
    return ByteBuffer.allocate(Short.BYTES).putShort(value.shortValue()).array();
  }

  @Override
  protected Integer fromBytes(byte[] bytes) {
    return (bytes[0] & 0xFF) << 8 | bytes[1] & 0xFF;
  }
}
