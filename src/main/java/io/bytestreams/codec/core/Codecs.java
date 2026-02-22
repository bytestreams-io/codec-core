package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.bytestreams.codec.core.util.Strings;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.IntFunction;
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
 * // Composition
 * Codec<String> prefixed = Codecs.prefixed(Codecs.uint16(), Codecs.utf8());
 * Codec<List<Integer>> list = Codecs.listOf(Codecs.uint8(), 5);
 *
 * // Object codecs
 * SequentialObjectCodec<Msg> codec = Codecs.<Msg>sequential(Msg::new)
 *     .field("id", Codecs.int32(), Msg::getId, Msg::setId)
 *     .build();
 * }</pre>
 */
public class Codecs {
  private static final Charset EBCDIC = Charset.forName("IBM1047");

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
   */
  public static Codec<Integer> uint16() {
    return BinaryNumberCodec.ofUnsignedShort();
  }

  /**
   * Creates a codec for unsigned integer values (0 to 4294967295), encoded as 4-byte big-endian
   * binary.
   *
   * @return a new unsigned integer codec
   */
  public static Codec<Long> uint32() {
    return BinaryNumberCodec.ofUnsignedInt();
  }

  /**
   * Creates a codec for signed short values (-32768 to 32767), encoded as 2-byte big-endian binary.
   *
   * @return a new signed short codec
   */
  public static Codec<Short> int16() {
    return BinaryNumberCodec.ofShort();
  }

  /**
   * Creates a codec for signed integer values (-2147483648 to 2147483647), encoded as 4-byte
   * big-endian binary.
   *
   * @return a new signed integer codec
   */
  public static Codec<Integer> int32() {
    return BinaryNumberCodec.ofInt();
  }

  /**
   * Creates a codec for signed long values, encoded as 8-byte big-endian binary.
   *
   * @return a new signed long codec
   */
  public static Codec<Long> int64() {
    return BinaryNumberCodec.ofLong();
  }

  /**
   * Creates a codec for float values (IEEE 754 single-precision, 4 bytes).
   *
   * @return a new float codec
   */
  public static Codec<Float> float32() {
    return BinaryNumberCodec.ofFloat();
  }

  /**
   * Creates a codec for double values (IEEE 754 double-precision, 8 bytes).
   *
   * @return a new double codec
   */
  public static Codec<Double> float64() {
    return BinaryNumberCodec.ofDouble();
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
   * Creates a variable-length UTF-8 string codec where the code point count is encoded as a
   * prefix.
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
   * <p>For single-byte charsets, uses {@link String#length()} for the count (O(1)). For multi-byte
   * charsets, uses {@link io.bytestreams.codec.core.util.Strings#codePointCount} (O(n)).
   *
   * @param charset the charset
   * @param lengthCodec the codec for the code point count prefix
   * @return a new codec
   */
  public static Codec<String> ofCharset(Charset charset, Codec<Integer> lengthCodec) {
    ToIntFunction<String> lengthOf =
        charset.newEncoder().maxBytesPerChar() == 1 ? String::length : Strings::codePointCount;
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
   * Creates a fixed-length list codec that encodes/decodes exactly {@code length} items.
   *
   * @param itemCodec the codec for individual list items
   * @param length the exact number of items
   * @param <V> the item type
   * @return a new fixed list codec
   */
  public static <V> Codec<List<V>> listOf(Codec<V> itemCodec, int length) {
    return new FixedListCodec<>(itemCodec, length);
  }

  /**
   * Creates a stream list codec that reads items until EOF.
   *
   * @param itemCodec the codec for individual list items
   * @param <V> the item type
   * @return a new stream list codec
   */
  public static <V> Codec<List<V>> listOf(Codec<V> itemCodec) {
    return new StreamListCodec<>(itemCodec);
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
   * Creates a constant codec that always writes the expected bytes on encode (ignoring the input
   * value) and validates that the bytes match on decode.
   *
   * <p>Useful for magic numbers, version bytes, and protocol signatures. The value passed to
   * {@link Codec#encode encode} is ignored; {@code null} is acceptable.
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
