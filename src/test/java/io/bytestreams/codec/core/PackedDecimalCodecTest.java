package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Packed decimal — COBOL {@code COMP-3}: digits in nibbles, with a sign in the last nibble. */
class PackedDecimalCodecTest {

  private static byte[] encode(Codec<Long> codec, long value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    return output.toByteArray();
  }

  @Test
  void encodes_a_negative_value_with_a_d_sign() throws IOException {
    assertThat(encode(Codecs.packedLong(5), -12345L)).containsExactly(0x12, 0x34, 0x5D);
  }

  @Test
  void encodes_a_positive_value_with_a_c_sign() throws IOException {
    assertThat(encode(Codecs.packedLong(5), 12345L)).containsExactly(0x12, 0x34, 0x5C);
  }

  @Test
  void decodes_both_signs() throws IOException {
    assertThat(Codecs.packedLong(5).decode(new ByteArrayInputStream(new byte[] {0x12, 0x34, 0x5D})))
        .isEqualTo(-12345L);
    assertThat(Codecs.packedLong(5).decode(new ByteArrayInputStream(new byte[] {0x12, 0x34, 0x5C})))
        .isEqualTo(12345L);
  }

  @Test
  void decodes_the_alternate_sign_nibbles() throws IOException {
    // A, C, E and F are positive; B and D are negative
    assertThat(Codecs.packedLong(3).decode(new ByteArrayInputStream(new byte[] {0x12, 0x3F})))
        .isEqualTo(123L);
    assertThat(Codecs.packedLong(3).decode(new ByteArrayInputStream(new byte[] {0x12, 0x3A})))
        .isEqualTo(123L);
    assertThat(Codecs.packedLong(3).decode(new ByteArrayInputStream(new byte[] {0x12, 0x3E})))
        .isEqualTo(123L);
    assertThat(Codecs.packedLong(3).decode(new ByteArrayInputStream(new byte[] {0x12, 0x3B})))
        .isEqualTo(-123L);
  }

  @Test
  void an_even_digit_count_leaves_a_leading_zero_nibble() throws IOException {
    assertThat(encode(Codecs.packedLong(4), 1234L)).containsExactly(0x01, 0x23, 0x4C);
  }

  @Test
  void an_even_digit_count_round_trips() throws IOException {
    byte[] encoded = encode(Codecs.packedLong(4), -1234L);

    assertThat(encoded).containsExactly(0x01, 0x23, 0x4D);
    assertThat(Codecs.packedLong(4).decode(new ByteArrayInputStream(encoded))).isEqualTo(-1234L);
  }

  @Test
  void byte_count_allows_for_the_sign_nibble() throws IOException {
    assertThat(encode(Codecs.packedLong(1), 7L)).hasSize(1);
    assertThat(encode(Codecs.packedLong(2), 42L)).hasSize(2);
    assertThat(encode(Codecs.packedLong(3), 123L)).hasSize(2);
    assertThat(encode(Codecs.packedLong(18), 1L)).hasSize(10);
  }

  @Test
  void encode_result_counts_digits_and_bytes() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = Codecs.packedLong(5).encode(-12345L, output);

    assertThat(result.count()).isEqualTo(5);
    assertThat(result.bytes()).isEqualTo(3);
  }

  @Test
  void zero_round_trips() throws IOException {
    byte[] encoded = encode(Codecs.packedLong(3), 0L);

    assertThat(encoded).containsExactly(0x00, 0x0C);
    assertThat(Codecs.packedLong(3).decode(new ByteArrayInputStream(encoded))).isZero();
  }

  @Test
  void rejects_a_value_that_does_not_fit() {
    Codec<Long> codec = Codecs.packedLong(3);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(1000L, output))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> codec.encode(-1000L, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_a_non_decimal_digit_nibble() {
    Codec<Long> codec = Codecs.packedLong(3);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x1A, (byte) 0x3C});

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(CodecException.class);
  }

  @Test
  void rejects_a_missing_sign_nibble() {
    Codec<Long> codec = Codecs.packedLong(3);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x12, 0x30});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("sign");
  }

  @Test
  void rejects_a_value_wider_than_the_field_declares() {
    // an even digit count leaves an unused leading nibble; a non-zero one means corrupt data
    Codec<Long> codec = Codecs.packedLong(4);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {(byte) 0x91, 0x23, 0x4C});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("more than 4 digits");
  }

  @Test
  void rejects_an_eighteen_digit_field_that_overflows_long() {
    Codec<Long> codec = Codecs.packedLong(18);
    byte[] nines = new byte[10];
    Arrays.fill(nines, 0, 9, (byte) 0x99);
    nines[9] = (byte) 0x9C;
    ByteArrayInputStream input = new ByteArrayInputStream(nines);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(CodecException.class);
  }

  @Test
  void constructor_rejects_more_digits_than_a_long_holds() {
    assertThatThrownBy(() -> new PackedDecimalCodec(19))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at most 18");
  }

  @Test
  void packed_int_variant_round_trips() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Codecs.packedInt(5).encode(-12345, output);

    assertThat(output.toByteArray()).containsExactly(0x12, 0x34, 0x5D);
    assertThat(Codecs.packedInt(5).decode(new ByteArrayInputStream(output.toByteArray())))
        .isEqualTo(-12345);
  }

  @Test
  void rejects_an_out_of_range_digit_count() {
    assertThatThrownBy(() -> Codecs.packedInt(10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Codecs.packedInt(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Codecs.packedLong(19)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Codecs.packedLong(0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void constructor_requires_a_positive_digit_count() {
    assertThatThrownBy(() -> new PackedDecimalCodec(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("digits must be positive");
  }
}
