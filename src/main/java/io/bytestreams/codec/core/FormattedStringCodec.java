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
 * <p>On encode, the value is padded to the delegate's length. On decode, the value is returned
 * as-is from the delegate (padding is not stripped).
 *
 * <pre>{@code
 * // Default: left-pad with space
 * FormattedStringCodec codec = FormattedStringCodec.builder(delegate).build();
 *
 * // Right-pad with '0'
 * FormattedStringCodec codec = FormattedStringCodec.builder(delegate).padRight('0').build();
 * }</pre>
 */
public class FormattedStringCodec implements Codec<String> {
  private final FixedLengthCodec<String> delegate;
  private final char paddingChar;
  private final boolean padLeft;

  /**
   * Creates a new formatted string codec.
   *
   * @param delegate the underlying codec to delegate to
   * @param paddingChar the character to use for padding
   * @param padLeft true to pad on the left, false to pad on the right
   */
  FormattedStringCodec(FixedLengthCodec<String> delegate, char paddingChar, boolean padLeft) {
    this.delegate = delegate;
    this.paddingChar = paddingChar;
    this.padLeft = padLeft;
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
    return delegate.decode(input);
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

    /**
     * Creates a new builder with the specified delegate codec.
     *
     * @param delegate the underlying codec to delegate to
     * @throws NullPointerException if delegate is null
     */
    private Builder(FixedLengthCodec<String> delegate) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      this.padChar = ' ';
      this.padLeft = true;
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
      return new FormattedStringCodec(delegate, padChar, padLeft);
    }
  }
}
