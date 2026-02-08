package io.bytestreams.codec.core.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharsetDecoder;

/**
 * Reads code points from an input stream using a charset decoder.
 *
 * <p>Use {@link #create(InputStream, CharsetDecoder)} to obtain an implementation optimized for
 * the stream's capabilities.
 */
public interface CodePointReader {

  /**
   * Creates a code point reader optimized for the given input stream.
   *
   * <p>If the stream supports mark/reset, returns a buffered implementation that reads ahead and
   * resets on over-read. Otherwise, returns a byte-by-byte implementation.
   *
   * @param input the input stream to read from
   * @param decoder the charset decoder to use
   * @return a code point reader
   */
  static CodePointReader create(InputStream input, CharsetDecoder decoder) {
    if (input.markSupported()) {
      return new BufferedCodePointReader(input, decoder);
    }
    return new UnbufferedCodePointReader(input, decoder);
  }

  /**
   * Reads the specified number of code points from the stream.
   *
   * @param count the number of code points to read (must be non-negative)
   * @return the string containing the code points, or empty string if count is 0
   * @throws EOFException if end of stream reached before reading the required code points
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if count is negative
   */
  String read(int count) throws IOException;
}
