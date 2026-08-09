package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import io.bytestreams.codec.core.util.Strings;

/**
 * Shared rules for the decimal formats that carry a sign in a nibble — packed decimal
 * ({@code COMP-3}) and zoned decimal ({@code DISPLAY}).
 *
 * <p>Both place the sign in a nibble rather than a character, and both use the same set of values:
 * {@code A}, {@code C}, {@code E} and {@code F} positive, {@code B} and {@code D} negative. They
 * differ only in where that nibble sits.
 *
 * <p>Sign nibbles are compared as uppercase characters, matching the {@link java.util.HexFormat}
 * each codec formats with.
 */
final class SignedDecimals {
  private static final String POSITIVE_SIGNS = "ACEF";
  private static final String NEGATIVE_SIGNS = "BD";
  private static final char POSITIVE = 'C';
  private static final char NEGATIVE = 'D';

  private SignedDecimals() {}

  /** Returns 10 to the power of {@code digits}; safe for the supported range of 1 to 18. */
  static long limitFor(int digits) {
    long result = 1;
    for (int i = 0; i < digits; i++) {
      result *= 10;
    }
    return result;
  }

  /** Rejects a value that would not fit in the declared number of digits. */
  static void checkRange(long value, long limit, int digits) {
    Preconditions.check(
        value > -limit && value < limit,
        "value must have at most %d digits, but was [%d]",
        digits,
        value);
  }

  /**
   * Returns the magnitude of {@code value}, left-padded with zeros to {@code length}.
   *
   * <p>Call {@link #checkRange(long, long, int)} first. {@code Math.abs(Long.MIN_VALUE)} is
   * negative, so an unchecked value would produce a magnitude carrying a minus sign.
   */
  static String padded(long value, int length) {
    return Strings.padStart(Long.toString(Math.abs(value)), '0', length);
  }

  /** Returns the sign nibble to write for {@code value}. */
  static char signFor(long value) {
    return value < 0 ? NEGATIVE : POSITIVE;
  }

  static boolean isPositive(char nibble) {
    return POSITIVE_SIGNS.indexOf(nibble) >= 0;
  }

  static boolean isNegative(char nibble) {
    return NEGATIVE_SIGNS.indexOf(nibble) >= 0;
  }
}
