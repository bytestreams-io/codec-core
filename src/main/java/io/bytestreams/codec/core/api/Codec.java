package io.bytestreams.codec.core.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract base class for encoding and decoding values to and from byte streams.
 *
 * @param <V> the type of value this codec handles
 */
public interface Codec<V> {

  /**
   * Encodes the given value and writes it to the output stream.
   *
   * @param value the value to encode
   * @param output the output stream to write the encoded bytes to
   * @throws IOException if an I/O error occurs during encoding
   */
  void encode(V value, OutputStream output) throws IOException;

  /**
   * Decodes a value from the input stream.
   *
   * @param input the input stream to read the encoded bytes from
   * @return the decoded value
   * @throws IOException if an I/O error occurs during decoding
   */
  V decode(InputStream input) throws IOException;
}
