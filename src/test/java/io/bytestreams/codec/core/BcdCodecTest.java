package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class BcdCodecTest {

  @Test
  void encode_even_digits() throws IOException {
    BcdCodec codec = new BcdCodec(4);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("1234", output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x12, 0x34});
  }

  @Test
  void encode_odd_digits() throws IOException {
    BcdCodec codec = new BcdCodec(3);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("123", output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x01, 0x23});
  }

  @Test
  void encode_single_digit() throws IOException {
    BcdCodec codec = new BcdCodec(1);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("5", output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x05});
  }

  @Test
  void encode_short_value() throws IOException {
    BcdCodec codec = new BcdCodec(4);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("12", output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x00, 0x12});
  }

  @Test
  void encode_all_zeros() throws IOException {
    BcdCodec codec = new BcdCodec(4);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("0000", output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x00, 0x00});
  }

  @Test
  void encode_result() throws IOException {
    BcdCodec codec = new BcdCodec(3);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    EncodeResult result = codec.encode("123", output);
    assertThat(result.count()).isEqualTo(3);
    assertThat(result.bytes()).isEqualTo(2);
  }

  @Test
  void encode_overflow() {
    BcdCodec codec = new BcdCodec(2);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode("123", output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void encode_non_digit_letter() {
    BcdCodec codec = new BcdCodec(4);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode("A234", output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid BCD value: A234");
  }

  @Test
  void encode_non_digit_special_char() {
    BcdCodec codec = new BcdCodec(4);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode("12 4", output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid BCD value: 12 4");
  }

  @Test
  void decode_even_digits() throws IOException {
    BcdCodec codec = new BcdCodec(4);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x12, 0x34});
    assertThat(codec.decode(input)).isEqualTo("1234");
  }

  @Test
  void decode_odd_digits() throws IOException {
    BcdCodec codec = new BcdCodec(3);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01, 0x23});
    assertThat(codec.decode(input)).isEqualTo("123");
  }

  @Test
  void decode_single_digit() throws IOException {
    BcdCodec codec = new BcdCodec(1);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x05});
    assertThat(codec.decode(input)).isEqualTo("5");
  }

  @Test
  void decode_all_zeros() throws IOException {
    BcdCodec codec = new BcdCodec(4);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x00, 0x00});
    assertThat(codec.decode(input)).isEqualTo("0000");
  }

  @Test
  void decode_does_not_consume_extra_bytes() throws IOException {
    BcdCodec codec = new BcdCodec(2);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x12, 0x34, 0x56});
    assertThat(codec.decode(input)).isEqualTo("12");
    assertThat(input.available()).isEqualTo(2);
  }

  @Test
  void decode_invalid_hex_digit() {
    BcdCodec codec = new BcdCodec(2);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {(byte) 0x1A});
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("invalid BCD value: 1A");
  }

  @Test
  void decode_insufficient_data() {
    BcdCodec codec = new BcdCodec(4);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x12});
    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void constructor_zero_digits() {
    assertThatThrownBy(() -> new BcdCodec(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("0");
  }

  @Test
  void constructor_negative_digits() {
    assertThatThrownBy(() -> new BcdCodec(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }
}
