package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.bytestreams.codec.core.util.Converters;
import io.bytestreams.codec.core.util.Preconditions;
import io.bytestreams.codec.core.util.Strings;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Unified entry point for creating all codec types.
 *
 * <p>This facade provides factory methods for number, string, hex, binary, boolean, list,
 * composition, and object codecs.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Number codecs
 * Codec<Integer> u8 = Codecs.uint8();
 * Codec<Long> i64 = Codecs.int64();
 *
 * // String codecs
 * Codec<String> ascii = Codecs.ascii(10);
 * Codec<String> utf8 = Codecs.utf8();
 *
 * // Constant bytes
 * Codec<byte[]> magic = Codecs.constant(new byte[] {0x4D, 0x5A});
 *
 * // Numeric string codecs
 * Codec<Integer> bcd = Codecs.bcdInt(4);
 * Codec<Integer> asciiNum = Codecs.asciiInt(4);
 * Codec<Long> ebcdicNum = Codecs.ebcdicLong(10);
 *
 * // Composition
 * Codec<String> prefixed = Codecs.prefixed(Codecs.uint16(), Codecs.utf8());
 * Codec<List<Integer>> list = Codecs.listOf(5, Codecs.uint8());
 *
 * // Object codecs
 * SequentialObjectCodec<Msg> codec = Codecs.<Msg>sequential(Msg::new)
 *     .field("id", Codecs.int32(), Msg::getId, Msg::setId)
 *     .build();
 * }</pre>
 */
public class Codecs {
  private static final Charset EBCDIC = Charset.forName("IBM1047");
  private static final String INT_DIGITS_MSG = "digits must be between 1 and 9, but was [%d]";
  private static final String LONG_DIGITS_MSG = "digits must be between 1 and 18, but was [%d]";
  private static final String POSITIVE_DIGITS_MSG = "digits must be positive, but was [%d]";

  private Codecs() {}

  // ---------------------------------------------------------------------------
  // Number codecs
  // ---------------------------------------------------------------------------

  /**
   * Creates a codec for unsigned byte values (0 to 255), encoded as 1-byte binary.
   *
   * @return a new unsigned byte codec
   */
  public static Codec<Integer> uint8() {
    return BinaryNumberCodec.ofUnsignedByte();
  }

  /**
   * Creates a codec for unsigned short values (0 to 65535), encoded as 2-byte big-endian binary.
   *
   * @return a new unsigned short codec
   * @see #uint16(ByteOrder)
   */
  public static Codec<Integer> uint16() {
    return BinaryNumberCodec.ofUnsignedShort();
  }

  /**
   * Creates a codec for unsigned short values (0 to 65535), encoded as 2-byte binary in the given byte order.
   *
   * <p>The no-argument factory is big-endian. Little-endian is common in formats of PC and
   * embedded origin, such as ZIP, RIFF and PE.
   *
   * @param order the byte order
   * @return a new codec
   * @throws NullPointerException if order is null
   */
  public static Codec<Integer> uint16(ByteOrder order) {
    return BinaryNumberCodec.ofUnsignedShort().withOrder(order);
  }

  /**
   * Creates a codec for unsigned integer values (0 to 4294967295), encoded as 4-byte big-endian
   * binary.
   *
   * @return a new unsigned integer codec
   * @see #uint32(ByteOrder)
   */
  public static Codec<Long> uint32() {
    return BinaryNumberCodec.ofUnsignedInt();
  }

  /**
   * Creates a codec for unsigned integer values (0 to 4294967295), encoded as 4-byte binary in the given byte order.
   *
   * <p>The no-argument factory is big-endian. Little-endian is common in formats of PC and
   * embedded origin, such as ZIP, RIFF and PE.
   *
   * @param order the byte order
   * @return a new codec
   * @throws NullPointerException if order is null
   */
  public static Codec<Long> uint32(ByteOrder order) {
    return BinaryNumberCodec.ofUnsignedInt().withOrder(order);
  }

  /**
   * Creates a codec for signed short values (-32768 to 32767), encoded as 2-byte big-endian binary.
   *
   * @return a new signed short codec
   * @see #int16(ByteOrder)
   */
  public static Codec<Short> int16() {
    return BinaryNumberCodec.ofShort();
  }

  /**
   * Creates a codec for signed short values (-32768 to 32767), encoded as 2-byte binary in the given byte order.
   *
   * <p>The no-argument factory is big-endian. Little-endian is common in formats of PC and
   * embedded origin, such as ZIP, RIFF and PE.
   *
   * @param order the byte order
   * @return a new codec
   * @throws NullPointerException if order is null
   */
  public static Codec<Short> int16(ByteOrder order) {
    return BinaryNumberCodec.ofShort().withOrder(order);
  }

