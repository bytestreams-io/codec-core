package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ListCodecsTest {
  private final Codec<Integer> itemCodec = NumberCodecs.ofUnsignedShort();

  @Test
  void of_stream_returns_codec() {
    assertThat(ListCodecs.of(itemCodec)).isNotNull();
  }

  @Test
  void of_fixed_returns_codec() {
    assertThat(ListCodecs.of(itemCodec, 3)).isNotNull();
  }
}
