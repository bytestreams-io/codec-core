package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import io.bytestreams.codec.core.util.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * A codec for fixed-length BCD (Binary Coded Decimal) digit strings.
 *
 * <p>Each byte holds two decimal digits (0–9) in its high and low nibbles. Odd-length digit counts
 * are left-padded with a zero nibble. Non-decimal nibble values are rejected on encode and decode.
 *
 * <p>This codec is package-private. Use {@link Codecs#bcdInt(int)} or {@link Codecs#bcdLong(int)}
 * for the public numeric API.
 */
class BcdCodec implements Codec<String> {
  private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");
  private static final String INVALID_BCD_VALUE = "invalid BCD value: %s";

  private final int digits;
  private final int byteCount;
  private final int paddedLength;

  BcdCodec(int digits) {
    Preconditions.check(digits > 0, "digits must be positive, but was [%d]", digits);
    this.digits = digits;
    this.byteCount = Strings.hexByteCount(digits);
    this.paddedLength = byteCount * 2;
  }

  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    Preconditions.check(
        value.length() <= digits,
        "value length must be less than or equal to %d, but was [%d]",
        digits,
        value.length());
    Preconditions.check(DIGIT_PATTERN.matcher(value).matches(), INVALID_BCD_VALUE, value);
    output.write(HEX_FORMAT.parseHex(Strings.padStart(value, '0', paddedLength)));
    return new EncodeResult(digits, byteCount);
  }

  @Override
  public String decode(InputStream input) throws IOException {
    String hex = HEX_FORMAT.formatHex(InputStreams.readFully(input, byteCount));
    if (!DIGIT_PATTERN.matcher(hex).matches()) {
      throw new CodecException(String.format(INVALID_BCD_VALUE, hex), null);
    }
    return hex.substring(hex.length() - digits);
  }
}
