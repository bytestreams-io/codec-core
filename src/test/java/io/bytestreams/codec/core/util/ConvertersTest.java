package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
