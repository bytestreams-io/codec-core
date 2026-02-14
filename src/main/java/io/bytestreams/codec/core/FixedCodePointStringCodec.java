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
 * // Using default charset
 * FixedCodePointStringCodec codec = FixedCodePointStringCodec.builder(5).build();
 *
 * // Using a custom decoder
 * FixedCodePointStringCodec codec = FixedCodePointStringCodec.builder(5)
 *     .decoder(UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE))
 *     .build();
 * }</pre>
 */
public class FixedCodePointStringCodec implements FixedLengthCodec<String> {
  private final int length;
  private final Charset charset;
  private final CharsetDecoder decoder;
  private final boolean singleByteCharset;

  FixedCodePointStringCodec(int length, CharsetDecoder decoder) {
    this.length = length;
    this.charset = decoder.charset();
    this.decoder = decoder;
    this.singleByteCharset = charset.newEncoder().maxBytesPerChar() <= 1.0f;
  }

  /**
   * Returns a new builder for creating a {@link FixedCodePointStringCodec} with the specified
   * length.
   *
   * @param length the number of code points (must be non-negative)
   * @return a new builder
   * @throws IllegalArgumentException if length is negative
   */
  public static Builder builder(int length) {
    return new Builder(length);
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
      decoder.reset();
      return CodePointReader.create(input, decoder).read(length);
    }
  }

  /** A builder for creating {@link FixedCodePointStringCodec} instances. */
  public static class Builder {
    private final int length;
    private CharsetDecoder decoder = Charset.defaultCharset().newDecoder();

    private Builder(int length) {
      Preconditions.check(length >= 0, "length must be non-negative, but was [%d]", length);
      this.length = length;
    }

    /**
     * Sets the charset to use for encoding and decoding.
     *
     * @param charset the charset
     * @return this builder
     * @throws NullPointerException if charset is null
     */
    public Builder charset(Charset charset) {
      this.decoder = Objects.requireNonNull(charset, "charset").newDecoder();
      return this;
    }

    /**
     * Sets a custom decoder for decoding. The charset is derived from the decoder.
     *
     * @param decoder the decoder
     * @return this builder
     * @throws NullPointerException if decoder is null
     */
    public Builder decoder(CharsetDecoder decoder) {
      this.decoder = Objects.requireNonNull(decoder, "decoder");
      return this;
    }

    /**
     * Builds a new {@link FixedCodePointStringCodec} with the configured settings.
     *
     * @return a new codec instance
     */
    public FixedCodePointStringCodec build() {
      return new FixedCodePointStringCodec(length, decoder);
    }
  }
}
