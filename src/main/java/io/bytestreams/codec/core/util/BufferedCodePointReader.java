package io.bytestreams.codec.core.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Code point reader optimized for streams that support mark/reset.
 *
 * <p>This implementation reads bytes in bulk, converts to a string using {@link Charset}, then
 * resets the stream to the exact byte position after the last complete code point.
 */
class BufferedCodePointReader implements CodePointReader {
  private final InputStream input;
  private final Charset charset;

  BufferedCodePointReader(InputStream input, Charset charset) {
    this.input = input;
    this.charset = charset;
  }

  @Override
  public String read(int count) throws IOException {
    Preconditions.check(count >= 0, "count must be non-negative, but was [%d]", count);
    if (count == 0) {
      return "";
    }

    int maxBytes = count * 4;
    input.mark(maxBytes);

    byte[] buffer = input.readNBytes(maxBytes);
    if (buffer.length == 0) {
      throw new EOFException("Read 0 code point(s), expected %d".formatted(count));
    }

    String decoded = new String(buffer, charset);
    int availableCodePoints = decoded.codePointCount(0, decoded.length());

    if (availableCodePoints < count) {
      throw new EOFException(
          "Read %d code point(s), expected %d".formatted(availableCodePoints, count));
    }

    int charOffset = decoded.offsetByCodePoints(0, count);
    String resultString = decoded.substring(0, charOffset);
    int bytesConsumed = resultString.getBytes(charset).length;

    if (bytesConsumed > buffer.length) {
      throw new EOFException(
          "Read %d code point(s), expected %d".formatted(availableCodePoints, count));
    }

    input.reset();
    long skipped = input.skip(bytesConsumed);
    if (skipped != bytesConsumed) {
      throw new IOException(
          "Failed to skip %d bytes, only skipped %d".formatted(bytesConsumed, skipped));
    }

    return resultString;
  }
}
