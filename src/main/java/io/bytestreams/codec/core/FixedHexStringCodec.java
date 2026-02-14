package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import io.bytestreams.codec.core.util.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HexFormat;

/**
 * A codec for fixed-length hexadecimal strings.
 *
 * <p>Supports configurable padding direction and character via the builder:
 *
 * <pre>{@code
 * // Default: left-pad with '0'
 * Codec<String> codec = StringCodecs.ofHex(4).build();
 *
 * // Right-pad with 'f'
 * Codec<String> codec = StringCodecs.ofHex(4).padRight('f').build();
 * }</pre>
 */
public class FixedHexStringCodec implements FixedLengthCodec<String> {
  private static final String ERROR_MESSAGE =
      "value length must be less than or equal to %d, but was [%d]";
  private static final HexFormat HEX_FORMAT = HexFormat.of();
  private final int length;
  private final char padChar;
  private final boolean padLeft;

  /**
   * Creates a new hex string codec with the specified length and padding configuration.
   *
   * @param length the number of hex digits
   * @param padChar the character to use for padding
   * @param padLeft true to pad on the left, false to pad on the right
   */
  FixedHexStringCodec(int length, char padChar, boolean padLeft) {
    this.length = length;
    this.padChar = padChar;
    this.padLeft = padLeft;
  }

  /**
   * Returns a new builder for creating a {@link FixedHexStringCodec} with the specified length.
   *
   * @param length the number of hex digits (must be non-negative)
   * @return a new builder
   * @throws IllegalArgumentException if length is negative
   */
  public static Builder builder(int length) {
    return new Builder(length);
  }

  /**
   * {@inheritDoc}
   *
   * @return the number of digits
   */
  @Override
  public int getLength() {
    return length;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Odd-length values are padded according to the configured padding direction and character.
   *
   * @throws IllegalArgumentException if the value length exceeds the configured length
   */
  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    Preconditions.check(value.length() <= length, ERROR_MESSAGE, length, value.length());
    int paddedLength = Strings.hexByteCount(length) * 2;
    String padded =
        padLeft
            ? Strings.padStart(value, paddedLength, padChar)
            : Strings.padEnd(value, paddedLength, padChar);
    byte[] bytes = HEX_FORMAT.parseHex(padded);
    output.write(bytes);
    return new EncodeResult(length, bytes.length);
  }

  /**
   * {@inheritDoc}
   *
   * @throws java.io.EOFException if the stream has insufficient bytes
   */
  @Override
  public String decode(InputStream input) throws IOException {
    String parsed =
        HEX_FORMAT.formatHex(InputStreams.readFully(input, Strings.hexByteCount(length)));
    if (padLeft) {
      return parsed.substring(parsed.length() - length);
    } else {
      return parsed.substring(0, length);
    }
  }

  /**
   * A builder for creating {@link FixedHexStringCodec} instances with configurable padding.
   */
  public static class Builder extends PaddingBuilder<Builder> {
    private final int length;

    /**
     * Creates a new builder with the specified length.
     *
     * @param length the number of hex digits (must be non-negative)
     * @throws IllegalArgumentException if length is negative
     */
    Builder(int length) {
      Preconditions.check(length >= 0, "length must be non-negative, but was [%d]", length);
      this.length = length;
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
     * Builds a new {@link FixedHexStringCodec} with the configured settings.
     *
     * @return a new codec instance
     */
    public FixedHexStringCodec build() {
      return new FixedHexStringCodec(length, padChar, padLeft);
    }
  }
}
