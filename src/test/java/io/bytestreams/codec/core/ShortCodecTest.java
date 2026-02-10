package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ShortCodecTest {
  private final ShortCodec codec = new ShortCodec();

  @Test
  void getLength() {
    assertThat(codec.getLength()).isEqualTo(Short.BYTES);
  }

  @Test
  void encode() throws IOException {
    short value = 12345;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray())
        .isEqualTo(ByteBuffer.allocate(Short.BYTES).putShort(value).array());
  }

  @Test
  void encode_negative() throws IOException {
    short value = -12345;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray())
        .isEqualTo(ByteBuffer.allocate(Short.BYTES).putShort(value).array());
  }

  @Test
  void decode() throws IOException {
    short expected = 12345;
    byte[] bytes = ByteBuffer.allocate(Short.BYTES).putShort(expected).array();
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void decode_negative() throws IOException {
    short expected = -12345;
    byte[] bytes = ByteBuffer.allocate(Short.BYTES).putShort(expected).array();
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void decode_only_reads_required_bytes() throws IOException {
    byte[] bytes = {0x00, 0x01, 0x02, 0x03};
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    codec.decode(input);
    assertThat(input.available()).isEqualTo(2);
  }

  @Test
  void decode_empty_stream() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_insufficient_data() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01});
    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }
}
