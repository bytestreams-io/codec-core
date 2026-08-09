package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** Numeric fields wider than a long holds — COBOL allows PIC 9(31). */
class BigIntegerCodecTest {

  private static final BigInteger WIDE = new BigInteger("1234567890123456789012345");

  @Test
  void ascii_carries_a_value_beyond_a_long() throws IOException {
    Codec<BigInteger> codec = Codecs.asciiBigInt(25);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(WIDE, output);

    assertThat(output.toString(US_ASCII)).isEqualTo("1234567890123456789012345");
    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo(WIDE);
  }

  @Test
  void ascii_pads_a_short_value() throws IOException {
    Codec<BigInteger> codec = Codecs.asciiBigInt(25);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(BigInteger.ONE, output);

    assertThat(output.toString(US_ASCII)).isEqualTo("0000000000000000000000001");
  }

  @Test
  void ebcdic_carries_a_value_beyond_a_long() throws IOException {
    Codec<BigInteger> codec = Codecs.ebcdicBigInt(25);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(WIDE, output);

    assertThat(output.toByteArray()).hasSize(25).startsWith((byte) 0xF1, (byte) 0xF2);
    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo(WIDE);
  }

  @Test
  void bcd_carries_a_value_beyond_a_long() throws IOException {
    // 24 digits packs into 12 bytes, where bcdLong stops at 18
    Codec<BigInteger> codec = Codecs.bcdBigInt(24);
    BigInteger value = new BigInteger("123456789012345678901234");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(value, output);

    assertThat(result.count()).isEqualTo(24);
    assertThat(result.bytes()).isEqualTo(12);
    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo(value);
  }

  @Test
  void rejects_a_negative_value() {
    Codec<BigInteger> codec = Codecs.asciiBigInt(10);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    BigInteger negative = new BigInteger("-1");

    assertThatThrownBy(() -> codec.encode(negative, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsigned");
  }

  @Test
  void rejects_a_value_wider_than_the_field() {
    Codec<BigInteger> codec = Codecs.asciiBigInt(4);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    BigInteger tooWide = new BigInteger("12345");

    assertThatThrownBy(() -> codec.encode(tooWide, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_a_non_positive_digit_count() {
    assertThatThrownBy(() -> Codecs.asciiBigInt(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Codecs.ebcdicBigInt(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Codecs.bcdBigInt(0)).isInstanceOf(IllegalArgumentException.class);
  }
}
