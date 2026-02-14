package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringCodecsTest {

  @Test
  void ofCodePoint_fixed_returns_builder() {
    assertThat(StringCodecs.ofCodePoint(5)).isNotNull();
  }

  @Test
  void ofCodePoint_stream_returns_builder() {
    assertThat(StringCodecs.ofCodePoint()).isNotNull();
  }

  @Test
  void ofHex_fixed_returns_builder() {
    assertThat(StringCodecs.ofHex(4)).isNotNull();
  }

  @Test
  void ofHex_stream_returns_builder() {
    assertThat(StringCodecs.ofHex()).isNotNull();
  }

  @Test
  void ofFormatted_returns_builder() {
    assertThat(StringCodecs.ofFormatted(StringCodecs.ofCodePoint(5).build())).isNotNull();
  }
}