  /**
   * Creates a codec for signed integer values (-2147483648 to 2147483647), encoded as 4-byte
   * big-endian binary.
   *
   * @return a new signed integer codec
   * @see #int32(ByteOrder)
   */
  public static Codec<Integer> int32() {
    return BinaryNumberCodec.ofInt();
  }

  /**
   * Creates a codec for signed integer values (-2147483648 to 2147483647), encoded as 4-byte binary in the given byte order.
   *
   * <p>The no-argument factory is big-endian. Little-endian is common in formats of PC and
   * embedded origin, such as ZIP, RIFF and PE.
   *
   * @param order the byte order
   * @return a new codec
   * @throws NullPointerException if order is null
   */
  public static Codec<Integer> int32(ByteOrder order) {
    return BinaryNumberCodec.ofInt().withOrder(order);
  }

  /**
   * Creates a codec for signed long values, encoded as 8-byte big-endian binary.
   *
   * @return a new signed long codec
   * @see #int64(ByteOrder)
   */
  public static Codec<Long> int64() {
    return BinaryNumberCodec.ofLong();
  }

  /**
   * Creates a codec for signed long values, encoded as 8-byte binary in the given byte order.
   *
   * <p>The no-argument factory is big-endian. Little-endian is common in formats of PC and
   * embedded origin, such as ZIP, RIFF and PE.
   *
   * @param order the byte order
   * @return a new codec
   * @throws NullPointerException if order is null
   */
  public static Codec<Long> int64(ByteOrder order) {
    return BinaryNumberCodec.ofLong().withOrder(order);
  }

  /**
   * Creates a codec for float values (IEEE 754 single-precision, 4 bytes).
   *
   * @return a new float codec
   * @see #float32(ByteOrder)
   */
  public static Codec<Float> float32() {
    return BinaryNumberCodec.ofFloat();
  }

  /**
   * Creates a codec for IEEE 754 single-precision values, encoded as 4-byte binary in the given byte order.
   *
   * <p>The no-argument factory is big-endian. Little-endian is common in formats of PC and
   * embedded origin, such as ZIP, RIFF and PE.
   *
   * @param order the byte order
   * @return a new codec
   * @throws NullPointerException if order is null
   */
  public static Codec<Float> float32(ByteOrder order) {
    return BinaryNumberCodec.ofFloat().withOrder(order);
  }

  /**
   * Creates a codec for double values (IEEE 754 double-precision, 8 bytes).
   *
   * @return a new double codec
   * @see #float64(ByteOrder)
   */
  public static Codec<Double> float64() {
    return BinaryNumberCodec.ofDouble();
  }

  /**
   * Creates a codec for IEEE 754 double-precision values, encoded as 8-byte binary in the given byte order.
   *
   * <p>The no-argument factory is big-endian. Little-endian is common in formats of PC and
   * embedded origin, such as ZIP, RIFF and PE.
   *
   * @param order the byte order
   * @return a new codec
   * @throws NullPointerException if order is null
   */
  public static Codec<Double> float64(ByteOrder order) {
    return BinaryNumberCodec.ofDouble().withOrder(order);
  }

  // ---------------------------------------------------------------------------
  // String codecs
  // ---------------------------------------------------------------------------

  /**
   * Creates a fixed-length US-ASCII string codec.
   *
   * @param length the number of code points
   * @return a new codec
   */
  public static Codec<String> ascii(int length) {
    return new FixedCodePointStringCodec(length, US_ASCII);
  }

  /**
   * Creates a variable-length US-ASCII string codec.
   *
   * @return a new codec
   */
  public static Codec<String> ascii() {
    return new StreamCodePointStringCodec(US_ASCII);
  }

  /**
   * Creates a variable-length US-ASCII string codec where the code point count is encoded as a
   * prefix.
   *
   * @param lengthCodec the codec for the code point count prefix
   * @return a new codec
   */
  public static Codec<String> ascii(Codec<Integer> lengthCodec) {
    return ofCharset(US_ASCII, lengthCodec);
  }

  /**
   * Creates a fixed-length UTF-8 string codec.
   *
   * @param length the number of code points
   * @return a new codec
   */
  public static Codec<String> utf8(int length) {
    return new FixedCodePointStringCodec(length, UTF_8);
  }

  /**
   * Creates a variable-length UTF-8 string codec.
   *
   * @return a new codec
   */
  public static Codec<String> utf8() {
    return new StreamCodePointStringCodec(UTF_8);
  }

  /**
   * Creates a variable-length UTF-8 string codec where the code point count is encoded as a prefix.
   *
   * @param lengthCodec the codec for the code point count prefix
   * @return a new codec
   */
  public static Codec<String> utf8(Codec<Integer> lengthCodec) {
    return ofCharset(UTF_8, lengthCodec);
  }

  /**
   * Creates a fixed-length ISO-8859-1 (Latin-1) string codec.
   *
   * @param length the number of code points
   * @return a new codec
   */
  public static Codec<String> latin1(int length) {
    return new FixedCodePointStringCodec(length, ISO_8859_1);
  }

