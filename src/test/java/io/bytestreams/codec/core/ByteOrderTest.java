package io.bytestreams.codec.core;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ByteOrderTest {

  private static byte[] encode(Codec<?> codec, Object value) throws IOException {
    @SuppressWarnings("unchecked")
    Codec<Object> c = (Codec<Object>) codec;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    c.encode(value, output);
    return output.toByteArray();
  }

  @Test
  void uint16_little_endian_reverses_the_bytes() throws IOException {
    assertThat(encode(Codecs.uint16(LITTLE_ENDIAN), 0x0102)).containsExactly(0x02, 0x01);
    assertThat(encode(Codecs.uint16(), 0x0102)).containsExactly(0x01, 0x02);
  }

  @Test
  void uint32_little_endian_reverses_the_bytes() throws IOException {
    assertThat(encode(Codecs.uint32(LITTLE_ENDIAN), 0x01020304L))
        .containsExactly(0x04, 0x03, 0x02, 0x01);
  }

  @Test
  void int64_little_endian_reverses_the_bytes() throws IOException {
    assertThat(encode(Codecs.int64(LITTLE_ENDIAN), 0x0102030405060708L))
        .containsExactly(0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01);
  }

  @Test
  void little_endian_decodes_what_it_encoded() throws IOException {
    assertThat(Codecs.uint16(LITTLE_ENDIAN).decode(new ByteArrayInputStream(new byte[] {2, 1})))
        .isEqualTo(0x0102);
    assertThat(Codecs.int16(LITTLE_ENDIAN).decode(new ByteArrayInputStream(new byte[] {2, 1})))
        .isEqualTo((short) 0x0102);
    assertThat(
            Codecs.int32(LITTLE_ENDIAN).decode(new ByteArrayInputStream(new byte[] {4, 3, 2, 1})))
        .isEqualTo(0x01020304);
  }

  @Test
  void floating_point_honours_byte_order() throws IOException {
    byte[] be = encode(Codecs.float32(BIG_ENDIAN), 1.5f);
    byte[] le = encode(Codecs.float32(LITTLE_ENDIAN), 1.5f);

    assertThat(le).containsExactly(be[3], be[2], be[1], be[0]);
    assertThat(Codecs.float32(LITTLE_ENDIAN).decode(new ByteArrayInputStream(le))).isEqualTo(1.5f);
    assertThat(
            Codecs.float64(LITTLE_ENDIAN)
                .decode(new ByteArrayInputStream(encode(Codecs.float64(LITTLE_ENDIAN), 2.5d))))
        .isEqualTo(2.5d);
  }

  @Test
  void negative_values_round_trip_little_endian() throws IOException {
    assertThat(encode(Codecs.int16(LITTLE_ENDIAN), (short) -2)).containsExactly(0xFE, 0xFF);
    assertThat(encode(Codecs.int64(LITTLE_ENDIAN), Long.MIN_VALUE))
        .containsExactly(0, 0, 0, 0, 0, 0, 0, 0x80);
    assertThat(
            Codecs.int32(LITTLE_ENDIAN)
                .decode(
                    new ByteArrayInputStream(
                        encode(Codecs.int32(LITTLE_ENDIAN), Integer.MIN_VALUE))))
        .isEqualTo(Integer.MIN_VALUE);
  }

  @Test
  void byte_order_composes_with_a_length_prefix() throws IOException {
    Codec<String> codec = Codecs.prefixed(Codecs.uint16(LITTLE_ENDIAN), Codecs.ascii());

    byte[] framed = encode(codec, "hello");

    assertThat(framed).startsWith((byte) 0x05, (byte) 0x00);
    assertThat(codec.decode(new ByteArrayInputStream(framed))).isEqualTo("hello");
  }

  @Test
  void big_endian_overload_matches_the_default() throws IOException {
    assertThat(encode(Codecs.uint32(BIG_ENDIAN), 0x01020304L))
        .containsExactly(encode(Codecs.uint32(), 0x01020304L));
  }

  @Test
  void byte_count_is_unchanged_by_order() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = Codecs.uint32(LITTLE_ENDIAN).encode(1L, output);

    assertThat(result.bytes()).isEqualTo(4);
    assertThat(result.count()).isEqualTo(4);
  }

  @Test
  void unsigned_range_is_still_enforced() {
    Codec<Integer> codec = Codecs.uint16(LITTLE_ENDIAN);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0x10000, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void order_must_not_be_null() {
    assertThatThrownBy(() -> Codecs.uint16(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("order");
  }
}
