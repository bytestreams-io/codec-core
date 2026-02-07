package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class BcdStringCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Test
  void getLength(@Randomize(intMin = 1, intMax = 100) int length) {
    BcdStringCodec codec = new BcdStringCodec(length);
    assertThat(codec.getLength()).isEqualTo(length);
  }

  @Test
  void encode(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
    BcdStringCodec codec = new BcdStringCodec(2);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    String bcdString = String.format("%02d", value);
    codec.encode(bcdString, output);
    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(bcdString));
  }

  @Test
  void encode_odd_length(@Randomize(intMin = 0, intMax = 1000) int value) throws IOException {
    BcdStringCodec codec = new BcdStringCodec(3);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(String.format("%03d", value), output);
    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(String.format("%04d", value)));
  }

  @Test
  void encode_overflow(@Randomize(intMin = 0, intMax = 1000) int value) {
    BcdStringCodec codec = new BcdStringCodec(2);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    String data = String.format("%03d", value);
    assertThatThrownBy(() -> codec.encode(data, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value length must be less than or equal to %d, but was [%d]", 2, 3);
  }

  @Test
  void encode_invalid_bcd_string() {
    BcdStringCodec codec = new BcdStringCodec(3);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode("abc", output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid BCD string [abc]");
  }

  @Test
  void decode(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
    BcdStringCodec codec = new BcdStringCodec(2);
    String bcdString = String.format("%02d", value);
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(bcdString));
    assertThat(codec.decode(input)).isEqualTo(bcdString);
  }

  @Test
  void decode_odd_length(@Randomize(intMin = 0, intMax = 1000) int value) throws IOException {
    BcdStringCodec codec = new BcdStringCodec(3);
    String expected = String.format("%03d", value);
    ByteArrayInputStream input =
        new ByteArrayInputStream(HEX_FORMAT.parseHex(String.format("%04d", value)));
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void decode_insufficient_data(@Randomize(intMin = 0, intMax = 100) int value) {
    BcdStringCodec codec = new BcdStringCodec(3);
    String bcdString = String.format("%02d", value);
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(bcdString));
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(EOFException.class)
        .hasMessage("End of stream reached after reading %d bytes, bytes expected [%d]", 1, 2);
  }

  @Test
  void decode_invalid_bcd_string() {
    BcdStringCodec codec = new BcdStringCodec(3);
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex("0abc"));
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid BCD string [abc]");
  }
}
