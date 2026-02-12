package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class StreamHexStringCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Test
  void encode_even_length(@Randomize(intMin = 0, intMax = 0xFF) int value) throws IOException {
    StreamHexStringCodec codec = new StreamHexStringCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    String hexString = String.format("%02x", value);

    EncodeResult result = codec.encode(hexString, output);

    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(hexString));
    assertThat(result.length()).isEqualTo(2);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void encode_odd_length_pads() throws IOException {
    StreamHexStringCodec codec = new StreamHexStringCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("f", output);

    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x0f});
    assertThat(result.length()).isEqualTo(2);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void encode_empty_string() throws IOException {
    StreamHexStringCodec codec = new StreamHexStringCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("", output);

    assertThat(output.toByteArray()).isEmpty();
    assertThat(result).isEqualTo(EncodeResult.EMPTY);
  }

  @Test
  void decode(@Randomize(length = 3) byte[] value) throws IOException {
    StreamHexStringCodec codec = new StreamHexStringCodec();
    ByteArrayInputStream input = new ByteArrayInputStream(value);

    String result = codec.decode(input);

    assertThat(result).isEqualTo(HEX_FORMAT.formatHex(value)).hasSize(6);
    assertThat(input.available()).isZero();
  }

  @Test
  void decode_empty_stream() throws IOException {
    StreamHexStringCodec codec = new StreamHexStringCodec();
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThat(codec.decode(input)).isEmpty();
  }

  @Test
  void roundtrip(@Randomize(intMin = 0, intMax = 0xFFFF) int value) throws IOException {
    StreamHexStringCodec codec = new StreamHexStringCodec();
    String hexString = String.format("%04x", value);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(hexString, output);

    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo(hexString);
  }

  @Test
  void roundtrip_odd_length() throws IOException {
    StreamHexStringCodec codec = new StreamHexStringCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);

    // Decode returns even-length "0abc"
    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo("0abc");
  }
}
