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

  @Test
  void ascii_fixed() {
    FixedCodePointStringCodec codec = StringCodecs.ascii(5);
    assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
    assertThat(codec.getLength()).isEqualTo(5);
  }

  @Test
  void ascii_stream() {
    assertThat(StringCodecs.ascii()).isInstanceOf(StreamCodePointStringCodec.class);
  }

  @Test
  void utf8_fixed() {
    FixedCodePointStringCodec codec = StringCodecs.utf8(5);
    assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
    assertThat(codec.getLength()).isEqualTo(5);
  }

  @Test
  void utf8_stream() {
    assertThat(StringCodecs.utf8()).isInstanceOf(StreamCodePointStringCodec.class);
  }

  @Test
  void latin1_fixed() {
    FixedCodePointStringCodec codec = StringCodecs.latin1(5);
    assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
    assertThat(codec.getLength()).isEqualTo(5);
  }

  @Test
  void latin1_stream() {
    assertThat(StringCodecs.latin1()).isInstanceOf(StreamCodePointStringCodec.class);
  }

  @Test
  void ebcdic_fixed() {
    FixedCodePointStringCodec codec = StringCodecs.ebcdic(5);
    assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
    assertThat(codec.getLength()).isEqualTo(5);
  }

  @Test
  void ebcdic_stream() {
    assertThat(StringCodecs.ebcdic()).isInstanceOf(StreamCodePointStringCodec.class);
  }
}
