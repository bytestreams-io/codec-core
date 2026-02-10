package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class LongCodecTest {
  private final LongCodec codec = new LongCodec();

  @Test
  void getLength() {
    assertThat(codec.getLength()).isEqualTo(Long.BYTES);
  }

  @Test
  void encode() throws IOException {
    long value = 1234567890123456789L;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray())
        .isEqualTo(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }

  @Test
  void encode_negative() throws IOException {
    long value = -1234567890123456789L;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray())
        .isEqualTo(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }

  @Test
  void decode() throws IOException {
    long expected = 1234567890123456789L;
    byte[] bytes = ByteBuffer.allocate(Long.BYTES).putLong(expected).array();
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void decode_negative() throws IOException {
    long expected = -1234567890123456789L;
    byte[] bytes = ByteBuffer.allocate(Long.BYTES).putLong(expected).array();
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void decode_only_reads_required_bytes() throws IOException {
    byte[] bytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
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