  /**
   * Creates a variable-length ISO-8859-1 (Latin-1) string codec.
   *
   * @return a new codec
   */
  public static Codec<String> latin1() {
    return new StreamCodePointStringCodec(ISO_8859_1);
  }

  /**
   * Creates a variable-length ISO-8859-1 (Latin-1) string codec where the code point count is
   * encoded as a prefix.
   *
   * @param lengthCodec the codec for the code point count prefix
   * @return a new codec
   */
  public static Codec<String> latin1(Codec<Integer> lengthCodec) {
    return ofCharset(ISO_8859_1, lengthCodec);
  }

  /**
   * Creates a fixed-length EBCDIC (IBM1047) string codec.
   *
   * @param length the number of code points
   * @return a new codec
   */
  public static Codec<String> ebcdic(int length) {
    return new FixedCodePointStringCodec(length, EBCDIC);
  }

  /**
   * Creates a variable-length EBCDIC (IBM1047) string codec.
   *
   * @return a new codec
   */
  public static Codec<String> ebcdic() {
    return new StreamCodePointStringCodec(EBCDIC);
  }

  /**
   * Creates a variable-length EBCDIC (IBM1047) string codec where the code point count is encoded
   * as a prefix.
   *
   * @param lengthCodec the codec for the code point count prefix
   * @return a new codec
   */
  public static Codec<String> ebcdic(Codec<Integer> lengthCodec) {
    return ofCharset(EBCDIC, lengthCodec);
  }

  /**
   * Creates a fixed-length string codec with the specified charset.
   *
   * @param charset the charset
   * @param length the number of code points
   * @return a new codec
   */
  public static Codec<String> ofCharset(Charset charset, int length) {
    return new FixedCodePointStringCodec(length, charset);
  }

  /**
   * Creates a variable-length string codec with the specified charset.
   *
   * @param charset the charset
   * @return a new codec
   */
  public static Codec<String> ofCharset(Charset charset) {
    return new StreamCodePointStringCodec(charset);
  }

  /**
   * Creates a variable-length string codec where the code point count is encoded as a prefix.
   *
   * <p>For single-byte charsets, uses {@link String#length()} for the count (O(1)). For multibyte
   * charsets, uses {@link io.bytestreams.codec.core.util.Strings#codePointCount} (O(n)).
   *
   * @param charset the charset
   * @param lengthCodec the codec for the code point count prefix
   * @return a new codec
   */
  public static Codec<String> ofCharset(Charset charset, Codec<Integer> lengthCodec) {
    ToIntFunction<String> lengthOf =
        Strings.isSingleByte(charset) ? String::length : Strings::codePointCount;
    return prefixed(lengthCodec, lengthOf, length -> ofCharset(charset, length));
  }

  // ---------------------------------------------------------------------------
  // Hex codecs
  // ---------------------------------------------------------------------------

  /**
   * Creates a fixed-length hex string codec. Odd-length values are left-padded with '0' to align to
   * byte boundaries.
   *
   * @param length the number of hex digits
   * @return a new codec
   */
  public static Codec<String> hex(int length) {
    return new FixedHexStringCodec(length);
  }

  /**
   * Creates a variable-length hex string codec. Odd-length values are left-padded with '0' to align
   * to byte boundaries.
   *
   * @return a new codec
   */
  public static Codec<String> hex() {
    return new StreamHexStringCodec();
  }

  /**
   * Creates a variable-length hex string codec where the hex digit count is encoded as a prefix.
   * Odd-length values are left-padded with '0' to align to byte boundaries.
   *
   * @param lengthCodec the codec for the hex digit count prefix
   * @return a new codec
   */
  public static Codec<String> hex(Codec<Integer> lengthCodec) {
    return prefixed(lengthCodec, String::length, Codecs::hex);
  }

  // ---------------------------------------------------------------------------
  // BCD codecs
  // ---------------------------------------------------------------------------

  /**
   * Creates a fixed-length BCD (Binary Coded Decimal) codec that decodes to {@link Integer}.
   *
   * <p>Each byte holds two decimal digits (0–9) in its high and low nibbles. Odd-length digit
   * counts are left-padded with a zero nibble.
   *
   * <pre>{@code
   * Codec<Integer> codec = Codecs.bcdInt(4);
   * codec.encode(42, out);   // writes 0x00, 0x42
   * codec.decode(in);        // reads 0x00, 0x42 → 42
   * }</pre>
   *
   * @param digits the number of BCD digits (1 to 9)
   * @return a new BCD integer codec
   * @throws IllegalArgumentException if digits is not between 1 and 9
   */
  public static Codec<Integer> bcdInt(int digits) {
    Preconditions.check(digits >= 1 && digits <= 9, INT_DIGITS_MSG, digits);
    return new BcdCodec(digits).xmap(Converters.toInt(digits));
  }

