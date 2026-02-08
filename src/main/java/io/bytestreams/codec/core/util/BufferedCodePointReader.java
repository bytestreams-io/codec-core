package io.bytestreams.codec.core.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;

/**
 * Code point reader optimized for streams that support mark/reset.
 *
 * <p>This implementation reads bytes in bulk, decodes using {@link CharsetDecoder}, then resets
 * the stream to the exact byte position after the last complete code point.
 */
class BufferedCodePointReader implements CodePointReader {
  private final InputStream input;
  private final CharsetDecoder decoder;

  BufferedCodePointReader(InputStream input, CharsetDecoder decoder) {
    this.input = input;
    this.decoder = decoder;
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

    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
    CharBuffer charBuffer = CharBuffer.allocate(count * 2);

    CoderResult result = decoder.decode(byteBuffer, charBuffer, false);
    if (result.isMalformed()) {
      throw new MalformedInputException(result.length());
    }
    charBuffer.flip();

    StringBuilder sb = new StringBuilder(count);
    int codePointsRead = 0;

    while (codePointsRead < count && charBuffer.hasRemaining()) {
      char c = charBuffer.get();
      if (Character.isHighSurrogate(c)) {
        sb.appendCodePoint(Character.toCodePoint(c, charBuffer.get()));
      } else {
        sb.appendCodePoint(c);
      }
      codePointsRead++;
    }

    if (codePointsRead < count) {
      throw new EOFException("Read %d code point(s), expected %d".formatted(codePointsRead, count));
    }

    String resultString = sb.toString();
    int bytesConsumed = resultString.getBytes(decoder.charset()).length;

    input.reset();
    long skipped = input.skip(bytesConsumed);
    if (skipped != bytesConsumed) {
      throw new IOException(
          "Failed to skip %d bytes, only skipped %d".formatted(bytesConsumed, skipped));
    }

    return resultString;
  }
}
