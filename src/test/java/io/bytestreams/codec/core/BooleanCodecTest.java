package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class BooleanCodecTest {
  private final BooleanCodec codec = new BooleanCodec();

  @Test
  void encode_true() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(true, output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x01});
  }

  @Test
  void encode_false() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(false, output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {0x00});
  }

  @Test
  void decode_true() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01});
    assertThat(codec.decode(input)).isTrue();
  }

  @Test
  void decode_false() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x00});
    assertThat(codec.decode(input)).isFalse();
  }

  @Test
  void decode_invalid_value() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x02});
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("invalid boolean value: 0x02, expected 0x00 or 0x01");
  }

  @Test
  void decode_empty_stream() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_consumes_only_one_byte() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03});
    assertThat(codec.decode(input)).isTrue();
    assertThat(input.available()).isEqualTo(2);
  }
}
