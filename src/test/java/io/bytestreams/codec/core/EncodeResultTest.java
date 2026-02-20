package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EncodeResultTest {

  @Test
  void negative_count() {
    assertThatThrownBy(() -> new EncodeResult(-1, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }

  @Test
  void negative_bytes() {
    assertThatThrownBy(() -> new EncodeResult(0, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }

  @Test
  void empty() {
    assertThat(EncodeResult.EMPTY.count()).isZero();
    assertThat(EncodeResult.EMPTY.bytes()).isZero();
  }

  @Test
  void ofBytes() {
    EncodeResult result = EncodeResult.ofBytes(5);
    assertThat(result.count()).isEqualTo(5);
    assertThat(result.bytes()).isEqualTo(5);
  }
}
