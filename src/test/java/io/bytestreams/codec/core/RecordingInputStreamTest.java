package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
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
}
