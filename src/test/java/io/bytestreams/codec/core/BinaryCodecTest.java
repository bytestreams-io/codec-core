package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class BinaryCodecTest {

  @Test
  void getLength(@Randomize(intMin = 1, intMax = 100) int length) {
    BinaryCodec codec = new BinaryCodec(length);
    assertThat(codec.getLength()).isEqualTo(length);
  }

  @Test
  void encode(@Randomize byte[] value) throws IOException {
    BinaryCodec decoder = new BinaryCodec(value.length);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    decoder.encode(value, output);
    assertThat(output.toByteArray()).isEqualTo(value);
  }

  @Test
  void encode_insufficient_data(@Randomize byte[] value) {
    BinaryCodec decoder = new BinaryCodec(value.length + 1);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> decoder.encode(value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must be of length %d, but was [%d]", value.length + 1, value.length);
  }

  @Test
  void encode_oversize_data(@Randomize byte[] value) {
    BinaryCodec decoder = new BinaryCodec(value.length - 1);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> decoder.encode(value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must be of length %d, but was [%d]", value.length - 1, value.length);
  }

  @Test
  void decode(@Randomize byte[] value) throws IOException {
    BinaryCodec decoder = new BinaryCodec(value.length - 1);
    ByteArrayInputStream input = new ByteArrayInputStream(value);
    assertThat(decoder.decode(input)).isEqualTo(Arrays.copyOfRange(value, 0, value.length - 1));
    assertThat(input.available()).isPositive();
  }

  @Test
  void decode_insufficient_data(@Randomize byte[] value) {
    BinaryCodec decoder = new BinaryCodec(value.length + 1);
    ByteArrayInputStream input = new ByteArrayInputStream(value);
    assertThatThrownBy(() -> decoder.decode(input))
        .isInstanceOf(EOFException.class)
        .hasMessage(
            "End of stream reached after reading %d bytes, bytes expected [%d]",
            value.length, value.length + 1);
  }

  @Test
  void constructor_negative_length() {
    assertThatThrownBy(() -> new BinaryCodec(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }

  @Test
  void encode_zero_length() throws IOException {
    BinaryCodec codec = new BinaryCodec(0);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(new byte[0], output);
    assertThat(output.toByteArray()).isEmpty();
  }

  @Test
  void decode_zero_length() throws IOException {
    BinaryCodec codec = new BinaryCodec(0);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2, 3});
    assertThat(codec.decode(input)).isEmpty();
    assertThat(input.available()).isEqualTo(3);
  }
}
