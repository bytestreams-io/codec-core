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
 * <p>Odd-length values are left-padded with '0' to align to byte boundaries. Encode accepts both
 * uppercase and lowercase hex digits. Decode always returns uppercase.
 *
 * <pre>{@code
 * Codec<String> codec = Codecs.hex(4);
 * }</pre>
 */
public class FixedHexStringCodec implements Codec<String> {
  private static final String ERROR_MESSAGE =
      "value length must be less than or equal to %d, but was [%d]";
  private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
  private final int length;

  /**
   * Creates a new hex string codec with the specified length.
   *
   * @param length the number of hex digits (must be non-negative)
   * @throws IllegalArgumentException if length is negative
   */
  FixedHexStringCodec(int length) {
    Preconditions.check(length >= 0, "length must be non-negative, but was [%d]", length);
    this.length = length;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Odd-length values are left-padded with '0' to align to byte boundaries.
   *
   * @throws IllegalArgumentException if the value length exceeds the configured length
   */
  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    Preconditions.check(value.length() <= length, ERROR_MESSAGE, length, value.length());
    int paddedLength = Strings.hexByteCount(length) * 2;
    String padded = Strings.padStart(value, '0', paddedLength);
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
    return parsed.substring(parsed.length() - length);
  }
}
