package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HexFormat;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class StringNumberCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Nested
  class IntegerTests {
    @Test
    void encode_default_radix(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<Integer> codec = StringNumberCodec.builder(hexCodec).ofInt();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(HEX_FORMAT.formatHex(output.toByteArray()))
          .isEqualTo(String.format("%02d", value));
    }

    @Test
    void encode(
        @Randomize(intMin = 0, intMax = Integer.MAX_VALUE) int value,
        @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Integer.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Integer> codec = StringNumberCodec.builder(hexCodec).ofInt(radix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(HEX_FORMAT.formatHex(output.toByteArray())).endsWith(string);
    }

    @Test
    void encode_overflow() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<Integer> codec = StringNumberCodec.builder(hexCodec).ofInt(16);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      int value = 0x1FF;
      assertThatThrownBy(() -> codec.encode(value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("value length must be less than or equal to %d, but was [%d]", 2, 3);
    }

    @Test
    void decode(
        @Randomize(intMin = 0, intMax = Integer.MAX_VALUE) int value,
        @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Integer.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      String padded = "0".repeat(length - string.length()) + string;
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Integer> codec = StringNumberCodec.builder(hexCodec).ofInt(radix);
      ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(padded));
      assertThat(codec.decode(input)).isEqualTo(value);
    }

    @Test
    void decode_overflow() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(16).build();
      StringNumberCodec<Integer> codec = StringNumberCodec.builder(hexCodec).ofInt(16);
      // ffffffffffffffff exceeds Integer.MAX_VALUE
      ByteArrayInputStream input =
          new ByteArrayInputStream(HEX_FORMAT.parseHex("ffffffffffffffff"));
      assertThatThrownBy(() -> codec.decode(input))
          .isInstanceOf(CodecException.class)
          .hasMessageContaining("failed to parse number from string")
          .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void roundtrip(
        @Randomize(intMin = 0, intMax = Integer.MAX_VALUE) int value,
        @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Integer.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Integer> codec = StringNumberCodec.builder(hexCodec).ofInt(radix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(value);
    }

    @Test
    void roundtrip_with_code_point_string_codec(
        @Randomize(intMin = 0, intMax = Integer.MAX_VALUE) int value) throws IOException {
      String string = Integer.toString(value);
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Integer> codec = StringNumberCodec.builder(codePointCodec).ofInt();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(value);
    }
  }

  @Nested
  class LongTests {
    @Test
    void encode_default_radix(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<Long> codec = StringNumberCodec.builder(hexCodec).ofLong();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode((long) value, output);
      assertThat(HEX_FORMAT.formatHex(output.toByteArray()))
          .isEqualTo(String.format("%02d", value));
    }

    @Test
    void encode(
        @Randomize(longMin = 0, longMax = Long.MAX_VALUE) long value,
        @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Long.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Long> codec = StringNumberCodec.builder(hexCodec).ofLong(radix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(HEX_FORMAT.formatHex(output.toByteArray())).endsWith(string);
    }

    @Test
    void encode_overflow() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<Long> codec = StringNumberCodec.builder(hexCodec).ofLong(16);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      long value = 0x1FFL;
      assertThatThrownBy(() -> codec.encode(value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("value length must be less than or equal to %d, but was [%d]", 2, 3);
    }

    @Test
    void decode(
        @Randomize(longMin = 0, longMax = Long.MAX_VALUE) long value,
        @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Long.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      String padded = "0".repeat(length - string.length()) + string;
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Long> codec = StringNumberCodec.builder(hexCodec).ofLong(radix);
      ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(padded));
      assertThat(codec.decode(input)).isEqualTo(value);
    }

    @Test
    void decode_overflow() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(20).build();
      StringNumberCodec<Long> codec = StringNumberCodec.builder(hexCodec).ofLong(16);
      // 20 hex digits exceeds Long.MAX_VALUE
      ByteArrayInputStream input =
          new ByteArrayInputStream(HEX_FORMAT.parseHex("ffffffffffffffffffff"));
      assertThatThrownBy(() -> codec.decode(input))
          .isInstanceOf(CodecException.class)
          .hasMessageContaining("failed to parse number from string")
          .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void roundtrip(
        @Randomize(longMin = 0, longMax = Long.MAX_VALUE) long value,
        @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Long.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Long> codec = StringNumberCodec.builder(hexCodec).ofLong(radix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(value);
    }

    @Test
    void roundtrip_with_code_point_string_codec(
        @Randomize(longMin = 0, longMax = Long.MAX_VALUE) long value) throws IOException {
      String string = Long.toString(value);
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Long> codec = StringNumberCodec.builder(codePointCodec).ofLong();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(value);
    }
  }

  @Nested
  class ShortTests {
    @Test
    void encode_default_radix(@Randomize(shortMin = 0, shortMax = 100) short value)
        throws IOException {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<Short> codec = StringNumberCodec.builder(hexCodec).ofShort();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(HEX_FORMAT.formatHex(output.toByteArray()))
          .isEqualTo(String.format("%02d", value));
    }

    @Test
    void encode(@Randomize(shortMin = 0) short value, @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Integer.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Short> codec = StringNumberCodec.builder(hexCodec).ofShort(radix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(HEX_FORMAT.formatHex(output.toByteArray())).endsWith(string);
    }

    @Test
    void encode_overflow() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<Short> codec = StringNumberCodec.builder(hexCodec).ofShort(16);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      short value = 0x1FF;
      assertThatThrownBy(() -> codec.encode(value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("value length must be less than or equal to %d, but was [%d]", 2, 3);
    }

    @Test
    void decode(@Randomize(shortMin = 0) short value, @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Integer.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      String padded = "0".repeat(length - string.length()) + string;
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Short> codec = StringNumberCodec.builder(hexCodec).ofShort(radix);
      ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(padded));
      assertThat(codec.decode(input)).isEqualTo(value);
    }

    @Test
    void decode_overflow() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(16).build();
      StringNumberCodec<Short> codec = StringNumberCodec.builder(hexCodec).ofShort(16);
      // ffffffffffffffff exceeds Short.MAX_VALUE
      ByteArrayInputStream input =
          new ByteArrayInputStream(HEX_FORMAT.parseHex("ffffffffffffffff"));
      assertThatThrownBy(() -> codec.decode(input))
          .isInstanceOf(CodecException.class)
          .hasMessageContaining("failed to parse number from string")
          .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void roundtrip(
        @Randomize(shortMin = 0) short value, @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      String string = Integer.toString(value, radix);
      int length = string.length() + (string.length() % 2);
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<Short> codec = StringNumberCodec.builder(hexCodec).ofShort(radix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(value);
    }

    @Test
    void roundtrip_with_code_point_string_codec(@Randomize(shortMin = 0) short value)
        throws IOException {
      String string = Integer.toString(value);
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Short> codec = StringNumberCodec.builder(codePointCodec).ofShort();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(value);
    }
  }

  @Nested
  class BigIntegerTests {
    @Test
    void encode_default_radix(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<BigInteger> codec = StringNumberCodec.builder(hexCodec).ofBigInt();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(BigInteger.valueOf(value), output);
      assertThat(HEX_FORMAT.formatHex(output.toByteArray()))
          .isEqualTo(String.format("%02d", value));
    }

    @Test
    void encode(@Randomize BigInteger value, @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      BigInteger absValue = value.abs();
      String string = absValue.toString(radix);
      int length = string.length() + (string.length() % 2);
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<BigInteger> codec = StringNumberCodec.builder(hexCodec).ofBigInt(radix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(absValue, output);
      assertThat(HEX_FORMAT.formatHex(output.toByteArray())).endsWith(string);
    }

    @Test
    void encode_overflow() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<BigInteger> codec = StringNumberCodec.builder(hexCodec).ofBigInt(16);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      BigInteger value = BigInteger.valueOf(0x1FF);
      assertThatThrownBy(() -> codec.encode(value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("value length must be less than or equal to %d, but was [%d]", 2, 3);
    }

    @Test
    void decode(@Randomize BigInteger value, @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      BigInteger absValue = value.abs();
      String string = absValue.toString(radix);
      int length = string.length() + (string.length() % 2);
      String padded = "0".repeat(length - string.length()) + string;
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<BigInteger> codec = StringNumberCodec.builder(hexCodec).ofBigInt(radix);
      ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(padded));
      assertThat(codec.decode(input)).isEqualTo(absValue);
    }

    @Test
    void decode_invalid() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec<BigInteger> codec = StringNumberCodec.builder(hexCodec).ofBigInt(10);
      // "zz" is not a valid decimal number
      ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex("7a7a"));
      assertThatThrownBy(() -> codec.decode(input))
          .isInstanceOf(CodecException.class)
          .hasMessageContaining("failed to parse number from string")
          .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void roundtrip(@Randomize BigInteger value, @Randomize(intMin = 2, intMax = 17) int radix)
        throws IOException {
      BigInteger absValue = value.abs();
      String string = absValue.toString(radix);
      int length = string.length() + (string.length() % 2);
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(length).build();
      StringNumberCodec<BigInteger> codec = StringNumberCodec.builder(hexCodec).ofBigInt(radix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(absValue, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(absValue);
    }

    @Test
    void roundtrip_with_code_point_string_codec(@Randomize BigInteger value) throws IOException {
      BigInteger absValue = value.abs();
      String string = absValue.toString();
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<BigInteger> codec = StringNumberCodec.builder(codePointCodec).ofBigInt();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(absValue, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(absValue);
    }
  }

  @Nested
  class DoubleTests {
    @Test
    void encode(@Randomize(doubleMin = 0, doubleMax = Double.MAX_VALUE) double value)
        throws IOException {
      String string = Double.toString(value);
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Double> codec = StringNumberCodec.builder(codePointCodec).ofDouble();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toString(UTF_8)).isEqualTo(string);
    }

    @Test
    void encode_overflow() {
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(3).build();
      StringNumberCodec<Double> codec = StringNumberCodec.builder(codePointCodec).ofDouble();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      double value = 1234.5;
      assertThatThrownBy(() -> codec.encode(value, output))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode() throws IOException {
      String string = "3.14";
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Double> codec = StringNumberCodec.builder(codePointCodec).ofDouble();
      ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes(UTF_8));
      assertThat(codec.decode(input)).isEqualTo(3.14);
    }

    @Test
    void decode_invalid() {
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(3).build();
      StringNumberCodec<Double> codec = StringNumberCodec.builder(codePointCodec).ofDouble();
      ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(UTF_8));
      assertThatThrownBy(() -> codec.decode(input))
          .isInstanceOf(CodecException.class)
          .hasMessageContaining("failed to parse number from string")
          .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void roundtrip(@Randomize(doubleMin = 0, doubleMax = Double.MAX_VALUE) double value)
        throws IOException {
      String string = Double.toString(value);
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Double> codec = StringNumberCodec.builder(codePointCodec).ofDouble();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(value);
    }
  }

  @Nested
  class FloatTests {
    @Test
    void encode(@Randomize(floatMin = 0, floatMax = Float.MAX_VALUE) float value)
        throws IOException {
      String string = Float.toString(value);
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Float> codec = StringNumberCodec.builder(codePointCodec).ofFloat();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toString(UTF_8)).isEqualTo(string);
    }

    @Test
    void encode_overflow() {
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(3).build();
      StringNumberCodec<Float> codec = StringNumberCodec.builder(codePointCodec).ofFloat();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      float value = 1234.5f;
      assertThatThrownBy(() -> codec.encode(value, output))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode() throws IOException {
      String string = "3.14";
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Float> codec = StringNumberCodec.builder(codePointCodec).ofFloat();
      ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes(UTF_8));
      assertThat(codec.decode(input)).isEqualTo(3.14f);
    }

    @Test
    void decode_invalid() {
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(3).build();
      StringNumberCodec<Float> codec = StringNumberCodec.builder(codePointCodec).ofFloat();
      ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(UTF_8));
      assertThatThrownBy(() -> codec.decode(input))
          .isInstanceOf(CodecException.class)
          .hasMessageContaining("failed to parse number from string")
          .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void roundtrip(@Randomize(floatMin = 0, floatMax = Float.MAX_VALUE) float value)
        throws IOException {
      String string = Float.toString(value);
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<Float> codec = StringNumberCodec.builder(codePointCodec).ofFloat();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualTo(value);
    }
  }

  @Nested
  class BigDecimalTests {
    @Test
    void encode(@Randomize BigDecimal value) throws IOException {
      String string = value.toPlainString();
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<BigDecimal> codec =
          StringNumberCodec.builder(codePointCodec).ofBigDecimal();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toString(UTF_8)).isEqualTo(string);
    }

    @Test
    void encode_overflow() {
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(3).build();
      StringNumberCodec<BigDecimal> codec =
          StringNumberCodec.builder(codePointCodec).ofBigDecimal();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      BigDecimal value = new BigDecimal("1234.56");
      assertThatThrownBy(() -> codec.encode(value, output))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode() throws IOException {
      String string = "3.14";
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<BigDecimal> codec =
          StringNumberCodec.builder(codePointCodec).ofBigDecimal();
      ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes(UTF_8));
      assertThat(codec.decode(input)).isEqualByComparingTo(new BigDecimal("3.14"));
    }

    @Test
    void decode_invalid() {
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(3).build();
      StringNumberCodec<BigDecimal> codec =
          StringNumberCodec.builder(codePointCodec).ofBigDecimal();
      ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(UTF_8));
      assertThatThrownBy(() -> codec.decode(input))
          .isInstanceOf(CodecException.class)
          .hasMessageContaining("failed to parse number from string")
          .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void roundtrip(@Randomize BigDecimal value) throws IOException {
      String string = value.toPlainString();
      FixedCodePointStringCodec codePointCodec = StringCodecs.ofCodePoint(string.length()).build();
      StringNumberCodec<BigDecimal> codec =
          StringNumberCodec.builder(codePointCodec).ofBigDecimal();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
      assertThat(codec.decode(input)).isEqualByComparingTo(value);
    }
  }

  @Nested
  class BuilderValidationTests {
    @Test
    void builder_string_codec_null() {
      assertThatThrownBy(() -> StringNumberCodec.builder(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("stringCodec");
    }

    @Test
    void builder_invalid_radix_too_low() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec.Builder builder = StringNumberCodec.builder(hexCodec);
      assertThatThrownBy(() -> builder.ofInt(1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "radix must be between %d and %d, but was [%d]",
              Character.MIN_RADIX, Character.MAX_RADIX, 1);
    }

    @Test
    void builder_invalid_radix_too_high() {
      FixedHexStringCodec hexCodec = StringCodecs.ofHex(2).build();
      StringNumberCodec.Builder builder = StringNumberCodec.builder(hexCodec);
      assertThatThrownBy(() -> builder.ofInt(37))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "radix must be between %d and %d, but was [%d]",
              Character.MIN_RADIX, Character.MAX_RADIX, 37);
    }
  }
}
