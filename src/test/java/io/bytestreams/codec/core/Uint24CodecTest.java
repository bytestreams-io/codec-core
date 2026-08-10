package io.bytestreams.codec.core;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Three-byte unsigned integers — TLS record lengths, MPEG-TS, RTP, ISO 8583 lengths. */
class Uint24CodecTest {

  private static byte[] encode(Codec<Integer> codec, int value) throws IOException {
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
  void encodes_big_endian_by_default() throws IOException {
    assertThat(encode(Codecs.uint24(), 0x123456)).containsExactly(0x12, 0x34, 0x56);
  }

  @Test
  void encodes_little_endian_reversed() throws IOException {
    assertThat(encode(Codecs.uint24(LITTLE_ENDIAN), 0x123456)).containsExactly(0x56, 0x34, 0x12);
  }

  @Test
  void decodes_big_endian() throws IOException {
    assertThat(Codecs.uint24().decode(bytes(0x12, 0x34, 0x56))).isEqualTo(0x123456);
  }

  @Test
  void decodes_little_endian() throws IOException {
    assertThat(Codecs.uint24(LITTLE_ENDIAN).decode(bytes(0x12, 0x34, 0x56))).isEqualTo(0x563412);
  }

  @Test
  void big_endian_overload_matches_the_default() throws IOException {
    assertThat(encode(Codecs.uint24(BIG_ENDIAN), 0x010203))
        .containsExactly(encode(Codecs.uint24(), 0x010203));
  }

  @Test
  void carries_the_full_unsigned_range() throws IOException {
    assertThat(encode(Codecs.uint24(), 0xFFFFFF)).containsExactly(0xFF, 0xFF, 0xFF);
    assertThat(Codecs.uint24().decode(bytes(0xFF, 0xFF, 0xFF))).isEqualTo(0xFFFFFF);
    assertThat(Codecs.uint24(LITTLE_ENDIAN).decode(bytes(0xFF, 0xFF, 0xFF))).isEqualTo(0xFFFFFF);
  }

  @Test
  void the_high_byte_is_not_sign_extended() throws IOException {
    // 0x800000 has the top bit of the top byte set
    assertThat(Codecs.uint24().decode(bytes(0x80, 0x00, 0x00))).isEqualTo(0x800000);
    assertThat(Codecs.uint24(LITTLE_ENDIAN).decode(bytes(0x00, 0x00, 0x80))).isEqualTo(0x800000);
    assertThat(encode(Codecs.uint24(), 0x800000)).containsExactly(0x80, 0x00, 0x00);
    assertThat(encode(Codecs.uint24(LITTLE_ENDIAN), 0x800000)).containsExactly(0x00, 0x00, 0x80);
  }

  @Test
  void zero_round_trips() throws IOException {
    assertThat(encode(Codecs.uint24(), 0)).containsExactly(0x00, 0x00, 0x00);
    assertThat(Codecs.uint24().decode(bytes(0x00, 0x00, 0x00))).isZero();
  }

  @Test
  void encode_result_reports_three_bytes() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = Codecs.uint24().encode(1, output);

    assertThat(result.count()).isEqualTo(3);
    assertThat(result.bytes()).isEqualTo(3);
  }

  @Test
  void rejects_a_value_outside_the_range() {
    Codec<Integer> codec = Codecs.uint24();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0x1000000, output))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> codec.encode(-1, output)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void the_range_check_survives_a_byte_order_change() {
    Codec<Integer> codec = Codecs.uint24(LITTLE_ENDIAN);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0x1000000, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void works_as_a_length_prefix() throws IOException {
    Codec<String> codec = Codecs.prefixed(Codecs.uint24(), Codecs.ascii());

    byte[] framed = encodeString(codec, "hello");

    assertThat(framed).startsWith((byte) 0x00, (byte) 0x00, (byte) 0x05);
    assertThat(codec.decode(new ByteArrayInputStream(framed))).isEqualTo("hello");
  }

  private static byte[] encodeString(Codec<String> codec, String value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    return output.toByteArray();
  }
}
