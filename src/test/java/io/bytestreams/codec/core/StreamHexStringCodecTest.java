package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
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
class StreamHexStringCodecTest {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Test
  void encode_even_length(@Randomize(intMin = 0, intMax = 0xFF) int value) throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    String hexString = String.format("%02x", value);

    EncodeResult result = codec.encode(hexString, output);

    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(hexString));
    assertThat(result.length()).isEqualTo(2);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void encode_odd_length_pads() throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("f", output);

    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x0f});
    assertThat(result.length()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void encode_empty_string() throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("", output);

    assertThat(output.toByteArray()).isEmpty();
    assertThat(result).isEqualTo(EncodeResult.EMPTY);
  }

  @Test
  void decode(@Randomize(length = 3) byte[] value) throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().build();
    ByteArrayInputStream input = new ByteArrayInputStream(value);

    String result = codec.decode(input);

    assertThat(result).isEqualTo(HEX_FORMAT.formatHex(value)).hasSize(6);
    assertThat(input.available()).isZero();
  }

  @Test
  void decode_empty_stream() throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().build();
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThat(codec.decode(input)).isEmpty();
  }

  @Test
  void roundtrip(@Randomize(intMin = 0, intMax = 0xFFFF) int value) throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().build();
    String hexString = String.format("%04x", value);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(hexString, output);

    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo(hexString);
  }

  @Test
  void roundtrip_odd_length() throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);

    // Decode returns even-length "0abc"
    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo("0abc");
  }

  @Test
  void builder_default() throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("0f", output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x0f});
  }

  @Test
  void builder_padRight_encode_odd_length(@Randomize(charMin = '0', charMax = ':') char padChar)
      throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().padRight(padChar).build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("a", output);

    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex("a" + padChar));
    assertThat(result.length()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void builder_padRight_roundtrip_odd_length(@Randomize(charMin = '0', charMax = ':') char padChar)
      throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().padRight(padChar).build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("abc", output);

    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray())))
        .isEqualTo("abc" + padChar);
  }

  @Test
  void builder_padLeft_custom_char_encode(@Randomize(charMin = '0', charMax = ':') char padChar)
      throws IOException {
    StreamHexStringCodec codec = StreamHexStringCodec.builder().padLeft(padChar).build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("a", output);

    assertThat(output.toByteArray()).isEqualTo(HEX_FORMAT.parseHex(padChar + "a"));
    assertThat(result.length()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void builder_invalid_pad_char(@Randomize(charMin = 'g', charMax = 'z') char padChar) {
    var builder = StreamHexStringCodec.builder();
    assertThatThrownBy(() -> builder.padLeft(padChar))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("padChar must be a valid hex character (0-9, a-f, A-F), but was [%s]", padChar);
  }

  @Test
  void builder_valid_pad_char_boundaries() {
    assertThat(StreamHexStringCodec.builder().padLeft('0').build()).isNotNull();
    assertThat(StreamHexStringCodec.builder().padLeft('9').build()).isNotNull();
    assertThat(StreamHexStringCodec.builder().padLeft('a').build()).isNotNull();
    assertThat(StreamHexStringCodec.builder().padRight('f').build()).isNotNull();
    assertThat(StreamHexStringCodec.builder().padLeft('A').build()).isNotNull();
    assertThat(StreamHexStringCodec.builder().padRight('F').build()).isNotNull();
  }
}
