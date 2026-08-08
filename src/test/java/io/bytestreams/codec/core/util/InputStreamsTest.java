package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
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

  @Test
  void markable_returns_a_markable_stream_unchanged() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2});

    assertThat(InputStreams.markable(input)).isSameAs(input);
  }

  @Test
  void markable_wraps_a_stream_that_refuses_mark() {
    MarkNotSupportedInputStream input = new MarkNotSupportedInputStream(new byte[] {1, 2});

    InputStream result = InputStreams.markable(input);

    assertThat(result).isNotSameAs(input);
    assertThat(result.markSupported()).isTrue();
  }

  @Test
  void markable_rejects_null() {
    assertThatThrownBy(() -> InputStreams.markable(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("input");
  }

  @Test
  void atEndOfStream_consumes_nothing_when_bytes_remain() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {7, 8});

    assertThat(InputStreams.atEndOfStream(input)).isFalse();
    assertThat(input.readAllBytes()).containsExactly(7, 8);
  }

  @Test
  void atEndOfStream_reports_an_exhausted_stream() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThat(InputStreams.atEndOfStream(input)).isTrue();
  }
}