  /**
   * Creates a BCD codec for values wider than a {@code long} holds.
   *
   * <p>{@link #bcdLong(int)} stops at eighteen digits, which is what a {@code long} carries. This
   * has no such limit.
   *
   * @param digits the number of digits
   * @return a new BCD big integer codec
   * @throws IllegalArgumentException if digits is not positive
   */
  public static Codec<BigInteger> bcdBigInt(int digits) {
    Preconditions.check(digits > 0, POSITIVE_DIGITS_MSG, digits);
    return new BcdCodec(digits).xmap(Converters.toBigInt(digits));
  }

  /**
   * Creates a fixed-length BCD (Binary Coded Decimal) codec that decodes to {@link Long}.
   *
   * <p>Each byte holds two decimal digits (0–9) in its high and low nibbles. Odd-length digit
   * counts are left-padded with a zero nibble.
   *
   * <pre>{@code
   * Codec<Long> codec = Codecs.bcdLong(10);
   * codec.encode(1234567890L, out);  // writes 0x12, 0x34, 0x56, 0x78, 0x90
   * codec.decode(in);                // reads 0x12, 0x34, 0x56, 0x78, 0x90 → 1234567890
   * }</pre>
   *
   * @param digits the number of BCD digits (1 to 18)
   * @return a new BCD long codec
   * @throws IllegalArgumentException if digits is not between 1 and 18
   */
  public static Codec<Long> bcdLong(int digits) {
    Preconditions.check(digits >= 1 && digits <= 18, LONG_DIGITS_MSG, digits);
    return new BcdCodec(digits).xmap(Converters.toLong(digits));
  }

  /**
   * Creates a codec for signed packed decimal integers, the format COBOL declares as
   * {@code COMP-3}.
   *
   * <p>Digits occupy a nibble each, two to a byte, with the final nibble carrying the sign. A
   * five-digit field takes three bytes:
   *
   * <pre>{@code
   * Codec<Integer> amount = Codecs.packedInt(5);
   * amount.encode(-12345, out);   // writes 12 34 5D
   * }</pre>
   *
   * <p>Unlike {@link #bcdInt(int)}, which packs digits the same way but has no sign nibble and so
   * cannot represent a negative value.
   *
   * @param digits the number of digits (1 to 9)
   * @return a new packed decimal integer codec
   * @throws IllegalArgumentException if digits is outside 1 to 9
   */
  public static Codec<Integer> packedInt(int digits) {
    Preconditions.check(digits >= 1 && digits <= 9, INT_DIGITS_MSG, digits);
    return new PackedDecimalCodec(digits).xmap(Long::intValue, Integer::longValue);
  }

  /**
   * Creates a codec for signed packed decimal longs, the format COBOL declares as {@code COMP-3}.
   *
   * <p>Digits occupy a nibble each, two to a byte, with the final nibble carrying the sign.
   * Encoding writes {@code C} for positive and {@code D} for negative; decoding also accepts
   * {@code A}, {@code E} and {@code F} as positive and {@code B} as negative.
   *
   * <pre>{@code
   * Codec<Long> balance = Codecs.packedLong(11);
   * }</pre>
   *
   * <p>Unlike {@link #bcdLong(int)}, which packs digits the same way but has no sign nibble and so
   * cannot represent a negative value.
   *
   * @param digits the number of digits (1 to 18)
   * @return a new packed decimal long codec
   * @throws IllegalArgumentException if digits is outside 1 to 18
   */
  public static Codec<Long> packedLong(int digits) {
    Preconditions.check(digits >= 1 && digits <= 18, LONG_DIGITS_MSG, digits);
    return new PackedDecimalCodec(digits);
  }

  /**
   * Creates a codec for EBCDIC zoned decimal integers, the format COBOL declares as
   * {@code DISPLAY}.
   *
   * <p>One digit per byte as EBCDIC characters, with the sign held in the high nibble (the "zone")
   * of the final byte:
   *
   * <pre>{@code
   * Codec<Integer> quantity = Codecs.zonedInt(3);
   * quantity.encode(-123, out);   // writes F1 F2 D3
   * }</pre>
   *
   * <p>Unlike {@link #ebcdicInt(int)}, which reads the same bytes as text and cannot represent a
   * sign. Reading a signed field that way is the trap this codec closes: {@code F1 F2 D3} is the
   * EBCDIC string {@code "12L"}, so the value decodes to something plausible with no error and the
   * sign silently lost.
   *
   * @param digits the number of digits (1 to 9)
   * @return a new zoned decimal integer codec
   * @throws IllegalArgumentException if digits is outside 1 to 9
   */
  public static Codec<Integer> zonedInt(int digits) {
    Preconditions.check(digits >= 1 && digits <= 9, INT_DIGITS_MSG, digits);
    return new ZonedDecimalCodec(digits).xmap(Long::intValue, Integer::longValue);
  }

