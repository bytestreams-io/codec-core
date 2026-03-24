package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RecordedTest {

  @Test
  void equals_compares_value_and_raw_bytes() {
    Recorded<String> a = new Recorded<>("hello", new byte[] {1, 2});
    Recorded<String> b = new Recorded<>("hello", new byte[] {1, 2});
    Recorded<String> c = new Recorded<>("hello", new byte[] {3});
    Recorded<String> d = new Recorded<>("world", new byte[] {1, 2});

    assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo(d).isNotEqualTo("not a Recorded");
  }

  @Test
  void hashCode_considers_raw_bytes() {
    Recorded<String> a = new Recorded<>("hello", new byte[] {1, 2});
    Recorded<String> b = new Recorded<>("hello", new byte[] {1, 2});

    assertThat(a).hasSameHashCodeAs(b);
  }

  @Test
  void toString_shows_byte_count_not_content() {
    Recorded<String> recorded = new Recorded<>("hello", new byte[] {1, 2, 3});

    assertThat(recorded.toString())
        .contains("hello")
        .contains("3 bytes")
        .doesNotContain("01")
        .doesNotContain("02");
  }

  @Test
  void null_value_rejected() {
    assertThatThrownBy(() -> new Recorded<>(null, new byte[0]))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }

  @Test
  void null_raw_bytes_rejected() {
    assertThatThrownBy(() -> new Recorded<>("hello", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("rawBytes");
  }
}
