package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import io.bytestreams.codec.core.util.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * A codec that formats strings with padding before delegating to an underlying codec.
 *
 * <p>On encode, the value is padded to the delegate's length. On decode, padding is stripped if
 * {@code trim} is enabled; otherwise the value is returned as-is from the delegate.
 *
 * <pre>{@code
 * // Default: left-pad with space, no trim
 * Codec<String> codec = StringCodecs.ofFormatted(delegate).build();
 *
 * // Right-pad with '0', trim on decode
 * Codec<String> codec = StringCodecs.ofFormatted(delegate).padRight('0').trim().build();
 * }</pre>
 */
public class FormattedStringCodec implements Codec<String> {
  private final FixedLengthCodec<String> delegate;
  private final char paddingChar;
  private final boolean padLeft;
  private final boolean trim;

  /**
   * Creates a new formatted string codec.
   *
   * @param delegate the underlying codec to delegate to
   * @param paddingChar the character to use for padding
   * @param padLeft true to pad on the left, false to pad on the right
   * @param trim true to strip the padding character on decode
   */
  FormattedStringCodec(
      FixedLengthCodec<String> delegate, char paddingChar, boolean padLeft, boolean trim) {
    this.delegate = delegate;
    this.paddingChar = paddingChar;
    this.padLeft = padLeft;
    this.trim = trim;
  }

  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    String padded =
        padLeft
            ? Strings.padStart(value, delegate.getLength(), paddingChar)
            : Strings.padEnd(value, delegate.getLength(), paddingChar);
    return delegate.encode(padded, output);
  }

  @Override
  public String decode(InputStream input) throws IOException {
    String value = delegate.decode(input);
    if (trim) {
      return padLeft ? Strings.trimStart(value, paddingChar) : Strings.trimEnd(value, paddingChar);
    }
    return value;
  }

  /**
   * Returns a new builder for creating a {@link FormattedStringCodec} with the specified delegate.
   *
   * @param delegate the underlying codec to delegate to
   * @return a new builder
   * @throws NullPointerException if delegate is null
   */
  public static Builder builder(FixedLengthCodec<String> delegate) {
    return new Builder(delegate);
  }

  /**
   * A builder for creating {@link FormattedStringCodec} instances with configurable padding.
   * Restricts pad characters to printable ASCII (0x20-0x7E).
   */
  public static class Builder extends PaddingBuilder<Builder> {
    private static final char PRINTABLE_ASCII_MIN = ' ';
    private static final char PRINTABLE_ASCII_MAX = '~';
    private final FixedLengthCodec<String> delegate;
    private boolean trim;

    /**
     * Creates a new builder with the specified delegate codec.
     *
     * @param delegate the underlying codec to delegate to
     * @throws NullPointerException if delegate is null
     */
    Builder(FixedLengthCodec<String> delegate) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      this.padChar = ' ';
      this.padLeft = true;
    }

    /**
     * Enables stripping the padding character on decode. Left-padded codecs strip from the start;
     * right-padded codecs strip from the end.
     *
     * @return this builder
     */
    public Builder trim() {
      this.trim = true;
      return this;
    }

    @Override
    void validatePadChar(char c) {
      Preconditions.check(
          c >= PRINTABLE_ASCII_MIN && c <= PRINTABLE_ASCII_MAX,
          "padChar must be a printable ASCII character (0x20-0x7E), but was [%s]",
          c);
    }

    /**
     * Builds a new {@link FormattedStringCodec} with the configured settings.
     *
     * @return a new codec instance
     */
    public FormattedStringCodec build() {
      return new FormattedStringCodec(delegate, padChar, padLeft, trim);
    }
  }
}
