package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VariableLengthCodecsTest {

  @Test
  void ofByteLength_returns_builder() {
    assertThat(VariableLengthCodecs.ofByteLength(NumberCodecs.ofUnsignedByte())).isNotNull();
  }

  @Test
  void ofItemLength_returns_builder() {
    assertThat(VariableLengthCodecs.ofItemLength(NumberCodecs.ofUnsignedByte())).isNotNull();
  }
}
