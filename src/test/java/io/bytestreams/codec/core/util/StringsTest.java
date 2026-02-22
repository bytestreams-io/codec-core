package io.bytestreams.codec.core.util;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

class StringsTest {

  @Test
  void isSingleByte_ascii() {
    assertThat(Strings.isSingleByte(US_ASCII)).isTrue();
  }

  @Test
  void isSingleByte_latin1() {
    assertThat(Strings.isSingleByte(ISO_8859_1)).isTrue();
  }

  @Test
  void isSingleByte_utf8() {
    assertThat(Strings.isSingleByte(UTF_8)).isFalse();
  }

  @Test
  void padStart() {
    assertThat(Strings.padStart("abc", '0', 5)).isEqualTo("00abc");
  }

  @Test
  void padStart_empty_string() {
    assertThat(Strings.padStart("", '0', 3)).isEqualTo("000");
  }

  @Test
  void padStart_different_padding() {
    assertThat(Strings.padStart("abc", ' ', 5)).isEqualTo("  abc");
  }

  @Test
  void padStart_already_at_length() {
    assertThat(Strings.padStart("abc", '0', 3)).isEqualTo("abc");
  }

  @Test
  void padStart_exceeds_length() {
    assertThat(Strings.padStart("abcde", '0', 3)).isEqualTo("abcde");
  }

  @Test
  void padEnd() {
    assertThat(Strings.padEnd("abc", '0', 5)).isEqualTo("abc00");
  }

  @Test
  void padEnd_empty_string() {
    assertThat(Strings.padEnd("", '0', 3)).isEqualTo("000");
  }

  @Test
  void padEnd_different_padding() {
    assertThat(Strings.padEnd("abc", ' ', 5)).isEqualTo("abc  ");
  }

  @Test
  void padEnd_already_at_length() {
    assertThat(Strings.padEnd("abc", '0', 3)).isEqualTo("abc");
  }

  @Test
  void padEnd_exceeds_length() {
    assertThat(Strings.padEnd("abcde", '0', 3)).isEqualTo("abcde");
  }

  @Test
  void codePointCount() {
    assertThat(Strings.codePointCount("hello")).isEqualTo(5);
  }

  @Test
  void codePointCount_empty() {
    assertThat(Strings.codePointCount("")).isZero();
  }

  @Test
  void codePointCount_surrogate_pair() {
    assertThat(Strings.codePointCount("a\uD83D\uDE00b")).isEqualTo(3);
  }

  @Test
  void hexByteCount_string() {
    assertThat(Strings.hexByteCount("AABBCC")).isEqualTo(3);
  }

  @Test
  void hexByteCount_string_empty() {
    assertThat(Strings.hexByteCount("")).isZero();
  }

  @Test
  void hexByteCount_string_odd_length() {
    assertThat(Strings.hexByteCount("ABC")).isEqualTo(2);
  }

  @Test
  void hexByteCount_digits() {
    assertThat(Strings.hexByteCount(6)).isEqualTo(3);
  }

  @Test
  void hexByteCount_digits_zero() {
    assertThat(Strings.hexByteCount(0)).isZero();
  }

  @Test
  void hexByteCount_digits_odd() {
    assertThat(Strings.hexByteCount(3)).isEqualTo(2);
  }

  @Test
  void padStart_function() {
    Function<String, String> pad = Strings.padStart('0', 6);
    assertThat(pad.apply("abc")).isEqualTo("000abc");
    assertThat(pad.apply("abcdef")).isEqualTo("abcdef");
    assertThat(pad.apply("abcdefgh")).isEqualTo("abcdefgh");
  }

  @Test
  void padEnd_function() {
    Function<String, String> pad = Strings.padEnd(' ', 5);
    assertThat(pad.apply("hi")).isEqualTo("hi   ");
    assertThat(pad.apply("hello")).isEqualTo("hello");
    assertThat(pad.apply("toolong")).isEqualTo("toolong");
  }
}
