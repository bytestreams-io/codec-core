package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ConstantCodecTest {

  @Test
  void encode() throws IOException {
    ConstantCodec codec = new ConstantCodec(new byte[] {0x4D, 0x5A});
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    EncodeResult result = codec.encode(new byte[] {0x00, 0x00}, output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x4D, 0x5A});
    assertThat(result.bytes()).isEqualTo(2);
  }

  @Test
  void encode_null_value() throws IOException {
    ConstantCodec codec = new ConstantCodec(new byte[] {0x4D, 0x5A});
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    EncodeResult result = codec.encode(null, output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x4D, 0x5A});
    assertThat(result.bytes()).isEqualTo(2);
  }

  @Test
  void decode() throws IOException {
    ConstantCodec codec = new ConstantCodec(new byte[] {0x4D, 0x5A});
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x4D, 0x5A});
    assertThat(codec.decode(input)).isEqualTo(new byte[] {0x4D, 0x5A});
  }

  @Test
  void decode_mismatch() {
    ConstantCodec codec = new ConstantCodec(new byte[] {0x4D, 0x5A});
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x00, 0x00});
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("expected constant [4D5A] but got [0000]");
  }

  @Test
  void decode_insufficient_data() {
    ConstantCodec codec = new ConstantCodec(new byte[] {0x4D, 0x5A});
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x4D});
    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_consumes_only_required_bytes() throws IOException {
    ConstantCodec codec = new ConstantCodec(new byte[] {0x4D, 0x5A});
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x4D, 0x5A, 0x01, 0x02});
    codec.decode(input);
    assertThat(input.available()).isEqualTo(2);
  }

  @Test
  void roundtrip() throws IOException {
    ConstantCodec codec = new ConstantCodec(new byte[] {0x4D, 0x5A});
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(null, output);
    byte[] decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded).isEqualTo(new byte[] {0x4D, 0x5A});
  }

  @Test
  void single_byte_constant() throws IOException {
    ConstantCodec codec = new ConstantCodec(new byte[] {0x01});
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(null, output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x01});
    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray())))
        .isEqualTo(new byte[] {0x01});
  }

  @Test
  void constructor_null() {
    assertThatThrownBy(() -> new ConstantCodec(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_empty() {
    assertThatThrownBy(() -> new ConstantCodec(new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