  /**
   * Creates a codec for EBCDIC zoned decimal longs, the format COBOL declares as {@code DISPLAY}.
   *
   * <p>One digit per byte as EBCDIC characters, with the sign held in the high nibble of the final
   * byte. Encoding writes zone {@code C} for positive and {@code D} for negative; decoding also
   * accepts {@code A}, {@code E} and {@code F} as positive and {@code B} as negative.
   *
   * <p>Leading bytes must carry the digit zone {@code F}, which rejects ASCII digits read as
   * EBCDIC — they would otherwise decode to the right number by accident, both encodings holding
   * the digit in the low nibble.
   *
   * @param digits the number of digits (1 to 18)
   * @return a new zoned decimal long codec
   * @throws IllegalArgumentException if digits is outside 1 to 18
   */
  public static Codec<Long> zonedLong(int digits) {
    Preconditions.check(digits >= 1 && digits <= 18, LONG_DIGITS_MSG, digits);
    return new ZonedDecimalCodec(digits);
  }

  // ---------------------------------------------------------------------------
  // ASCII numeric codecs
  // ---------------------------------------------------------------------------

  /**
   * Creates a fixed-length ASCII numeric codec that decodes to {@link Integer}.
   *
   * <p>The value is encoded as a zero-padded decimal string in US-ASCII.
   *
   * <pre>{@code
   * Codec<Integer> codec = Codecs.asciiInt(4);
   * codec.encode(42, out);   // writes "0042" in ASCII
   * codec.decode(in);        // reads "0042" in ASCII → 42
   * }</pre>
   *
   * @param digits the number of digits (1 to 9)
   * @return a new ASCII integer codec
   * @throws IllegalArgumentException if digits is not between 1 and 9
   */
  public static Codec<Integer> asciiInt(int digits) {
    Preconditions.check(digits >= 1 && digits <= 9, INT_DIGITS_MSG, digits);
    return ascii(digits).xmap(Converters.toInt(digits));
  }

  /**
   * Creates a fixed-length ASCII numeric codec that decodes to {@link Long}.
   *
   * <p>The value is encoded as a zero-padded decimal string in US-ASCII.
   *
   * <pre>{@code
   * Codec<Long> codec = Codecs.asciiLong(10);
   * codec.encode(1234567890L, out);  // writes "1234567890" in ASCII
   * codec.decode(in);                // reads "1234567890" in ASCII → 1234567890
   * }</pre>
   *
   * @param digits the number of digits (1 to 18)
   * @return a new ASCII long codec
   * @throws IllegalArgumentException if digits is not between 1 and 18
   */
  public static Codec<Long> asciiLong(int digits) {
    Preconditions.check(digits >= 1 && digits <= 18, LONG_DIGITS_MSG, digits);
    return ascii(digits).xmap(Converters.toLong(digits));
  }

  /**
   * Creates an ASCII numeric codec for values wider than a {@code long} holds.
   *
   * <p>{@link #asciiLong(int)} stops at eighteen digits. COBOL allows {@code PIC 9(31)}, and long
   * reference numbers exceed a long routinely.
   *
   * @param digits the number of digits
   * @return a new ASCII big integer codec
   * @throws IllegalArgumentException if digits is not positive
   */
  public static Codec<BigInteger> asciiBigInt(int digits) {
    Preconditions.check(digits > 0, POSITIVE_DIGITS_MSG, digits);
    return ascii(digits).xmap(Converters.toBigInt(digits));
  }

  // ---------------------------------------------------------------------------
  // EBCDIC numeric codecs
  // ---------------------------------------------------------------------------

  /**
   * Creates a fixed-length EBCDIC numeric codec that decodes to {@link Integer}.
   *
   * <p>The value is encoded as a zero-padded decimal string in EBCDIC (IBM1047).
   *
   * <pre>{@code
   * Codec<Integer> codec = Codecs.ebcdicInt(4);
   * codec.encode(42, out);   // writes "0042" in EBCDIC
   * codec.decode(in);        // reads "0042" in EBCDIC → 42
   * }</pre>
   *
   * @param digits the number of digits (1 to 9)
   * @return a new EBCDIC integer codec
   * @throws IllegalArgumentException if digits is not between 1 and 9
   */
  public static Codec<Integer> ebcdicInt(int digits) {
    Preconditions.check(digits >= 1 && digits <= 9, INT_DIGITS_MSG, digits);
    return ebcdic(digits).xmap(Converters.toInt(digits));
  }

