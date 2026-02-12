package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
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
class UnsignedIntegerCodecTest {
  private static final long UNSIGNED_INT_MAX = 0xFFFFFFFFL;
  private final UnsignedIntegerCodec codec = new UnsignedIntegerCodec();

  @Test
  void getLength() {
    assertThat(codec.getLength()).isEqualTo(Integer.BYTES);
  }

  @Test
  void encode(@Randomize(longMin = 0, longMax = UNSIGNED_INT_MAX + 1) long value)
      throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray())
        .isEqualTo(
            new byte[] {
              (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value
            });
  }

  @Test
  void encode_overflow(@Randomize(longMin = 1) long value) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode(UNSIGNED_INT_MAX + value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "value must be between 0 and %d, but was [%d]",
            UNSIGNED_INT_MAX, UNSIGNED_INT_MAX + value);
  }

  @Test
  void encode_negative_value(@Randomize(longMin = 1) long value) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode(-value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must be between 0 and %d, but was [%d]", UNSIGNED_INT_MAX, -value);
  }

  @Test
  void decode(@Randomize byte[] value) throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(value);
    assertThat(codec.decode(input))
        .isEqualTo(
            (long) (value[0] & 0xFF) << 24
                | (long) (value[1] & 0xFF) << 16
                | (long) (value[2] & 0xFF) << 8
                | (long) (value[3] & 0xFF));
    assertThat(input.available()).isEqualTo(value.length - 4);
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
