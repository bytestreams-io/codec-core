package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class FormattedStringCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Test
  void encode_left_padding() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo("000abc");
  }

  @Test
  void encode_right_padding() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padRight('0').build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo("abc000");
  }

  @Test
  void encode_already_at_length() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abcdef", output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo("abcdef");
  }

  @Test
  void encode_exceeds_length() {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    // When value exceeds length, padStart/padEnd returns original value
    // and the underlying codec throws an exception
    assertThatThrownBy(() -> codec.encode("abcdefgh", output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value length must be less than or equal to %d, but was [%d]", 6, 8);
  }

  @Test
  void decode() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').build();
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex("000abc"));
    assertThat(codec.decode(input)).isEqualTo("000abc");
  }

  @Test
  void decode_trim_left() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').trim().build();
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex("000abc"));
    assertThat(codec.decode(input)).isEqualTo("abc");
  }

  @Test
  void decode_trim_right() throws IOException {
    FixedCodePointStringCodec delegate =
        FixedCodePointStringCodec.builder(6).charset(US_ASCII).build();
    FormattedStringCodec codec =
        FormattedStringCodec.builder(delegate).padRight(' ').trim().build();
    ByteArrayInputStream input = new ByteArrayInputStream("abc   ".getBytes(US_ASCII));
    assertThat(codec.decode(input)).isEqualTo("abc");
  }

  @Test
  void decode_trim_all_padding() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').trim().build();
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex("000000"));
    assertThat(codec.decode(input)).isEmpty();
  }

  @Test
  void decode_trim_no_padding_present() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').trim().build();
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex("abcdef"));
    assertThat(codec.decode(input)).isEqualTo("abcdef");
  }

  @Test
  void roundtrip_left_padding() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    // Note: decode returns padded value, not original
    assertThat(codec.decode(input)).isEqualTo("000abc");
  }

  @Test
  void roundtrip_right_padding() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padRight('0').build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    // Note: decode returns padded value, not original
    assertThat(codec.decode(input)).isEqualTo("abc000");
  }

  @Test
  void roundtrip_left_padding_with_trim() throws IOException {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(hexCodec).padLeft('0').trim().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo("abc");
  }

  @Test
  void roundtrip_right_padding_with_trim() throws IOException {
    FixedCodePointStringCodec delegate =
        FixedCodePointStringCodec.builder(6).charset(US_ASCII).build();
    FormattedStringCodec codec =
        FormattedStringCodec.builder(delegate).padRight(' ').trim().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo("abc");
  }

  @Test
  void builder_default_left_pads_with_space() throws IOException {
    FixedCodePointStringCodec delegate =
        FixedCodePointStringCodec.builder(6).charset(US_ASCII).build();
    FormattedStringCodec codec = FormattedStringCodec.builder(delegate).build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    assertThat(output.toString(US_ASCII)).isEqualTo("   abc");
  }

  @Test
  void builder_invalid_pad_char_control() {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec.Builder builder = FormattedStringCodec.builder(hexCodec);
    assertThatThrownBy(() -> builder.padLeft('\t'))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("padChar must be a printable ASCII character (0x20-0x7E), but was [\t]");
  }

  @Test
  void builder_invalid_pad_char_delete() {
    FixedHexStringCodec hexCodec = FixedHexStringCodec.builder(6).build();
    FormattedStringCodec.Builder builder = FormattedStringCodec.builder(hexCodec);
    assertThatThrownBy(() -> builder.padRight('\u007F'))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
