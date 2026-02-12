package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class LongCodecTest {
  private final LongCodec codec = new LongCodec();

  @Test
  void getLength() {
    assertThat(codec.getLength()).isEqualTo(Long.BYTES);
  }

  @Test
  void encode(@Randomize long value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toByteArray())
        .isEqualTo(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }

  @Test
  void decode(@Randomize(length = 10) byte[] value) throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(value);
    assertThat(codec.decode(input)).isEqualTo(ByteBuffer.wrap(value, 0, Long.BYTES).getLong());
    assertThat(input.available()).isEqualTo(value.length - Long.BYTES);
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
