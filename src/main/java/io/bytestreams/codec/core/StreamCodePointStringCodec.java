package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * A codec for variable-length character strings that reads all remaining bytes from the stream.
 *
 * <p>Unlike {@link FixedCodePointStringCodec}, which reads a fixed number of code points, this
 * codec reads all bytes until EOF. This makes it suitable for use as a value codec inside {@link
 * VariableByteLengthCodec}, where the stream is bounded by the length prefix.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Codec<String> codec = Codecs.ascii();
 * }</pre>
 */
public class StreamCodePointStringCodec implements Codec<String> {
  private final Charset charset;

  /**
   * Creates a new stream code point string codec.
   *
   * @param charset the charset for encoding and decoding
   * @throws NullPointerException if charset is null
   */
  StreamCodePointStringCodec(Charset charset) {
    this.charset = Objects.requireNonNull(charset, "charset");
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    byte[] encoded = value.getBytes(charset);
    output.write(encoded);
    return new EncodeResult(value.codePointCount(0, value.length()), encoded.length);
  }

  /** {@inheritDoc} */
  @Override
  public String decode(InputStream input) throws IOException {
    return new String(input.readAllBytes(), charset);
  }
}
