package io.bytestreams.codec.core;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A codec that always throws, indicating no implementation has been provided.
 *
 * @param <V> the value type
 */
public class NotImplementedCodec<V> implements Codec<V> {
  @Override
  public void encode(V value, OutputStream output) {
    throw new CodecException("codec not implemented", null);
  }

  @Override
  public V decode(InputStream input) {
    throw new CodecException("codec not implemented", null);
  }
}
