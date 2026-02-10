package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ObjectCodecTest {

  @Test
  void ordered_returns_builder() {
    assertThat(ObjectCodec.ordered()).isNotNull();
  }

  @Test
  void tagged_returns_builder() {
    assertThat(ObjectCodec.tagged()).isNotNull();
  }
}
