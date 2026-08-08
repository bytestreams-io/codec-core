package io.bytestreams.codec.core.util;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Utility methods for working with input streams.
 */
public final class InputStreams {
  private static final String END_OF_BYTE_STREAM_REACHED =
      "End of stream reached after reading %d bytes, bytes expected [%d]";

  private InputStreams() {}

  /**
   * Returns a stream that supports {@link InputStream#mark(int) mark}, wrapping the given stream
   * only if it does not already.
   *
   * <p>Composite codecs that peek before decoding an item should pass the result down rather than
   * the original stream, so that combinators needing to rewind can be nested inside them.
   * {@link java.io.ByteArrayInputStream} and {@link BufferedInputStream} already support mark and
   * are returned unchanged; {@link java.io.FileInputStream}, socket streams and
   * {@link java.io.PushbackInputStream} do not.
   *
   * @param input the stream to make markable
   * @return {@code input} if it supports mark, otherwise a buffered wrapper
   * @throws NullPointerException if input is null
   */
  public static InputStream markable(InputStream input) {
    Objects.requireNonNull(input, "input");
    return input.markSupported() ? input : new BufferedInputStream(input);
  }

  /**
   * Reports whether the stream is exhausted, consuming nothing when it is not.
   *
   * <p>The stream must support {@link InputStream#mark(int) mark}; pass it through
   * {@link #markable(InputStream)} first.
   *
   * @param input a markable stream
   * @return true if the next read would return end-of-stream
   * @throws IOException if an I/O error occurs
   */
  public static boolean atEndOfStream(InputStream input) throws IOException {
    input.mark(1);
    if (input.read() == -1) {
      return true;
    }
    input.reset();
    return false;
  }

  /**
   * Reads exactly the specified number of bytes from the input stream.
   *
   * @param input the input stream to read from
   * @param length the exact number of bytes to read
   * @return a byte array containing the bytes read
   * @throws IllegalArgumentException if length is negative
   * @throws IOException if an I/O error occurs
   * @throws EOFException if the stream ends before the required bytes are read
   */
  public static byte[] readFully(InputStream input, int length) throws IOException {
    Preconditions.check(length >= 0, "length must be non-negative, but was [%d]", length);
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
