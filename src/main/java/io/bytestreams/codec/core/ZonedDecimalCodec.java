package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HexFormat;

/**
 * A codec for EBCDIC zoned decimal, the format COBOL declares as {@code DISPLAY}.
 *
 * <p>One digit per byte, written as EBCDIC characters, with the sign held in the high nibble (the
 * "zone") of the final byte rather than in a byte of its own:
 *
 * <pre>
 * PIC S9(3) DISPLAY, value -123   -&gt;   F1 F2 D3
 * </pre>
 *
 * <p>Encoding writes zone {@code C} for positive and {@code D} for negative on the last byte, and
 * {@code F} on the rest. Decoding accepts the full set IBM defines — {@code A}, {@code C},
 * {@code E} and {@code F} positive, {@code B} and {@code D} negative — since {@code F} is what a
 * producer writes for a field it treats as unsigned.
 *
 * <p>Leading bytes must carry the digit zone {@code F}. That rejects ASCII digits read as EBCDIC,
 * which would otherwise decode to the right number by accident, since both encodings hold the digit
 * in the low nibble.
 *
 * <p>Reading such a field as text instead is the trap this codec exists to close: {@code F1 F2 D3}
 * decodes as the EBCDIC string {@code "12L"}, because {@code 0xD3} is the letter {@code L}. Nothing
 * reports an error, and the sign is silently lost.
 *
 * <p>This codec is package-private. Use {@link Codecs#zonedInt(int)} or
 * {@link Codecs#zonedLong(int)} for the public numeric API.
 */
class ZonedDecimalCodec implements Codec<Long> {
  private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
  private static final char DIGIT_ZONE = 'F';

  private final int digits;
  private final long limit;

  ZonedDecimalCodec(int digits) {
    Preconditions.check(digits > 0, "digits must be positive, but was [%d]", digits);
    // The decoded type is long, and 10^19 would overflow the range check in encode.
    Preconditions.check(digits <= 18, "digits must be at most 18, but was [%d]", digits);
    this.digits = digits;
    this.limit = SignedDecimals.limitFor(digits);
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(Long value, OutputStream output) throws IOException {
    SignedDecimals.checkRange(value, limit, digits);
    String magnitude = SignedDecimals.padded(value, digits);
    StringBuilder hex = new StringBuilder(digits * 2);
    for (int i = 0; i < digits; i++) {
      char zone = i == digits - 1 ? SignedDecimals.signFor(value) : DIGIT_ZONE;
      hex.append(zone).append(magnitude.charAt(i));
    }
    output.write(HEX_FORMAT.parseHex(hex.toString()));
    return new EncodeResult(digits, digits);
  }

  /** {@inheritDoc} */
  @Override
  public Long decode(InputStream input) throws IOException {
    String hex = HEX_FORMAT.formatHex(InputStreams.readFully(input, digits));
    StringBuilder magnitude = new StringBuilder(digits);
    for (int i = 0; i < digits; i++) {
      char zone = hex.charAt(i * 2);
      char digit = hex.charAt(i * 2 + 1);
      // formatHex yields only 0-9 and A-F, so a digit above '9' is a letter and nothing else.
      if (digit > '9') {
        throw new CodecException("invalid zoned decimal digit: %s".formatted(hex), null);
      }
      if (i < digits - 1 && zone != DIGIT_ZONE) {
        throw new CodecException("invalid zoned decimal zone: %s".formatted(hex), null);
      }
      magnitude.append(digit);
    }
    char sign = hex.charAt((digits - 1) * 2);
    // In range: every character was checked to be 0-9, there are exactly `digits` of them, and the
    // constructor caps `digits` at 18, so the value is below Long.MAX_VALUE.
    long parsed = Long.parseLong(magnitude.toString());
    if (SignedDecimals.isPositive(sign)) {
      return parsed;
    }
    if (SignedDecimals.isNegative(sign)) {
      return -parsed;
    }
    throw new CodecException("invalid zoned decimal sign zone: %s".formatted(hex), null);
  }
}
