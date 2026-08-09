package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Zoned decimal — COBOL {@code DISPLAY}: one digit per byte, sign in the last byte's zone. */
class ZonedDecimalCodecTest {

  private static byte[] encode(Codec<Long> codec, long value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    return output.toByteArray();
  }

  private static ByteArrayInputStream bytes(int... values) {
    byte[] data = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      data[i] = (byte) values[i];
    }
    return new ByteArrayInputStream(data);
  }

  @Test
  void encodes_a_negative_value_with_a_d_zone() throws IOException {
    assertThat(encode(Codecs.zonedLong(3), -123L)).containsExactly(0xF1, 0xF2, 0xD3);
  }

  @Test
  void encodes_a_positive_value_with_a_c_zone() throws IOException {
    assertThat(encode(Codecs.zonedLong(3), 123L)).containsExactly(0xF1, 0xF2, 0xC3);
  }

  @Test
  void decodes_both_signs() throws IOException {
    assertThat(Codecs.zonedLong(3).decode(bytes(0xF1, 0xF2, 0xD3))).isEqualTo(-123L);
    assertThat(Codecs.zonedLong(3).decode(bytes(0xF1, 0xF2, 0xC3))).isEqualTo(123L);
  }

  @Test
  void decodes_an_unsigned_field() throws IOException {
    assertThat(Codecs.zonedLong(3).decode(bytes(0xF1, 0xF2, 0xF3))).isEqualTo(123L);
  }

  @Test
  void decodes_the_alternate_sign_zones() throws IOException {
    assertThat(Codecs.zonedLong(2).decode(bytes(0xF1, 0xA2))).isEqualTo(12L);
    assertThat(Codecs.zonedLong(2).decode(bytes(0xF1, 0xE2))).isEqualTo(12L);
    assertThat(Codecs.zonedLong(2).decode(bytes(0xF1, 0xB2))).isEqualTo(-12L);
  }

  @Test
  void uses_one_byte_per_digit() throws IOException {
    assertThat(encode(Codecs.zonedLong(1), 7L)).hasSize(1);
    assertThat(encode(Codecs.zonedLong(6), 1L)).hasSize(6);
    assertThat(encode(Codecs.zonedLong(18), 1L)).hasSize(18);
  }

  @Test
  void pads_with_leading_zero_digits() throws IOException {
    assertThat(encode(Codecs.zonedLong(4), -7L)).containsExactly(0xF0, 0xF0, 0xF0, 0xD7);
  }

  @Test
  void encode_result_counts_digits_and_bytes() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = Codecs.zonedLong(5).encode(-12345L, output);

    assertThat(result.count()).isEqualTo(5);
    assertThat(result.bytes()).isEqualTo(5);
  }

  @Test
  void zero_round_trips() throws IOException {
    byte[] encoded = encode(Codecs.zonedLong(2), 0L);

    assertThat(encoded).containsExactly(0xF0, 0xC0);
    assertThat(Codecs.zonedLong(2).decode(new ByteArrayInputStream(encoded))).isZero();
  }

  @Test
  void a_single_digit_field_carries_only_the_sign_zone() throws IOException {
    // with one byte there are no leading bytes, so the digit-zone check never applies
    assertThat(Codecs.zonedLong(1).decode(bytes(0xD7))).isEqualTo(-7L);
    assertThat(Codecs.zonedLong(1).decode(bytes(0xC7))).isEqualTo(7L);
    assertThat(Codecs.zonedLong(1).decode(bytes(0xF7))).isEqualTo(7L);
  }

  @Test
  void rejects_a_blank_filled_field() {
    // EBCDIC space is 0x40: a plausible filler, and not a number
    Codec<Long> codec = Codecs.zonedLong(3);
    ByteArrayInputStream input = bytes(0x40, 0x40, 0x40);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("zone");
  }

  @Test
  void rejects_a_value_that_does_not_fit() {
    Codec<Long> codec = Codecs.zonedLong(3);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(1000L, output))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> codec.encode(-1000L, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_a_non_decimal_digit() {
    Codec<Long> codec = Codecs.zonedLong(2);
    ByteArrayInputStream input = bytes(0xFA, 0xC2);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("digit");
  }

  @Test
  void rejects_a_wrong_zone_on_a_leading_byte() {
    // 0x31 is ASCII '1'; reading it as EBCDIC zoned would otherwise pass unnoticed
    Codec<Long> codec = Codecs.zonedLong(2);
    ByteArrayInputStream input = bytes(0x31, 0xC2);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("zone");
  }

  @Test
  void rejects_an_invalid_sign_zone() {
    Codec<Long> codec = Codecs.zonedLong(2);
    ByteArrayInputStream input = bytes(0xF1, 0x32);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("sign");
  }

  @Test
  void zoned_int_variant_round_trips() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Codecs.zonedInt(3).encode(-123, output);

    assertThat(output.toByteArray()).containsExactly(0xF1, 0xF2, 0xD3);
    assertThat(Codecs.zonedInt(3).decode(new ByteArrayInputStream(output.toByteArray())))
        .isEqualTo(-123);
  }

  @Test
  void constructor_requires_a_positive_digit_count() {
    assertThatThrownBy(() -> new ZonedDecimalCodec(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("digits must be positive");
  }

  @Test
  void constructor_rejects_more_digits_than_a_long_holds() {
    assertThatThrownBy(() -> new ZonedDecimalCodec(19))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at most 18");
  }

  @Test
  void rejects_an_out_of_range_digit_count() {
    assertThatThrownBy(() -> Codecs.zonedInt(10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Codecs.zonedInt(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Codecs.zonedLong(19)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Codecs.zonedLong(0)).isInstanceOf(IllegalArgumentException.class);
  }
}
