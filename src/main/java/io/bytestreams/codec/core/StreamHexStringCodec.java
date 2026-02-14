package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import io.bytestreams.codec.core.util.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HexFormat;

/**
 * A codec for variable-length hexadecimal strings that reads all remaining bytes from the stream.
 *
 * <p>Unlike {@link FixedHexStringCodec}, which reads a fixed number of hex digits, this codec reads
 * all bytes until EOF. This makes it suitable for use as a value codec inside {@link
 * VariableByteLengthCodec}, where the stream is bounded by the length prefix.
 *
 * <p>Encode pads odd-length values to even according to the configured padding direction and
 * character. Decode always returns an even-length hex string.
 *
 * <pre>{@code
 * // Default: left-pad with '0'
 * Codec<String> codec = StringCodecs.ofHex().build();
 *
 * // Right-pad with 'f'
 * Codec<String> codec = StringCodecs.ofHex().padRight('f').build();
 * }</pre>
 */
public class StreamHexStringCodec implements Codec<String> {
  private static final HexFormat HEX_FORMAT = HexFormat.of();
  private final char padChar;
  private final boolean padLeft;

  /**
   * Creates a new stream hex string codec with the specified padding configuration.
   *
   * @param padChar the character to use for padding
   * @param padLeft true to pad on the left, false to pad on the right
   */
  StreamHexStringCodec(char padChar, boolean padLeft) {
    this.padChar = padChar;
    this.padLeft = padLeft;
  }

  /**
   * Returns a new builder for creating a {@link StreamHexStringCodec} with configurable padding.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    int paddedLength = value.length() + (value.length() % 2);
    String padded =
        padLeft
            ? Strings.padStart(value, paddedLength, padChar)
            : Strings.padEnd(value, paddedLength, padChar);
    byte[] bytes = HEX_FORMAT.parseHex(padded);
    output.write(bytes);
    return new EncodeResult(value.length(), bytes.length);
  }

  @Override
  public String decode(InputStream input) throws IOException {
    return HEX_FORMAT.formatHex(input.readAllBytes());
  }

  /**
   * A builder for creating {@link StreamHexStringCodec} instances with configurable padding.
   */
  public static class Builder extends PaddingBuilder<Builder> {

    /**
     * Creates a new builder.
     */
    Builder() {
      this.padChar = '0';
      this.padLeft = true;
    }

    @Override
    void validatePadChar(char c) {
      Preconditions.check(
          Character.digit(c, 16) >= 0,
          "padChar must be a valid hex character (0-9, a-f, A-F), but was [%s]",
          c);
    }

    /**
     * Builds a new {@link StreamHexStringCodec} with the configured settings.
     *
     * @return a new codec instance
     */
    public StreamHexStringCodec build() {
      return new StreamHexStringCodec(padChar, padLeft);
    }
  }
}
