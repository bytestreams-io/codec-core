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
class StreamBcdStringCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Test
  void encode(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
    StreamBcdStringCodec codec = new StreamBcdStringCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    String bcdString = String.format("%02d", value);

    EncodeResult result = codec.encode(bcdString, output);

    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(bcdString));
    assertThat(result.length()).isEqualTo(2);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void encode_odd_length(@Randomize(intMin = 0, intMax = 1000) int value) throws IOException {
    StreamBcdStringCodec codec = new StreamBcdStringCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(String.format("%03d", value), output);

    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(String.format("%04d", value)));
    assertThat(result.length()).isEqualTo(4);
    assertThat(result.bytes()).isEqualTo(2);
  }

  @Test
  void encode_invalid_bcd_string() {
    StreamBcdStringCodec codec = new StreamBcdStringCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode("abc", output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid BCD string [abc]");
  }

  @Test
  void encode_empty_string() {
    StreamBcdStringCodec codec = new StreamBcdStringCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // Empty string doesn't match \\d+ pattern
    assertThatThrownBy(() -> codec.encode("", output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid BCD string []");
  }

  @Test
  void decode(@Randomize(intMin = 0, intMax = 100) int value) throws IOException {
    StreamBcdStringCodec codec = new StreamBcdStringCodec();
    String bcdString = String.format("%02d", value);
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex(bcdString));

    assertThat(codec.decode(input)).isEqualTo(bcdString);
  }

  @Test
  void decode_invalid_bcd_string() {
    StreamBcdStringCodec codec = new StreamBcdStringCodec();
    ByteArrayInputStream input = new ByteArrayInputStream(HEX_FORMAT.parseHex("ab"));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid BCD string [ab]");
  }

  @Test
  void decode_empty_stream() {
    StreamBcdStringCodec codec = new StreamBcdStringCodec();
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    // Empty string doesn't match \\d+ pattern
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid BCD string []");
  }

  @Test
  void roundtrip(@Randomize(intMin = 0, intMax = 10000) int value) throws IOException {
    StreamBcdStringCodec codec = new StreamBcdStringCodec();
    String bcdString = String.format("%04d", value);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(bcdString, output);

    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo(bcdString);
  }
}
