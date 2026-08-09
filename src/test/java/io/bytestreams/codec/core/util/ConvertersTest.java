package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import org.junit.jupiter.api.Test;

class ConvertersTest {

  private final Converter<String, String> rightPad = Converters.rightPad(' ', 5);
  private final Converter<String, String> leftPad = Converters.leftPad('0', 5);
  private final Converter<String, String> rightFitPad = Converters.rightFitPad(' ', 5);
  private final Converter<String, String> leftFitPad = Converters.leftFitPad('0', 5);
  private final Converter<String, String> leftEvenPad = Converters.leftEvenPad('0');
  private final Converter<String, String> rightEvenPad = Converters.rightEvenPad('F');

  // rightPad

  @Test
  void rightPad_from() {
    assertThat(rightPad.from("hi")).isEqualTo("hi   ");
  }

  @Test
  void rightPad_from_exact_length() {
    assertThat(rightPad.from("hello")).isEqualTo("hello");
  }

  @Test
  void rightPad_from_over_length() {
    assertThatThrownBy(() -> rightPad.from("toolong")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rightPad_from_empty() {
    assertThat(rightPad.from("")).isEqualTo("     ");
  }

  @Test
  void rightPad_to() {
    assertThat(rightPad.to("hi   ")).isEqualTo("hi");
  }

  @Test
  void rightPad_to_no_padding() {
    assertThat(rightPad.to("hello")).isEqualTo("hello");
  }

  @Test
  void rightPad_to_all_padding() {
    assertThat(rightPad.to("     ")).isEmpty();
  }

  @Test
  void rightPad_invalid_length() {
    assertThatThrownBy(() -> Converters.rightPad(' ', 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // leftPad

  @Test
  void leftPad_from() {
    assertThat(leftPad.from("42")).isEqualTo("00042");
  }

  @Test
  void leftPad_from_exact_length() {
    assertThat(leftPad.from("12345")).isEqualTo("12345");
  }

  @Test
  void leftPad_from_over_length() {
    assertThatThrownBy(() -> leftPad.from("toolong")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void leftPad_from_empty() {
    assertThat(leftPad.from("")).isEqualTo("00000");
  }

  @Test
  void leftPad_to() {
    assertThat(leftPad.to("00042")).isEqualTo("42");
  }

  @Test
  void leftPad_to_no_padding() {
    assertThat(leftPad.to("12345")).isEqualTo("12345");
  }

  @Test
  void leftPad_to_all_padding() {
    assertThat(leftPad.to("00000")).isEmpty();
  }

  @Test
  void leftPad_invalid_length() {
    assertThatThrownBy(() -> Converters.leftPad('0', 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // rightFitPad

  @Test
  void rightFitPad_from() {
    assertThat(rightFitPad.from("hi")).isEqualTo("hi   ");
  }

  @Test
  void rightFitPad_from_exact_length() {
    assertThat(rightFitPad.from("hello")).isEqualTo("hello");
  }

  @Test
  void rightFitPad_from_over_length() {
    assertThat(rightFitPad.from("toolong")).isEqualTo("toolo");
  }

  @Test
  void rightFitPad_to() {
    assertThat(rightFitPad.to("hi   ")).isEqualTo("hi");
  }

  @Test
  void rightFitPad_to_no_padding() {
    assertThat(rightFitPad.to("hello")).isEqualTo("hello");
  }

  @Test
  void rightFitPad_to_all_padding() {
    assertThat(rightFitPad.to("     ")).isEmpty();
  }

  @Test
  void rightFitPad_invalid_length() {
    assertThatThrownBy(() -> Converters.rightFitPad(' ', 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // leftFitPad

  @Test
  void leftFitPad_from() {
    assertThat(leftFitPad.from("42")).isEqualTo("00042");
  }

  @Test
  void leftFitPad_from_exact_length() {
    assertThat(leftFitPad.from("12345")).isEqualTo("12345");
  }

  @Test
  void leftFitPad_from_over_length() {
    assertThat(leftFitPad.from("toolong")).isEqualTo("olong");
  }

  @Test
  void leftFitPad_to() {
    assertThat(leftFitPad.to("00042")).isEqualTo("42");
  }

  @Test
  void leftFitPad_to_no_padding() {
    assertThat(leftFitPad.to("12345")).isEqualTo("12345");
  }

  @Test
  void leftFitPad_to_all_padding() {
    assertThat(leftFitPad.to("00000")).isEmpty();
  }

  @Test
  void leftFitPad_invalid_length() {
    assertThatThrownBy(() -> Converters.leftFitPad('0', 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // leftEvenPad

  @Test
  void leftEvenPad_from_odd() {
    assertThat(leftEvenPad.from("123")).isEqualTo("0123");
  }

  @Test
  void leftEvenPad_from_even() {
    assertThat(leftEvenPad.from("1234")).isEqualTo("1234");
  }

  @Test
  void leftEvenPad_from_empty() {
    assertThat(leftEvenPad.from("")).isEmpty();
  }

  @Test
  void leftEvenPad_to() {
    assertThat(leftEvenPad.to("0123")).isEqualTo("123");
  }

  @Test
  void leftEvenPad_to_no_padding() {
    assertThat(leftEvenPad.to("1234")).isEqualTo("1234");
  }

  @Test
  void leftEvenPad_to_all_padding() {
    assertThat(leftEvenPad.to("00")).isEmpty();
  }

  // rightEvenPad

  @Test
  void rightEvenPad_from_odd() {
    assertThat(rightEvenPad.from("123")).isEqualTo("123F");
  }

  @Test
  void rightEvenPad_from_even() {
    assertThat(rightEvenPad.from("1234")).isEqualTo("1234");
  }

  @Test
  void rightEvenPad_from_empty() {
    assertThat(rightEvenPad.from("")).isEmpty();
  }

  @Test
  void rightEvenPad_to() {
    assertThat(rightEvenPad.to("123F")).isEqualTo("123");
  }

  @Test
  void rightEvenPad_to_no_padding() {
    assertThat(rightEvenPad.to("1234")).isEqualTo("1234");
  }

  @Test
  void rightEvenPad_to_all_padding() {
    assertThat(rightEvenPad.to("FF")).isEmpty();
  }

  // toInt

  @Test
  void toInt_from() {
    assertThat(Converters.toInt(4).from(42)).isEqualTo("0042");
  }

  @Test
  void toInt_from_exact_length() {
    assertThat(Converters.toInt(4).from(1234)).isEqualTo("1234");
  }

  @Test
  void toInt_from_zero() {
    assertThat(Converters.toInt(3).from(0)).isEqualTo("000");
  }

  @Test
  void toInt_to() {
    assertThat(Converters.toInt(4).to("0042")).isEqualTo(42);
  }

  @Test
  void toInt_to_zero() {
    assertThat(Converters.toInt(3).to("000")).isZero();
  }

  @Test
  void toInt_to_invalid() {
    Converter<String, Integer> converter = Converters.toInt(4);
    assertThatThrownBy(() -> converter.to("abcd"))
        .isInstanceOf(ConverterException.class)
        .hasMessageContaining("invalid integer: abcd")
        .hasCauseInstanceOf(NumberFormatException.class);
  }

  @Test
  void toInt_invalid_digits() {
    assertThatThrownBy(() -> Converters.toInt(0)).isInstanceOf(IllegalArgumentException.class);
  }

  // toLong

  @Test
  void toLong_from() {
    assertThat(Converters.toLong(10).from(12345678L)).isEqualTo("0012345678");
  }

  @Test
  void toLong_from_exact_length() {
    assertThat(Converters.toLong(10).from(1234567890L)).isEqualTo("1234567890");
  }

  @Test
  void toLong_from_zero() {
    assertThat(Converters.toLong(3).from(0L)).isEqualTo("000");
  }

  @Test
  void toLong_to() {
    assertThat(Converters.toLong(10).to("0012345678")).isEqualTo(12345678L);
  }

  @Test
  void toLong_to_zero() {
    assertThat(Converters.toLong(3).to("000")).isZero();
  }

  @Test
  void toLong_to_invalid() {
    Converter<String, Long> converter = Converters.toLong(10);
    assertThatThrownBy(() -> converter.to("abcdefghij"))
        .isInstanceOf(ConverterException.class)
        .hasMessageContaining("invalid long: abcdefghij")
        .hasCauseInstanceOf(NumberFormatException.class);
  }

  @Test
  void toLong_invalid_digits() {
    assertThatThrownBy(() -> Converters.toLong(0)).isInstanceOf(IllegalArgumentException.class);
  }

  // temporal

  @Test
  void temporal_to_localDate() {
    Converter<String, LocalDate> converter = Converters.temporal("yyyyMMdd", LocalDate::from);
    assertThat(converter.to("20250115")).isEqualTo(LocalDate.of(2025, 1, 15));
  }

  @Test
  void temporal_from_localDate() {
    Converter<String, LocalDate> converter = Converters.temporal("yyyyMMdd", LocalDate::from);
    assertThat(converter.from(LocalDate.of(2025, 1, 15))).isEqualTo("20250115");
  }

  @Test
  void temporal_to_localTime() {
    Converter<String, LocalTime> converter = Converters.temporal("HHmmss", LocalTime::from);
    assertThat(converter.to("143052")).isEqualTo(LocalTime.of(14, 30, 52));
  }

  @Test
  void temporal_from_localTime() {
    Converter<String, LocalTime> converter = Converters.temporal("HHmmss", LocalTime::from);
    assertThat(converter.from(LocalTime.of(14, 30, 52))).isEqualTo("143052");
  }

  @Test
  void temporal_to_localDateTime() {
    Converter<String, LocalDateTime> converter =
        Converters.temporal("yyyyMMddHHmmss", LocalDateTime::from);
    assertThat(converter.to("20250115143052")).isEqualTo(LocalDateTime.of(2025, 1, 15, 14, 30, 52));
  }

  @Test
  void temporal_from_localDateTime() {
    Converter<String, LocalDateTime> converter =
        Converters.temporal("yyyyMMddHHmmss", LocalDateTime::from);
    assertThat(converter.from(LocalDateTime.of(2025, 1, 15, 14, 30, 52)))
        .isEqualTo("20250115143052");
  }

  @Test
  void temporal_to_invalid() {
    Converter<String, LocalDate> converter = Converters.temporal("yyyyMMdd", LocalDate::from);
    assertThatThrownBy(() -> converter.to("notadate"))
        .isInstanceOf(ConverterException.class)
        .hasMessageContaining("invalid temporal: notadate")
        .hasCauseInstanceOf(DateTimeParseException.class);
  }

  @Test
  void temporal_null_format() {
    assertThatThrownBy(() -> Converters.temporal((String) null, LocalDate::from))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void temporal_null_query() {
    assertThatThrownBy(() -> Converters.temporal("yyyyMMdd", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void temporal_formatter_to_localDate() {
    Converter<String, LocalDate> converter =
        Converters.temporal(DateTimeFormatter.ofPattern("MM/dd/uuuu"), LocalDate::from);
    assertThat(converter.to("01/15/2025")).isEqualTo(LocalDate.of(2025, 1, 15));
  }

  @Test
  void temporal_formatter_from_localDate() {
    Converter<String, LocalDate> converter =
        Converters.temporal(DateTimeFormatter.ofPattern("MM/dd/uuuu"), LocalDate::from);
    assertThat(converter.from(LocalDate.of(2025, 1, 15))).isEqualTo("01/15/2025");
  }

  @Test
  void temporal_formatter_strict_rejectsInvalidDate() {
    Converter<String, LocalDate> converter =
        Converters.temporal(
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT),
            LocalDate::from);
    assertThatThrownBy(() -> converter.to("02/31/2025"))
        .isInstanceOf(ConverterException.class)
        .hasMessageContaining("invalid temporal: 02/31/2025")
        .hasCauseInstanceOf(DateTimeParseException.class);
  }

  @Test
  void temporal_smart_clampsInvalidDate() {
    Converter<String, LocalDate> converter = Converters.temporal("MM/dd/uuuu", LocalDate::from);
    assertThat(converter.to("02/31/2025")).isEqualTo(LocalDate.of(2025, 2, 28));
  }

  @Test
  void temporal_null_formatter() {
    assertThatThrownBy(() -> Converters.temporal((DateTimeFormatter) null, LocalDate::from))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void scaled_applies_the_implied_decimal_point() {
    Converter<Long, BigDecimal> scaled = Converters.scaled(2);

    assertThat(scaled.to(12345L)).isEqualTo(new BigDecimal("123.45"));
    assertThat(scaled.to(12345L).scale()).isEqualTo(2);
    assertThat(scaled.from(new BigDecimal("123.45"))).isEqualTo(12345L);
  }

  @Test
  void scaled_is_exact_rather_than_floating_point() {
    // dividing by 100.0 first and wrapping the double keeps the binary expansion:
    // 123.4500000000000028421709430404007434844970703125
    assertThat(Converters.scaled(2).to(12345L)).isEqualByComparingTo("123.45");
    assertThat(new BigDecimal(12345 / 100.0)).isNotEqualByComparingTo("123.45");
  }

  @Test
  void scaled_handles_negative_values() {
    Converter<Long, BigDecimal> scaled = Converters.scaled(2);

    assertThat(scaled.to(-12345L)).isEqualTo(new BigDecimal("-123.45"));
    assertThat(scaled.from(new BigDecimal("-123.45"))).isEqualTo(-12345L);
  }

  @Test
  void scaled_accepts_a_value_with_fewer_decimals_than_the_scale() {
    assertThat(Converters.scaled(2).from(new BigDecimal("123.4"))).isEqualTo(12340L);
    assertThat(Converters.scaled(2).from(new BigDecimal("123"))).isEqualTo(12300L);
  }

  @Test
  void scaled_rejects_a_value_needing_rounding() {
    Converter<Long, BigDecimal> scaled = Converters.scaled(2);
    BigDecimal tooPrecise = new BigDecimal("123.456");

    assertThatThrownBy(() -> scaled.from(tooPrecise))
        .isInstanceOf(ConverterException.class)
        .hasMessageContaining("123.456");
  }

  @Test
  void scaled_rejects_a_value_too_large_for_a_long() {
    Converter<Long, BigDecimal> scaled = Converters.scaled(2);
    BigDecimal huge = new BigDecimal("999999999999999999999");

    assertThatThrownBy(() -> scaled.from(huge)).isInstanceOf(ConverterException.class);
  }

  @Test
  void scaled_with_zero_scale_is_a_plain_integer() {
    Converter<Long, BigDecimal> scaled = Converters.scaled(0);

    assertThat(scaled.to(12345L)).isEqualTo(new BigDecimal("12345"));
    assertThat(scaled.from(new BigDecimal("12345"))).isEqualTo(12345L);
  }

  @Test
  void scaled_rejects_a_negative_scale() {
    assertThatThrownBy(() -> Converters.scaled(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("scale");
  }

  @Test
  void toInt_rejects_a_negative_value() {
    Converter<String, Integer> converter = Converters.toInt(6);

    assertThatThrownBy(() -> converter.from(-123))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsigned");
  }

  @Test
  void toLong_rejects_a_negative_value() {
    Converter<String, Long> converter = Converters.toLong(12);

    assertThatThrownBy(() -> converter.from(-123L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsigned");
  }

  @Test
  void toBigInt_parses_and_pads() {
    Converter<String, BigInteger> converter = Converters.toBigInt(10);

    assertThat(converter.to("0000012345")).isEqualTo(new BigInteger("12345"));
    assertThat(converter.from(new BigInteger("12345"))).isEqualTo("0000012345");
  }

  @Test
  void toBigInt_carries_values_beyond_a_long() {
    Converter<String, BigInteger> converter = Converters.toBigInt(25);
    BigInteger wide = new BigInteger("1234567890123456789012345");

    assertThat(converter.to("1234567890123456789012345")).isEqualTo(wide);
    assertThat(converter.from(wide)).isEqualTo("1234567890123456789012345");
  }

  @Test
  void toBigInt_accepts_a_positive_value_whose_low_bits_look_negative() {
    // 2^63 truncates to Long.MIN_VALUE, so a longValue-based sign check would reject it
    Converter<String, BigInteger> converter = Converters.toBigInt(19);
    BigInteger justOverLong = new BigInteger("9223372036854775808");

    assertThat(justOverLong.longValue()).isNegative();
    assertThat(converter.from(justOverLong)).isEqualTo("9223372036854775808");
  }

  @Test
  void toBigInt_rejects_a_negative_value() {
    Converter<String, BigInteger> converter = Converters.toBigInt(10);
    BigInteger negative = new BigInteger("-5");

    assertThatThrownBy(() -> converter.from(negative))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsigned");
  }

  @Test
  void toBigInt_rejects_a_non_numeric_string() {
    Converter<String, BigInteger> converter = Converters.toBigInt(4);

    assertThatThrownBy(() -> converter.to("12x4"))
        .isInstanceOf(ConverterException.class)
        .hasMessageContaining("12x4");
  }

  @Test
  void toBigInt_rejects_a_non_positive_digit_count() {
    assertThatThrownBy(() -> Converters.toBigInt(0)).isInstanceOf(IllegalArgumentException.class);
  }
}
