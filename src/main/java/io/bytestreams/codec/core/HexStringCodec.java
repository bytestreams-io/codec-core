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
 */
public class HexStringCodec implements FixedLengthCodec<String> {
  private static final String ERROR_MESSAGE =
      "value length must be less than or equal to %d, but was [%d]";
  private static final HexFormat HEX_FORMAT = HexFormat.of();
  private final int length;

  /**
   * Creates a new hex string codec with the specified length.
   *
   * @param length the number of hex digits (not bytes)
   */
  public HexStringCodec(int length) {
    this.length = length;
  }

  /**
   * {@inheritDoc}
   *
   * @return the number of hex digits
   */
  @Override
  public int getLength() {
    return length;
  }

  private static int toByteSize(int digits) {
    return (digits + 1) / 2;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Odd-length values are left-padded with '0'.
   *
   * @throws IllegalArgumentException if the value length exceeds the configured length
   */
  @Override
  public void encode(String value, OutputStream output) throws IOException {
    Preconditions.check(value.length() <= length, ERROR_MESSAGE, length, value.length());
    String padded = Strings.padStart(value, toByteSize(length) * 2, '0');
    output.write(HEX_FORMAT.parseHex(padded));
  }

  /**
   * {@inheritDoc}
   *
   * @throws java.io.EOFException if the stream has insufficient bytes
   */
  @Override
  public String decode(InputStream input) throws IOException {
    String parsed = HEX_FORMAT.formatHex(InputStreams.readFully(input, toByteSize(length)));
    return parsed.substring(parsed.length() - length);
  }
}
