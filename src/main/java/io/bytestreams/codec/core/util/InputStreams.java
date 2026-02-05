package io.bytestreams.codec.core.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for working with input streams.
 */
public class InputStreams {
  private static final String END_OF_BYTE_STREAM_REACHED =
      "End of stream reached after reading %d bytes, bytes expected [%d]";

  private InputStreams() {}

  /**
   * Reads exactly the specified number of bytes from the input stream.
   *
   * @param input the input stream to read from
   * @param length the exact number of bytes to read
   * @return a byte array containing the bytes read
   * @throws IOException if an I/O error occurs
   * @throws EOFException if the stream ends before the required bytes are read
   */
  public static byte[] readFully(InputStream input, int length) throws IOException {
    byte[] bytes = new byte[length];
    int total = 0;
    while (total < length) {
      int read = input.read(bytes, total, length - total);
      if (read == -1) {
        break;
      } else {
        total += read;
      }
    }
    if (total != length) {
      throw new EOFException(String.format(END_OF_BYTE_STREAM_REACHED, total, length));
    }
    return bytes;
  }
}
