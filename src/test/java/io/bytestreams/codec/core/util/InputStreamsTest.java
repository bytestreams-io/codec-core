package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class InputStreamsTest {

  @Test
  void readFully() throws IOException {
    byte[] data = {1, 2, 3, 4, 5};
    ByteArrayInputStream input = new ByteArrayInputStream(data);

    byte[] result = InputStreams.readFully(input, 5);

    assertThat(result).isEqualTo(data);
  }

  @Test
  void readFully_partial_data() throws IOException {
    byte[] data = {1, 2, 3, 4, 5};
    ByteArrayInputStream input = new ByteArrayInputStream(data);

    byte[] result = InputStreams.readFully(input, 3);

    assertThat(result).isEqualTo(new byte[] {1, 2, 3});
  }

  @Test
  void readFully_insufficient_data() {
    byte[] data = {1, 2, 3};
    ByteArrayInputStream input = new ByteArrayInputStream(data);

    assertThatThrownBy(() -> InputStreams.readFully(input, 5))
        .isInstanceOf(EOFException.class)
        .hasMessageContaining("3")
        .hasMessageContaining("5");
  }

  @Test
  void readFully_negative_length() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2, 3});

    assertThatThrownBy(() -> InputStreams.readFully(input, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }

  @Test
  void readFully_empty_stream() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> InputStreams.readFully(input, 5)).isInstanceOf(EOFException.class);
  }
}
