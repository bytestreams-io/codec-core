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
 * Code point reader that reads one byte at a time until a complete code point is decoded.
 *
 * <p>This implementation works with any input stream but may be slower than {@link
 * BufferedCodePointReader} for streams that support mark/reset.
 */
class UnbufferedCodePointReader implements CodePointReader {
  private final InputStream input;
  private final CharsetDecoder decoder;
  private final ByteBuffer byteBuffer;
  private final CharBuffer charBuffer;

  UnbufferedCodePointReader(InputStream input, CharsetDecoder decoder) {
    this.input = input;
    this.decoder = decoder;
    this.byteBuffer = ByteBuffer.allocate(8);
    this.charBuffer = CharBuffer.allocate(2);
  }

  @Override
  public String read(int count) throws IOException {
    Preconditions.check(count >= 0, "count must be non-negative, but was [%d]", count);
    if (count == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      int codePoint = readCodePoint();
      if (codePoint == -1) {
        throw new EOFException("Read %d code point(s), expected %d".formatted(i, count));
      }
      result.appendCodePoint(codePoint);
    }
    return result.toString();
  }

  private int readCodePoint() throws IOException {
    int bytesRead = 0;
    while (true) {
      int nextByte = input.read();
      if (nextByte == -1) {
        if (bytesRead == 0) {
          return -1;
        }
        throw new EOFException("Incomplete code point after %d byte(s)".formatted(bytesRead));
      }

      byteBuffer.array()[bytesRead++] = (byte) nextByte;
      byteBuffer.position(0).limit(bytesRead);
      charBuffer.clear();

      CoderResult coderResult = decoder.decode(byteBuffer, charBuffer, false);
      if (charBuffer.position() > 0) {
        return toCodePoint();
      }
      handleCoderResult(coderResult);
    }
  }

  private int toCodePoint() {
    charBuffer.flip();
    char firstChar = charBuffer.get();
    if (Character.isHighSurrogate(firstChar)) {
      return Character.toCodePoint(firstChar, charBuffer.get());
    }
    return firstChar;
  }

  private static void handleCoderResult(CoderResult coderResult) throws MalformedInputException {
    if (coderResult.isMalformed()) {
      throw new MalformedInputException(coderResult.length());
    }
  }
}
