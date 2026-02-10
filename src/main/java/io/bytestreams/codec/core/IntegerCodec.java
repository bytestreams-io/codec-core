package io.bytestreams.codec.core;

import java.nio.ByteBuffer;

/**
 * A codec for signed integer values (-2147483648 to 2147483647).
 */
public class IntegerCodec extends BinaryNumberCodec<Integer> {

  public IntegerCodec() {
    super(Integer.BYTES);
  }

  @Override
  protected byte[] toBytes(Integer value) {
    return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
  }

  @Override
  protected Integer fromBytes(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getInt();
  }
}
