package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class StringLongCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Test
  void encode_default_radix(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
    BcdStringCodec bcdCodec = new BcdStringCodec(2);
    StringLongCodec codec = new StringLongCodec(bcdCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode((long) value, output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo(String.format("%02d", value));
  }

  @Test
  void encode(
      @Randomize(longMin = 0, longMax = Long.MAX_VALUE) long value,
      @Randomize(intMin = 2, intMax = 17) int radix)
      throws IOException {
    String string = Long.toString(value, radix);
    int length = string.length() + (string.length() % 2);
    HexStringCodec hexCodec = new HexStringCodec(length);
    StringLongCodec codec = new StringLongCodec(hexCodec, radix);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).endsWith(string);
  }

  @Test
  void encode_overflow() {
    HexStringCodec hexCodec = new HexStringCodec(2);
    StringLongCodec codec = new StringLongCodec(hexCodec, 16);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    long value = 0x1FFL;
    assertThatThrownBy(() -> codec.encode(value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value length must be less than or equal to %d, but was [%d]", 2, 3);
  }

  @Test
  void invalid_radix_too_low() {
    HexStringCodec hexCodec = new HexStringCodec(2);
    assertThatThrownBy(() -> new StringLongCodec(hexCodec, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "radix must be between %d and %d, but was [%d]",
            Character.MIN_RADIX, Character.MAX_RADIX, 1);
  }

  @Test
  void invalid_radix_too_high() {
    HexStringCodec hexCodec = new HexStringCodec(2);
    assertThatThrownBy(() -> new StringLongCodec(hexCodec, 37))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "radix must be between %d and %d, but was [%d]",
            Character.MIN_RADIX, Character.MAX_RADIX, 37);
  }

  @Test
  void decode(
      @Randomize(longMin = 0, longMax = Long.MAX_VALUE) long value,
      @Randomize(intMin = 2, intMax = 17) int radix)
      throws IOException {
    String string = Long.toString(value, radix);
    int length = string.length() + (string.length() % 2);
    String padded = "0".repeat(length - string.length()) + string;
    HexStringCodec hexCodec = new HexStringCodec(length);
    StringLongCodec codec = new StringLongCodec(hexCodec, radix);
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(padded));
    assertThat(codec.decode(input)).isEqualTo(value);
  }

  @Test
  void decode_overflow() {
    HexStringCodec hexCodec = new HexStringCodec(20);
    StringLongCodec codec = new StringLongCodec(hexCodec, 16);
    // 20 hex digits exceeds Long.MAX_VALUE
    ByteArrayInputStream input =
        new ByteArrayInputStream(HEX_FORMAT.parseHex("ffffffffffffffffffff"));
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(NumberFormatException.class)
        .hasMessageContaining("For input string");
  }

  @Test
  void roundtrip(
      @Randomize(longMin = 0, longMax = Long.MAX_VALUE) long value,
      @Randomize(intMin = 2, intMax = 17) int radix)
      throws IOException {
    String string = Long.toString(value, radix);
    int length = string.length() + (string.length() % 2);
    HexStringCodec hexCodec = new HexStringCodec(length);
    StringLongCodec codec = new StringLongCodec(hexCodec, radix);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertThat(codec.decode(input)).isEqualTo(value);
  }
}
