package io.bytestreams.codec.core;

import java.nio.ByteBuffer;

/**
 * A codec for signed short values (-32768 to 32767).
 */
public class ShortCodec extends BinaryNumberCodec<Short> {

  public ShortCodec() {
    super(Short.BYTES);
  }

  @Override
  protected byte[] toBytes(Short value) {
    return ByteBuffer.allocate(Short.BYTES).putShort(value).array();
  }

  @Override
  protected Short fromBytes(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getShort();
  }
}
