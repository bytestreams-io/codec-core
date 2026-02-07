package io.bytestreams.codec.core;

/**
 * A {@link Codec} that encodes and decodes fixed-length values.
 *
 * @param <V> the type of value this codec handles
 */
public interface FixedLengthCodec<V> extends Codec<V> {

  /**
   * Returns the fixed length of values this codec handles.
   *
   * @return the fixed length
   */
  int getLength();
}
