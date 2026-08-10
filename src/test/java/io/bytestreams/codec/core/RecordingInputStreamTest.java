package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class RecordingInputStreamTest {

  @Test
  void read_single_byte_records() throws IOException {
    RecordingInputStream recording =
        new RecordingInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    assertThat(recording.read()).isEqualTo(1);
    assertThat(recording.read()).isEqualTo(2);
    assertThat(recording.read()).isEqualTo(3);
    assertThat(recording.read()).isEqualTo(-1);
    assertThat(recording.recordedBytes()).containsExactly(1, 2, 3);
  }

  @Test
  void read_bulk_records() throws IOException {
    RecordingInputStream recording =
        new RecordingInputStream(new ByteArrayInputStream(new byte[] {10, 20, 30, 40, 50}));

    byte[] buf = new byte[3];
    int n = recording.read(buf, 0, 3);

    assertThat(n).isEqualTo(3);
    assertThat(buf).containsExactly(10, 20, 30);
    assertThat(recording.recordedBytes()).containsExactly(10, 20, 30);
  }

  @Test
  void read_bulk_at_eof_records_nothing() throws IOException {
    RecordingInputStream recording =
        new RecordingInputStream(new ByteArrayInputStream(new byte[0]));

    byte[] buf = new byte[3];
    int n = recording.read(buf, 0, 3);

    assertThat(n).isEqualTo(-1);
    assertThat(recording.recordedBytes()).isEmpty();
  }

  @Test
  void skip_records_skipped_bytes() throws IOException {
    RecordingInputStream recording =
        new RecordingInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5}));

    long skipped = recording.skip(3);

    assertThat(skipped).isEqualTo(3);
    assertThat(recording.recordedBytes()).containsExactly(1, 2, 3);
  }

  @Test
  void skip_past_eof_records_available_bytes() throws IOException {
    RecordingInputStream recording =
        new RecordingInputStream(new ByteArrayInputStream(new byte[] {1, 2}));

    long skipped = recording.skip(10);

    assertThat(skipped).isEqualTo(2);
    assertThat(recording.recordedBytes()).containsExactly(1, 2);
  }

  @Test
  void available_delegates_to_input() throws IOException {
    RecordingInputStream recording =
        new RecordingInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    assertThat(recording.available()).isEqualTo(3);
  }

  @Test
  void close_delegates_to_input() throws IOException {
    boolean[] closed = {false};
    InputStream input =
        new ByteArrayInputStream(new byte[] {1}) {
          @Override
          public void close() throws IOException {
            closed[0] = true;
            super.close();
          }
        };
    RecordingInputStream recording = new RecordingInputStream(input);

    recording.close();

    assertThat(closed[0]).isTrue();
  }

  @Test
  void null_input_rejected() {
    assertThatThrownBy(() -> new RecordingInputStream(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("input");
  }

  @Test
  void mark_support_follows_the_underlying_stream() {
    assertThat(new RecordingInputStream(new ByteArrayInputStream(new byte[] {1})).markSupported())
        .isTrue();
    assertThat(new RecordingInputStream(new MarkNotSupported()).markSupported()).isFalse();
  }

  @Test
  void reset_drops_the_bytes_recorded_since_the_mark() throws IOException {
    RecordingInputStream recording =
        new RecordingInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));

    assertThat(recording.read()).isEqualTo(1);
    recording.mark(16);
    assertThat(recording.read()).isEqualTo(2);
    assertThat(recording.read()).isEqualTo(3);
    recording.reset();

    assertThat(recording.recordedBytes()).containsExactly(1);
    assertThat(recording.read()).isEqualTo(2);
    assertThat(recording.recordedBytes()).containsExactly(1, 2);
  }

  @Test
  void reset_without_a_mark_follows_the_underlying_stream_back() throws IOException {
    // ByteArrayInputStream rewinds to the start, so the recording must too, or the bytes
    // would be recorded a second time on the way through
    RecordingInputStream recording =
        new RecordingInputStream(new ByteArrayInputStream(new byte[] {1, 2}));

    assertThat(recording.read()).isEqualTo(1);
    recording.reset();

    assertThat(recording.recordedBytes()).isEmpty();
    assertThat(recording.read()).isEqualTo(1);
    assertThat(recording.recordedBytes()).containsExactly(1);
  }

  @Test
  void records_more_than_the_initial_capacity() throws IOException {
    byte[] data = new byte[500];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }
    RecordingInputStream recording = new RecordingInputStream(new ByteArrayInputStream(data));

    assertThat(recording.readAllBytes()).hasSize(500);
    assertThat(recording.recordedBytes()).containsExactly(data);
  }

  /** Stands in for a stream that refuses mark, such as a socket. */
  static class MarkNotSupported extends FilterInputStream {
    MarkNotSupported() {
      super(new ByteArrayInputStream(new byte[] {1}));
    }

    @Override
    public boolean markSupported() {
      return false;
    }
  }
}
