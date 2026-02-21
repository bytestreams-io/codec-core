package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.CodePointReader;
import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * A codec for fixed-length character strings.
 *
 * <p>The length is specified in code points, not bytes or chars. For single-byte charsets (e.g.,
 * ISO-8859-1, US-ASCII), decoding uses simple byte-to-string conversion. For multi-byte charsets
 * (e.g., UTF-8, UTF-16), decoding uses {@link CodePointReader} to read exactly the required number
 * of code points.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Fixed-length ASCII string
 * Codec<String> ascii = Codecs.ascii(5);
 *
 * // Fixed-length UTF-8 string
 * Codec<String> utf8 = Codecs.utf8(5);
 * }</pre>
 */
public class FixedCodePointStringCodec implements Codec<String> {
  private final int length;
  private final Charset charset;
  private final boolean singleByteCharset;

  /**
   * Creates a new fixed-length code point string codec.
   *
   * @param length the number of code points (must be non-negative)
   * @param charset the charset for encoding and decoding
   * @throws IllegalArgumentException if length is negative
   * @throws NullPointerException if charset is null
   */
  FixedCodePointStringCodec(int length, Charset charset) {
    Preconditions.check(length >= 0, "length must be non-negative, but was [%d]", length);
    this.length = length;
    this.charset = Objects.requireNonNull(charset, "charset");
    this.singleByteCharset = charset.newEncoder().maxBytesPerChar() <= 1.0f;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the value does not have exactly {@code length} code points
   */
  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    int codePointCount = value.codePointCount(0, value.length());
    Preconditions.check(
        codePointCount == length,
        "value must have %d code points, but had [%d]",
        length,
        codePointCount);
    byte[] encoded = value.getBytes(charset);
    output.write(encoded);
    return new EncodeResult(length, encoded.length);
  }

  /**
   * {@inheritDoc}
   *
   * @throws EOFException if the stream ends before the required code points are read
   */
  @Override
  public String decode(InputStream input) throws IOException {
    if (singleByteCharset) {
      return new String(InputStreams.readFully(input, length), charset);
    } else {
      return CodePointReader.create(input, charset).read(length);
    }
  }
}