  /**
   * Creates a fixed-length EBCDIC numeric codec that decodes to {@link Long}.
   *
   * <p>The value is encoded as a zero-padded decimal string in EBCDIC (IBM1047).
   *
   * <pre>{@code
   * Codec<Long> codec = Codecs.ebcdicLong(10);
   * codec.encode(1234567890L, out);  // writes "1234567890" in EBCDIC
   * codec.decode(in);                // reads "1234567890" in EBCDIC → 1234567890
   * }</pre>
   *
   * @param digits the number of digits (1 to 18)
   * @return a new EBCDIC long codec
   * @throws IllegalArgumentException if digits is not between 1 and 18
   */
  public static Codec<Long> ebcdicLong(int digits) {
    Preconditions.check(digits >= 1 && digits <= 18, LONG_DIGITS_MSG, digits);
    return ebcdic(digits).xmap(Converters.toLong(digits));
  }

  /**
   * Creates an EBCDIC numeric codec for values wider than a {@code long} holds.
   *
   * <p>{@link #ebcdicLong(int)} stops at eighteen digits, which is what a {@code long} carries.
   *
   * @param digits the number of digits
   * @return a new EBCDIC big integer codec
   * @throws IllegalArgumentException if digits is not positive
   */
  public static Codec<BigInteger> ebcdicBigInt(int digits) {
    Preconditions.check(digits > 0, POSITIVE_DIGITS_MSG, digits);
    return ebcdic(digits).xmap(Converters.toBigInt(digits));
  }

  // ---------------------------------------------------------------------------
  // Composition combinators
  // ---------------------------------------------------------------------------

  /**
   * Creates a new builder for a choice codec that encodes discriminated unions.
   *
   * <p>The class tag codec determines which alternative to use. The tag-to-class mapping is handled
   * externally via {@link Codec#xmap xmap}.
   *
   * @param classCodec the codec for the class tag
   * @param <V> the base type of the discriminated union
   * @return a new choice codec builder
   */
  public static <V> ChoiceCodec.Builder<V> choice(Codec<Class<? extends V>> classCodec) {
    return ChoiceCodec.builder(classCodec);
  }

  /**
   * Creates a lazy codec that defers resolution to first use, enabling recursive definitions.
   *
   * @param supplier supplies the codec on first use
   * @param <V> the value type
   * @return a new lazy codec
   */
  public static <V> Codec<V> lazy(Supplier<Codec<V>> supplier) {
    return new LazyCodec<>(supplier);
  }

  /**
   * Creates a pair codec that encodes and decodes two values sequentially.
   *
   * @param first the codec for the first value
   * @param second the codec for the second value
   * @param <A> the first value type
   * @param <B> the second value type
   * @return a new pair codec
   */
  public static <A, B> PairCodec<A, B> pair(Codec<A> first, Codec<B> second) {
    return new PairCodec<>(first, second);
  }

  /**
   * Creates a triple codec that encodes and decodes three values sequentially.
   *
   * @param first the codec for the first value
   * @param second the codec for the second value
   * @param third the codec for the third value
   * @param <A> the first value type
   * @param <B> the second value type
   * @param <C> the third value type
   * @return a new triple codec
   */
  public static <A, B, C> TripleCodec<A, B, C> triple(
      Codec<A> first, Codec<B> second, Codec<C> third) {
    return new TripleCodec<>(first, second, third);
  }

  /**
   * Creates a variable-length codec where the byte count is encoded as a prefix.
   *
   * @param lengthCodec the codec for the byte count prefix
   * @param valueCodec the codec for the value
   * @param <V> the value type
   * @return a new variable byte-length codec
   */
  public static <V> Codec<V> prefixed(Codec<Integer> lengthCodec, Codec<V> valueCodec) {
    return new VariableByteLengthCodec<>(lengthCodec, valueCodec);
  }

  /**
   * Creates a variable-length codec where the value is followed by a terminator.
   *
   * <p>Bounds a value the same way {@link #prefixed(Codec, Codec)} does — a length in front versus
   * a sentinel behind — so the same value codecs work inside either. On decode, the bytes before
   * the terminator are passed to the value codec as a bounded stream and the terminator is
   * consumed, which is what makes read-until-EOF codecs such as {@link #ascii()} and {@link
   * #listOf(Codec)} usable inside it.
   *
   * <pre>{@code
   * byte[] lf = "\n".getBytes(US_ASCII);
   * Codec<String> line = Codecs.terminated(lf, Codecs.ascii());
   * }</pre>
   *
   * <p>The terminator is a {@code byte[]} rather than a {@code String} because the correct bytes
   * depend on the encoding: a newline is {@code 0x0A} in ASCII but {@code 0x25} in EBCDIC.
   *
   * <p>Decoding reads one byte at a time, since the end of the value is not known in advance and
   * outer codecs continue reading the same stream. Wrap unbuffered sources such as {@link
   * java.io.FileInputStream} in a {@link java.io.BufferedInputStream}.
   *
   * <p>On encode the value is written followed by the terminator. A value whose encoded bytes
   * contain the terminator could not be decoded back, so it is rejected before anything is written.
   * There is no escaping mechanism.
   *
   * <p>Uses {@link Termination#OPTIONAL}. For a fixed sequence of fields separated by a delimiter,
   * prefer leaving the last field unwrapped over relying on this policy — the enclosing scope ends
   * it, the wire format is then stated in the structure, and encoding round-trips exactly.
   *
   * @param terminator the terminating byte sequence (must be non-empty)
   * @param valueCodec the codec for the value
   * @param <V> the value type
   * @return a new terminated codec
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if the terminator is empty
   */
  public static <V> Codec<V> terminated(byte[] terminator, Codec<V> valueCodec) {
    return terminated(terminator, valueCodec, Termination.OPTIONAL);
  }

