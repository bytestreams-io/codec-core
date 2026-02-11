package io.bytestreams.codec.core;

import java.nio.ByteBuffer;

/**
 * A codec for double values (IEEE 754 double-precision, 8 bytes).
 */
public class DoubleCodec extends BinaryNumberCodec<Double> {

  public DoubleCodec() {
    super(Double.BYTES);
  }

  @Override
  protected byte[] toBytes(Double value) {
    return ByteBuffer.allocate(Double.BYTES).putDouble(value).array();
  }

  @Override
  protected Double fromBytes(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getDouble();
  }
}
