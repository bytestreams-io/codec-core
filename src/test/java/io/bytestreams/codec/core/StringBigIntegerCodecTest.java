package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class StringBigIntegerCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Test
  void encode_default_radix(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
    BcdStringCodec bcdCodec = new BcdStringCodec(2);
    StringBigIntegerCodec codec = new StringBigIntegerCodec(bcdCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(BigInteger.valueOf(value), output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo(String.format("%02d", value));
  }

  @Test
  void encode(@Randomize BigInteger value, @Randomize(intMin = 2, intMax = 17) int radix)
      throws IOException {
    BigInteger absValue = value.abs();
    String string = absValue.toString(radix);
    int length = string.length() + (string.length() % 2);
    HexStringCodec hexCodec = new HexStringCodec(length);
    StringBigIntegerCodec codec = new StringBigIntegerCodec(hexCodec, radix);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(absValue, output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).endsWith(string);
  }

  @Test
  void encode_overflow() {
    HexStringCodec hexCodec = new HexStringCodec(2);
    StringBigIntegerCodec codec = new StringBigIntegerCodec(hexCodec, 16);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    BigInteger value = BigInteger.valueOf(0x1FF);
    assertThatThrownBy(() -> codec.encode(value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value length must be less than or equal to %d, but was [%d]", 2, 3);
  }

  @Test
  void invalid_radix_too_low() {
    HexStringCodec hexCodec = new HexStringCodec(2);
    assertThatThrownBy(() -> new StringBigIntegerCodec(hexCodec, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "radix must be between %d and %d, but was [%d]",
            Character.MIN_RADIX, Character.MAX_RADIX, 1);
  }

  @Test
  void invalid_radix_too_high() {
    HexStringCodec hexCodec = new HexStringCodec(2);
    assertThatThrownBy(() -> new StringBigIntegerCodec(hexCodec, 37))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "radix must be between %d and %d, but was [%d]",
            Character.MIN_RADIX, Character.MAX_RADIX, 37);
  }

  @Test
  void decode(@Randomize BigInteger value, @Randomize(intMin = 2, intMax = 17) int radix)
      throws IOException {
    BigInteger absValue = value.abs();
    String string = absValue.toString(radix);
    int length = string.length() + (string.length() % 2);
    String padded = "0".repeat(length - string.length()) + string;
    HexStringCodec hexCodec = new HexStringCodec(length);
    StringBigIntegerCodec codec = new StringBigIntegerCodec(hexCodec, radix);
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(padded));
    assertThat(codec.decode(input)).isEqualTo(absValue);
  }

  @Test
  void decode_invalid() {
    HexStringCodec hexCodec = new HexStringCodec(2);
    StringBigIntegerCodec codec = new StringBigIntegerCodec(hexCodec, 10);
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
    HexStringCodec hexCodec = new HexStringCodec(length);
    StringBigIntegerCodec codec = new StringBigIntegerCodec(hexCodec, radix);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(absValue, output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertThat(codec.decode(input)).isEqualTo(absValue);
  }

  @Test
  void roundtrip_with_code_point_string_codec(@Randomize BigInteger value) throws IOException {
    BigInteger absValue = value.abs();
    String string = absValue.toString();
    CodePointStringCodec codePointCodec = new CodePointStringCodec(string.length(), UTF_8);
    StringBigIntegerCodec codec = new StringBigIntegerCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(absValue, output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertThat(codec.decode(input)).isEqualTo(absValue);
  }
}
