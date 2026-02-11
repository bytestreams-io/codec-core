package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * A codec for unsigned integer values (0 to 4294967295).
 */
public class UnsignedIntegerCodec extends BinaryNumberCodec<Long> {
  private static final String ERROR_MESSAGE =
      "value must be between 0 and 4294967295, but was [%d]";

  public UnsignedIntegerCodec() {
    super(Integer.BYTES);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the value is not between 0 and 4294967295
   */
  @Override
  public void encode(Long value, OutputStream output) throws IOException {
    Preconditions.check(value >= 0x00000000L, ERROR_MESSAGE, value);
    Preconditions.check(value <= 0xFFFFFFFFL, ERROR_MESSAGE, value);
    super.encode(value, output);
  }

  @Override
  protected byte[] toBytes(Long value) {
    return ByteBuffer.allocate(Integer.BYTES).putInt(value.intValue()).array();
  }

  @Override
  protected Long fromBytes(byte[] bytes) {
    return Integer.toUnsignedLong(ByteBuffer.wrap(bytes).getInt());
  }
}
