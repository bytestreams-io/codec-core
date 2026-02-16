package io.bytestreams.codec.core.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
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
  private final Charset charset;

  UnbufferedCodePointReader(InputStream input, Charset charset) {
    this.input = input;
    this.charset = charset;
  }

  @Override
  public String read(int count) throws IOException {
    Preconditions.check(count >= 0, "count must be non-negative, but was [%d]", count);
    if (count == 0) {
      return "";
    }
    CharsetDecoder decoder = charset.newDecoder();
    // max 4 bytes per code point in any Unicode encoding
    ByteBuffer byteBuffer = ByteBuffer.allocate(4);
    CharBuffer charBuffer = CharBuffer.allocate(2);
    StringBuilder result = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      int codePoint = readCodePoint(decoder, byteBuffer, charBuffer);
      if (codePoint == -1) {
        throw new EOFException("Read %d code point(s), expected %d".formatted(i, count));
      }
      result.appendCodePoint(codePoint);
    }
    return result.toString();
  }

  private int readCodePoint(CharsetDecoder decoder, ByteBuffer byteBuffer, CharBuffer charBuffer)
      throws IOException {
    byteBuffer.clear();
    while (true) {
      int nextByte = input.read();
      if (nextByte == -1) {
        if (byteBuffer.position() == 0) {
          return -1;
        }
        throw new EOFException(
            "Incomplete code point after %d byte(s)".formatted(byteBuffer.position()));
      }

      byteBuffer.put((byte) nextByte);
      byteBuffer.flip();
      charBuffer.clear();

      CoderResult coderResult = decoder.decode(byteBuffer, charBuffer, false);
      if (charBuffer.position() > 0) {
        return toCodePoint(charBuffer);
      }
      handleCoderResult(coderResult);
      byteBuffer.compact();
    }
  }

  private static int toCodePoint(CharBuffer charBuffer) {
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
