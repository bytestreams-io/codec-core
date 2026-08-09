package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.bytestreams.codec.core.util.Converters;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** A scaled amount is one converter over whichever integer encoding the format uses. */
class ScaledAmountTest {

  private static byte[] encode(Codec<BigDecimal> codec, String value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(new BigDecimal(value), output);
    return output.toByteArray();
  }

  @Test
  void packed_decimal_amount() throws IOException {
    // COBOL PIC S9(7)V99 COMP-3
    Codec<BigDecimal> amount = Codecs.packedLong(9).xmap(Converters.scaled(2));

    byte[] encoded = encode(amount, "-12345.67");

    assertThat(encoded).containsExactly(0x00, 0x12, 0x34, 0x56, 0x7D);
    assertThat(amount.decode(new ByteArrayInputStream(encoded))).isEqualByComparingTo("-12345.67");
  }

  @Test
  void zoned_decimal_amount() throws IOException {
    // COBOL PIC S9(3)V99 DISPLAY
    Codec<BigDecimal> amount = Codecs.zonedLong(5).xmap(Converters.scaled(2));

    byte[] encoded = encode(amount, "-123.45");

    assertThat(encoded).containsExactly(0xF1, 0xF2, 0xF3, 0xF4, 0xD5);
    assertThat(amount.decode(new ByteArrayInputStream(encoded))).isEqualByComparingTo("-123.45");
  }

  @Test
  void ascii_numeric_amount() throws IOException {
    // ISO 8583 field 4, n12 in minor units
    Codec<BigDecimal> amount = Codecs.asciiLong(12).xmap(Converters.scaled(2));

    byte[] encoded = encode(amount, "123.45");

    assertThat(new String(encoded, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("000000012345");
    assertThat(amount.decode(new ByteArrayInputStream(encoded))).isEqualByComparingTo("123.45");
  }

  @Test
  void bcd_amount() throws IOException {
    Codec<BigDecimal> amount = Codecs.bcdLong(6).xmap(Converters.scaled(2));

    byte[] encoded = encode(amount, "1234.56");

    assertThat(encoded).containsExactly(0x12, 0x34, 0x56);
    assertThat(amount.decode(new ByteArrayInputStream(encoded))).isEqualByComparingTo("1234.56");
  }

  @Test
  void an_over_precise_amount_is_rejected_with_the_field_path() {
    Codec<Payment> codec =
        Codecs.<Payment>sequential(Payment::new)
            .field(
                "amount",
                Codecs.packedLong(9).xmap(Converters.scaled(2)),
                Payment::getAmount,
                Payment::setAmount)
            .build();
    Payment payment = new Payment();
    payment.setAmount(new BigDecimal("123.456"));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(payment, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("field [amount]")
        .hasMessageContaining("123.456");
  }

  @Test
  void a_zero_padded_numeric_rejects_a_negative_amount() {
    // padding a negative would put the minus sign inside the zeros: "000000-12345"
    Codec<BigDecimal> amount = Codecs.asciiLong(12).xmap(Converters.scaled(2));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    BigDecimal negative = new BigDecimal("-123.45");

    assertThatThrownBy(() -> amount.encode(negative, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsigned");
  }

  static class Payment {
    private BigDecimal amount;

    BigDecimal getAmount() {
      return amount;
    }

    void setAmount(BigDecimal amount) {
      this.amount = amount;
    }
  }
}
