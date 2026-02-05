package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class UnsignedByteCodecTest {
  private static final int UNSIGNED_BYTE_MAX = 0xFF;
  private final UnsignedByteCodec codec = new UnsignedByteCodec();

  @Test
  void encode(@Randomize(intMin = 0, intMax = UNSIGNED_BYTE_MAX + 1) int value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray()).isEqualTo(new byte[] {(byte) value});
  }

  @Test
  void encode_overflow(@Randomize(intMin = 1) int value) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode(UNSIGNED_BYTE_MAX + value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "value must be between 0 and %d, but was [%d]",
            UNSIGNED_BYTE_MAX, UNSIGNED_BYTE_MAX + value);
  }

  @Test
  void encode_negative_value(@Randomize(intMin = 1) int value) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode(-value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must be between 0 and %d, but was [%d]", UNSIGNED_BYTE_MAX, -value);
  }

  @Test
  void decode(@Randomize byte[] value) throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(value);
    assertThat(codec.decode(input)).isEqualTo(value[0] & UNSIGNED_BYTE_MAX);
    assertThat(input.available()).isPositive();
  }

  @Test
  void decode_empty_stream() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }
}
