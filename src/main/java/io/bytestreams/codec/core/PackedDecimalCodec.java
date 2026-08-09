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
 * A codec for signed packed decimal, the format COBOL declares as {@code COMP-3}.
 *
 * <p>Digits occupy a nibble each, two to a byte, and the final nibble carries the sign rather than
 * a digit. A five-digit field therefore takes three bytes, the last of which holds one digit and
 * the sign:
 *
 * <pre>
 * PIC S9(5) COMP-3, value -12345   -&gt;   12 34 5D
 * </pre>
 *
 * <p>Encoding writes {@code C} for positive and {@code D} for negative. Decoding accepts the full
 * set IBM defines — {@code A}, {@code C}, {@code E} and {@code F} positive, {@code B} and {@code D}
 * negative — since {@code F} in particular is common for fields a producer treats as unsigned.
 *
 * <p>Where the digit count is even, the leading nibble is an unused zero.
 *
 * <p>This differs from {@link BcdCodec} only by the sign nibble: BCD packs digits the same way but
 * has no sign, so it cannot represent a negative value.
 *
 * <p>This codec is package-private. Use {@link Codecs#packedInt(int)} or
 * {@link Codecs#packedLong(int)} for the public numeric API.
 */
class PackedDecimalCodec implements Codec<Long> {
  private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d*");
  private static final String POSITIVE_SIGNS = "ACEF";
  private static final String NEGATIVE_SIGNS = "BD";
  private static final char POSITIVE = 'C';
  private static final char NEGATIVE = 'D';

  private final int digits;
  private final int byteCount;
  private final long limit;

  PackedDecimalCodec(int digits) {
    Preconditions.check(digits > 0, "digits must be positive, but was [%d]", digits);
    // The decoded type is long, and 10^19 would overflow the range check below.
    Preconditions.check(digits <= 18, "digits must be at most 18, but was [%d]", digits);
    this.digits = digits;
    // digits plus one sign nibble, rounded up to whole bytes
    this.byteCount = digits / 2 + 1;
    this.limit = pow10(digits);
  }

  private static long pow10(int exponent) {
    long result = 1;
    for (int i = 0; i < exponent; i++) {
      result *= 10;
    }
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(Long value, OutputStream output) throws IOException {
    Preconditions.check(
        value > -limit && value < limit,
        "value must have at most %d digits, but was [%d]",
        digits,
        value);
    String magnitude = Long.toString(Math.abs(value));
    char sign = value < 0 ? NEGATIVE : POSITIVE;
    String hex = Strings.padStart(magnitude, '0', byteCount * 2 - 1) + sign;
    output.write(HEX_FORMAT.parseHex(hex));
    return new EncodeResult(digits, byteCount);
  }

  /** {@inheritDoc} */
  @Override
  public Long decode(InputStream input) throws IOException {
    String hex = HEX_FORMAT.formatHex(InputStreams.readFully(input, byteCount));
    String magnitude = hex.substring(0, hex.length() - 1);
    char sign = hex.charAt(hex.length() - 1);
    if (!DIGIT_PATTERN.matcher(magnitude).matches()) {
      throw new CodecException("invalid packed decimal digits: %s".formatted(hex), null);
    }
    // An even digit count leaves a leading nibble the field does not use. It must be zero, or the
    // value carries more digits than the field declares.
    String unused = magnitude.substring(0, magnitude.length() - digits);
    if (unused.chars().anyMatch(c -> c != '0')) {
      throw new CodecException(
          "packed decimal has more than %d digits: %s".formatted(digits, hex), null);
    }
    // Both parses are in range: the checks above leave at most `digits` significant digits, the
    // constructor caps `digits` at 18, and 10^18 - 1 is below Long.MAX_VALUE. Removing either check
    // would let a wider field reach here and fail as NumberFormatException rather than
    // CodecException.
    if (POSITIVE_SIGNS.indexOf(sign) >= 0) {
      return Long.parseLong(magnitude);
    }
    if (NEGATIVE_SIGNS.indexOf(sign) >= 0) {
      return -Long.parseLong(magnitude);
    }
    throw new CodecException("invalid packed decimal sign nibble: %s".formatted(hex), null);
  }
}
