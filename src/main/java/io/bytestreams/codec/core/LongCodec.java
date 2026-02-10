package io.bytestreams.codec.core;

import java.nio.ByteBuffer;

/**
 * A codec for signed long values (-9223372036854775808 to 9223372036854775807).
 */
public class LongCodec extends BinaryNumberCodec<Long> {

  public LongCodec() {
    super(Long.BYTES);
  }

  @Override
  protected byte[] toBytes(Long value) {
    return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
  }

  @Override
  protected Long fromBytes(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getLong();
  }
}