  /**
   * Creates a variable-length codec where the value is followed by a terminator, with an explicit
   * policy for a final value that is not terminated.
   *
   * <p>The policy matters when the same codec is applied repeatedly and no position identifies the
   * last value, such as reading a document as a list of lines where the final line may not be
   * terminated:
   *
   * <pre>{@code
   * Codec<List<String>> lines = Codecs.listOf(Codecs.terminated(lf, Codecs.ascii()));
   * }</pre>
   *
   * @param terminator the terminating byte sequence (must be non-empty)
   * @param valueCodec the codec for the value
   * @param termination whether the terminator is required on decode
   * @param <V> the value type
   * @return a new terminated codec
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if the terminator is empty
   */
  public static <V> Codec<V> terminated(
      byte[] terminator, Codec<V> valueCodec, Termination termination) {
    return new TerminatedCodec<>(terminator, valueCodec, termination);
  }

  /**
   * Policy for decoding a value whose terminator is absent at the end of the stream.
   *
   * <p>Encoding always writes the terminator regardless of this setting, so decoding an
   * unterminated final value and re-encoding it adds one. Use {@link RecordingCodec} when the
   * original bytes must be preserved exactly.
   */
  public enum Termination {
    /**
     * A final value without a terminator is valid. Suitable for file formats, where end-of-stream
     * already marks the end of the last value.
     *
     * <p>Whatever was read is passed to the value codec, including nothing at all, so the value
     * codec decides whether empty is valid — {@link #ascii()} yields an empty string, while {@link
     * #ascii(int)} still reports end-of-stream.
     */
    OPTIONAL,

    /**
     * Every value must be terminated. Suitable for message formats, where the terminator is how the
     * end of a value is known.
     */
    REQUIRED
  }

  /**
   * Creates a variable-length codec where the item count is encoded as a prefix.
   *
   * @param lengthCodec the codec for the item count prefix
   * @param lengthOf a function that returns the item count for a given value
   * @param codecFactory a function that creates a codec for the given item count
   * @param <V> the value type
   * @return a new variable item-length codec
   */
  public static <V> Codec<V> prefixed(
      Codec<Integer> lengthCodec, ToIntFunction<V> lengthOf, IntFunction<Codec<V>> codecFactory) {
    return new VariableItemLengthCodec<>(lengthCodec, lengthOf, codecFactory);
  }

  /**
   * Creates a codec for a run of items that continues while a lookahead matches.
   *
   * <p>Before each item, {@code peekCodec} decodes a value and {@code accepts} decides whether
   * another item follows; the stream is then rewound so the item codec sees those bytes again. The
   * run ends at the first value the predicate rejects, leaving the stream positioned at that value
   * for whatever reads next.
   *
   * <pre>{@code
   * Codec<String> recordType = Codecs.ascii(2);
   *
   * Codec<List<Detail>> details =
   *     Codecs.repeatWhile(recordType, "D "::equals, Codecs.terminated(lf, detailCodec));
   * }</pre>
   *
   * <p>The input stream must support {@link java.io.InputStream#mark(int) mark}; wrap unbuffered
   * sources such as {@link java.io.FileInputStream} in a {@link java.io.BufferedInputStream}. A
   * run ends at end of stream or at the first value the predicate rejects, consuming nothing in
   * either case, so whatever reads next sees those bytes. Any other failure while reading the
   * lookahead propagates — a discriminator that cannot be decoded is an error, not a terminator.
   * The lookahead may read up to 8192 bytes; a codec that reads further cannot be rewound and is
   * reported as an error.
   *
   * <p>The item codec must consume input. One that does not would leave the lookahead unchanged and
   * the run would not terminate.
   *
   * <p>An empty run is valid. To require at least one item, add
   * {@code .validate(list -> !list.isEmpty(), "…")}.
   *
   * <p>Encoding writes the items and nothing else — the discriminator must be part of each item's
   * own encoding, or the result will not decode back.
   *
   * @param peekCodec the codec for the lookahead value
   * @param accepts the condition indicating another item follows
   * @param itemCodec the codec for a single item
   * @param <T> the lookahead value type
   * @param <V> the item type
   * @return a new codec for the run
   * @throws NullPointerException if any argument is null
   */
  public static <T, V> Codec<List<V>> repeatWhile(
      Codec<T> peekCodec, Predicate<T> accepts, Codec<V> itemCodec) {
    return new RepeatWhileCodec<>(peekCodec, accepts, itemCodec);
  }

