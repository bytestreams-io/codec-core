package io.bytestreams.codec.core;

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
    HexStringCodec hexCodec = new HexStringCodec(6);
    FormattedStringCodec codec = new FormattedStringCodec(hexCodec, '0');
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo("000abc");
  }

  @Test
  void encode_left_padding_explicit() throws IOException {
    HexStringCodec hexCodec = new HexStringCodec(6);
    FormattedStringCodec codec = new FormattedStringCodec(hexCodec, '0', true);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo("000abc");
  }

  @Test
  void encode_right_padding() throws IOException {
    HexStringCodec hexCodec = new HexStringCodec(6);
    FormattedStringCodec codec = new FormattedStringCodec(hexCodec, '0', false);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo("abc000");
  }

  @Test
  void encode_already_at_length() throws IOException {
    HexStringCodec hexCodec = new HexStringCodec(6);
    FormattedStringCodec codec = new FormattedStringCodec(hexCodec, '0');
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abcdef", output);
    assertThat(HEX_FORMAT.formatHex(output.toByteArray())).isEqualTo("abcdef");
  }

  @Test
  void encode_exceeds_length() {
    HexStringCodec hexCodec = new HexStringCodec(6);
    FormattedStringCodec codec = new FormattedStringCodec(hexCodec, '0');
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    // When value exceeds length, padStart/padEnd returns original value
    // and the underlying codec throws an exception
    assertThatThrownBy(() -> codec.encode("abcdefgh", output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value length must be less than or equal to %d, but was [%d]", 6, 8);
  }

  @Test
  void decode() throws IOException {
    HexStringCodec hexCodec = new HexStringCodec(6);
    FormattedStringCodec codec = new FormattedStringCodec(hexCodec, '0');
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex("000abc"));
    assertThat(codec.decode(input)).isEqualTo("000abc");
  }

  @Test
  void roundtrip_left_padding() throws IOException {
    HexStringCodec hexCodec = new HexStringCodec(6);
    FormattedStringCodec codec = new FormattedStringCodec(hexCodec, '0');
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    // Note: decode returns padded value, not original
    assertThat(codec.decode(input)).isEqualTo("000abc");
  }

  @Test
  void roundtrip_right_padding() throws IOException {
    HexStringCodec hexCodec = new HexStringCodec(6);
    FormattedStringCodec codec = new FormattedStringCodec(hexCodec, '0', false);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    // Note: decode returns padded value, not original
    assertThat(codec.decode(input)).isEqualTo("abc000");
  }
}
