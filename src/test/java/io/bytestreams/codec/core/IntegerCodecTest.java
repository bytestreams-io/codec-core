package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class IntegerCodecTest {
  private final IntegerCodec codec = new IntegerCodec();

  @Test
  void getLength() {
    assertThat(codec.getLength()).isEqualTo(Integer.BYTES);
  }

  @Test
  void encode() throws IOException {
    int value = 123456789;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray())
        .isEqualTo(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
  }

  @Test
  void encode_negative() throws IOException {
    int value = -123456789;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray())
        .isEqualTo(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
  }

  @Test
  void decode() throws IOException {
    int expected = 123456789;
    byte[] bytes = ByteBuffer.allocate(Integer.BYTES).putInt(expected).array();
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void decode_negative() throws IOException {
    int expected = -123456789;
    byte[] bytes = ByteBuffer.allocate(Integer.BYTES).putInt(expected).array();
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void decode_only_reads_required_bytes() throws IOException {
    byte[] bytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
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
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03});
    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }
}