  /**
   * Creates a fixed-length list codec that encodes/decodes exactly {@code length} items.
   *
   * @param length the exact number of items
   * @param itemCodec the codec for individual list items
   * @param <V> the item type
   * @return a new fixed list codec
   */
  public static <V> Codec<List<V>> listOf(int length, Codec<V> itemCodec) {
    return new FixedListCodec<>(itemCodec, length);
  }

  /**
   * Creates a stream list codec that reads items until EOF.
   *
   * <p>The item codec must consume input. One that does not would leave the stream unchanged and
   * the read would not terminate.
   *
   * @param itemCodec the codec for individual list items
   * @param <V> the item type
   * @return a new stream list codec
   */
  public static <V> Codec<List<V>> listOf(Codec<V> itemCodec) {
    return new StreamListCodec<>(itemCodec);
  }

  /**
   * Creates a variable-length list codec where the item count is encoded as a prefix.
   *
   * @param lengthCodec the codec for the item count prefix
   * @param itemCodec the codec for individual list items
   * @param <V> the item type
   * @return a new codec
   */
  public static <V> Codec<List<V>> listOf(Codec<Integer> lengthCodec, Codec<V> itemCodec) {
    return prefixed(lengthCodec, List::size, length -> listOf(length, itemCodec));
  }

  // ---------------------------------------------------------------------------
  // Object codecs
  // ---------------------------------------------------------------------------

  /**
   * Creates a new builder for a sequential object codec.
   *
   * @param factory factory that creates new instances during decoding
   * @param <T> the type of object to encode/decode
   * @return a new sequential object codec builder
   */
  public static <T> SequentialObjectCodec.Builder<T> sequential(Supplier<T> factory) {
    return SequentialObjectCodec.builder(factory);
  }

  /**
   * Creates a new builder for a tagged object codec.
   *
   * @param factory factory that creates new instances during decoding
   * @param tagCodec the codec used to read and write tags
   * @param <T> the type of object to encode/decode
   * @param <K> the tag key type
   * @return a new tagged object codec builder
   */
  public static <T extends Tagged<T, K>, K> TaggedObjectCodec.Builder<T, K> tagged(
      Supplier<T> factory, Codec<K> tagCodec) {
    return TaggedObjectCodec.builder(factory, tagCodec);
  }

  /**
   * Creates a new builder for a tagged object codec using {@link TaggedData}.
   *
   * @param tagCodec the codec used to read and write tags
   * @param <K> the tag key type
   * @return a new tagged object codec builder
   */
  public static <K> TaggedObjectCodec.Builder<TaggedData<K>, K> tagged(Codec<K> tagCodec) {
    return tagged(TaggedData::new, tagCodec);
  }

  // ---------------------------------------------------------------------------
  // Other codecs
  // ---------------------------------------------------------------------------

  /**
   * Creates a fixed-length binary codec.
   *
   * @param length the number of bytes
   * @return a new binary codec
   */
  public static Codec<byte[]> binary(int length) {
    return new BinaryCodec(length);
  }

  /**
   * Creates a variable-length binary codec that reads all remaining bytes from the stream.
   *
   * @return a new codec
   */
  public static Codec<byte[]> binary() {
    return new StreamBinaryCodec();
  }

  /**
   * Creates a variable-length binary codec where the byte count is encoded as a prefix.
   *
   * @param lengthCodec the codec for the byte count prefix
   * @return a new codec
   */
  public static Codec<byte[]> binary(Codec<Integer> lengthCodec) {
    return prefixed(lengthCodec, Codecs.binary());
  }

  /**
   * Creates a constant codec that always writes the expected bytes on encode (ignoring the input
   * value) and validates that the bytes match on decode.
   *
   * <p>Useful for magic numbers, version bytes, and protocol signatures. The value passed to {@link
   * Codec#encode encode} is ignored; {@code null} is acceptable.
   *
   * <pre>{@code
   * Codec<byte[]> magic = Codecs.constant(new byte[] {0x4D, 0x5A});
   * }</pre>
   *
   * @param expected the expected byte sequence (must be non-null and non-empty)
   * @return a new constant codec
   * @throws NullPointerException if expected is null
   * @throws IllegalArgumentException if expected is empty
   */
  public static Codec<byte[]> constant(byte[] expected) {
    return new ConstantCodec(expected);
  }

  /**
   * Creates a boolean codec (single byte: 0x00 = false, 0x01 = true).
   *
   * @return a new boolean codec
   */
  public static Codec<Boolean> bool() {
    return new BooleanCodec();
  }
}
