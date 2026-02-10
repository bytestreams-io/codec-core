package io.bytestreams.codec.core;

import java.nio.ByteBuffer;

/**
 * A codec for float values (IEEE 754 single-precision, 4 bytes).
 */
public class FloatCodec extends BinaryNumberCodec<Float> {

  public FloatCodec() {
    super(Float.BYTES);
  }

  @Override
  protected byte[] toBytes(Float value) {
    return ByteBuffer.allocate(Float.BYTES).putFloat(value).array();
  }

  @Override
  protected Float fromBytes(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getFloat();
  }
}
