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
 * StreamCodePointStringCodec codec = StreamCodePointStringCodec.builder().build();
 * }</pre>
 */
public class StreamCodePointStringCodec implements Codec<String> {
  private final Charset charset;

  StreamCodePointStringCodec(Charset charset) {
    this.charset = Objects.requireNonNull(charset, "charset");
  }

  /**
   * Returns a new builder for creating a {@link StreamCodePointStringCodec}.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    byte[] encoded = value.getBytes(charset);
    output.write(encoded);
    return new EncodeResult(value.codePointCount(0, value.length()), encoded.length);
  }

  @Override
  public String decode(InputStream input) throws IOException {
    return new String(input.readAllBytes(), charset);
  }

  /** A builder for creating {@link StreamCodePointStringCodec} instances. */
  public static class Builder {
    private Charset charset = Charset.defaultCharset();

    private Builder() {}

    /**
     * Sets the charset to use for encoding and decoding.
     *
     * @param charset the charset
     * @return this builder
     * @throws NullPointerException if charset is null
     */
    public Builder charset(Charset charset) {
      this.charset = Objects.requireNonNull(charset, "charset");
      return this;
    }

    /**
     * Builds a new {@link StreamCodePointStringCodec} with the configured settings.
     *
     * @return a new codec instance
     */
    public StreamCodePointStringCodec build() {
      return new StreamCodePointStringCodec(charset);
    }
  }
}
