package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NumberCodecsTest {
  private final Codec<String> stringCodec = StringCodecs.ofCodePoint(10).build();

  @Test
  void ofInt_returns_codec() {
    assertThat(NumberCodecs.ofInt()).isNotNull();
  }

  @Test
  void ofInt_string_returns_codec() {
    assertThat(NumberCodecs.ofInt(stringCodec)).isNotNull();
  }

  @Test
  void ofInt_string_radix_returns_codec() {
    assertThat(NumberCodecs.ofInt(stringCodec, 16)).isNotNull();
  }

  @Test
  void ofLong_returns_codec() {
    assertThat(NumberCodecs.ofLong()).isNotNull();
  }

  @Test
  void ofLong_string_returns_codec() {
    assertThat(NumberCodecs.ofLong(stringCodec)).isNotNull();
  }

  @Test
  void ofLong_string_radix_returns_codec() {
    assertThat(NumberCodecs.ofLong(stringCodec, 16)).isNotNull();
  }

  @Test
  void ofShort_returns_codec() {
    assertThat(NumberCodecs.ofShort()).isNotNull();
  }

  @Test
  void ofShort_string_returns_codec() {
    assertThat(NumberCodecs.ofShort(stringCodec)).isNotNull();
  }

  @Test
  void ofShort_string_radix_returns_codec() {
    assertThat(NumberCodecs.ofShort(stringCodec, 16)).isNotNull();
  }

  @Test
  void ofDouble_returns_codec() {
    assertThat(NumberCodecs.ofDouble()).isNotNull();
  }

  @Test
  void ofDouble_string_returns_codec() {
    assertThat(NumberCodecs.ofDouble(stringCodec)).isNotNull();
  }

  @Test
  void ofFloat_returns_codec() {
    assertThat(NumberCodecs.ofFloat()).isNotNull();
  }

  @Test
  void ofFloat_string_returns_codec() {
    assertThat(NumberCodecs.ofFloat(stringCodec)).isNotNull();
  }

  @Test
  void ofUnsignedByte_returns_codec() {
    assertThat(NumberCodecs.ofUnsignedByte()).isNotNull();
  }

  @Test
  void ofUnsignedShort_returns_codec() {
    assertThat(NumberCodecs.ofUnsignedShort()).isNotNull();
  }

  @Test
  void ofUnsignedInt_returns_codec() {
    assertThat(NumberCodecs.ofUnsignedInt()).isNotNull();
  }

  @Test
  void ofBigInt_string_returns_codec() {
    assertThat(NumberCodecs.ofBigInt(stringCodec)).isNotNull();
  }

  @Test
  void ofBigInt_string_radix_returns_codec() {
    assertThat(NumberCodecs.ofBigInt(stringCodec, 16)).isNotNull();
  }

  @Test
  void ofBigDecimal_string_returns_codec() {
    assertThat(NumberCodecs.ofBigDecimal(stringCodec)).isNotNull();
  }
}
