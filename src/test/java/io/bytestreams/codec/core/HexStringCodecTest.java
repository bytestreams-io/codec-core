package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class HexStringCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Test
  void getLength(@Randomize(intMin = 1, intMax = 100) int length) {
    HexStringCodec codec = new HexStringCodec(length);
    assertThat(codec.getLength()).isEqualTo(length);
  }

  @Test
  void encode(@Randomize(intMin = 0, intMax = 0xFF) int value) throws IOException {
    HexStringCodec codec = new HexStringCodec(2);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    String expected = String.format("%02x", value);
    codec.encode(expected, output);
    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(expected));
  }

  @Test
  void encode_odd_length(@Randomize(intMin = 0, intMax = 0x0FFF) int value) throws IOException {
    HexStringCodec codec = new HexStringCodec(3);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    String expected = String.format("%03x", value);
    codec.encode(expected, output);
    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(String.format("%04x", value)));
  }

  @Test
  void encode_short_value() throws IOException {
    HexStringCodec codec = new HexStringCodec(4);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("f", output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x00, 0x0f});
  }

  @Test
  void encode_overflow(@Randomize(intMin = 0, intMax = 0x0FFF) int value) {
    HexStringCodec codec = new HexStringCodec(2);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    String data = String.format("%03x", value);
    assertThatThrownBy(() -> codec.encode(data, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value length must be less than or equal to %d, but was [%d]", 2, 3);
  }

  @Test
  void decode(@Randomize(length = 3) byte[] value) throws IOException {
    HexStringCodec codec = new HexStringCodec(2);
    ByteArrayInputStream input = new ByteArrayInputStream(value);
    assertThat(codec.decode(input))
        .isEqualTo(HEX_FORMAT.formatHex(Arrays.copyOfRange(value, 0, 1)));
    assertThat(input.available()).isPositive();
  }

  @Test
  void decode_odd_length(@Randomize(length = 3) byte[] value) throws IOException {
    HexStringCodec codec = new HexStringCodec(3);
    ByteArrayInputStream input = new ByteArrayInputStream(value);
    String expected = HEX_FORMAT.formatHex(Arrays.copyOfRange(value, 0, 2)).substring(1, 4);
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void decode_insufficient_data(@Randomize(length = 1) byte[] value) {
    HexStringCodec codec = new HexStringCodec(3);
    ByteArrayInputStream input = new ByteArrayInputStream(value);
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(EOFException.class)
        .hasMessage(
            "End of stream reached after reading %d bytes, bytes expected [%d]", value.length, 2);
  }

  @Test
  void constructor_non_positive_length() {
    assertThatThrownBy(() -> new HexStringCodec(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("0");
    assertThatThrownBy(() -> new HexStringCodec(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }
}
