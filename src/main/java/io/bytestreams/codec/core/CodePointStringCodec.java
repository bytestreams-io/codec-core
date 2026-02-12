package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.CodePointReader;
import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * A codec for fixed-length character strings.
 *
 * <p>The length is specified in code points, not bytes or chars. For single-byte charsets (e.g.,
 * ISO-8859-1, US-ASCII), decoding uses simple byte-to-string conversion. For multi-byte charsets
 * (e.g., UTF-8, UTF-16), decoding uses {@link CodePointReader} to read exactly the required number
 * of code points.
 */
public class CodePointStringCodec implements FixedLengthCodec<String> {
  private final int length;
  private final Charset charset;
  private final CharsetDecoder decoder;
  private final boolean singleByteCharset;

  /**
   * Creates a new character string codec.
   *
   * @param length the number of code points to read/write (must be non-negative)
   * @param charset the charset to use for encoding and decoding
   * @throws IllegalArgumentException if length is negative
   */
  public CodePointStringCodec(int length, Charset charset) {
    this(length, charset.newDecoder());
  }

  /**
   * Creates a new character string codec with a custom decoder.
   *
   * @param length the number of code points to read/write (must be non-negative)
   * @param decoder the decoder to use for decoding (charset is derived from decoder)
   * @throws IllegalArgumentException if length is negative
   */
  public CodePointStringCodec(int length, CharsetDecoder decoder) {
    Preconditions.check(length >= 0, "length must be non-negative, but was [%d]", length);
    this.length = length;
    this.charset = decoder.charset();
    this.decoder = decoder;
    this.singleByteCharset = (int) charset.newEncoder().maxBytesPerChar() == 1;
  }

  /**
   * {@inheritDoc}
   *
   * @return the number of code points
   */
  @Override
  public int getLength() {
    return length;
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
      return CodePointReader.create(input, decoder).read(length);
    }
  }
}
