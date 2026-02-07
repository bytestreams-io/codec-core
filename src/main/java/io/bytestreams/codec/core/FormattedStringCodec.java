package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A codec that formats strings with padding before delegating to an underlying codec.
 *
 * <p>On encode, the value is padded to the delegate's length. On decode, the value is returned
 * as-is from the delegate (padding is not stripped).
 */
public class FormattedStringCodec implements Codec<String> {
  private final FixedLengthCodec<String> delegate;
  private final char paddingChar;
  private final boolean padLeft;

  /**
   * Creates a new formatted string codec with left padding.
   *
   * @param delegate the underlying codec to delegate to
   * @param paddingChar the character to use for padding
   */
  public FormattedStringCodec(FixedLengthCodec<String> delegate, char paddingChar) {
    this(delegate, paddingChar, true);
  }

  /**
   * Creates a new formatted string codec.
   *
   * @param delegate the underlying codec to delegate to
   * @param paddingChar the character to use for padding
   * @param padLeft true to pad on the left, false to pad on the right
   */
  public FormattedStringCodec(
      FixedLengthCodec<String> delegate, char paddingChar, boolean padLeft) {
    this.delegate = delegate;
    this.paddingChar = paddingChar;
    this.padLeft = padLeft;
  }

  @Override
  public void encode(String value, OutputStream output) throws IOException {
    String padded =
        padLeft
            ? Strings.padStart(value, delegate.getLength(), paddingChar)
            : Strings.padEnd(value, delegate.getLength(), paddingChar);
    delegate.encode(padded, output);
  }

  @Override
  public String decode(InputStream input) throws IOException {
    return delegate.decode(input);
  }
}
