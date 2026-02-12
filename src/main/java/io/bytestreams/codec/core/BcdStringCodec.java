package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * A codec for fixed-length BCD (Binary-Coded Decimal) strings.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Binary-coded_decimal">Binary-Coded Decimal</a>
 */
public class BcdStringCodec extends HexStringCodec {
  private static final Pattern BCD_PATTERN = Pattern.compile("^\\d+$");
  private static final String ERROR_MESSAGE = "invalid BCD string [%s]";

  /**
   * Creates a new BCD string codec with the specified length.
   *
   * @param length the number of BCD digits (not bytes)
   */
  public BcdStringCodec(int length) {
    super(length);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the value contains non-digit characters
   */
  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    Preconditions.check(BCD_PATTERN.matcher(value).matches(), ERROR_MESSAGE, value);
    return super.encode(value, output);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the decoded value contains non-digit characters
   */
  @Override
  public String decode(InputStream input) throws IOException {
    String parsed = super.decode(input);
    Preconditions.check(BCD_PATTERN.matcher(parsed).matches(), ERROR_MESSAGE, parsed);
    return parsed;
  }
}
