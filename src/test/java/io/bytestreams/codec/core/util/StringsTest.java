package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringsTest {

  @Test
  void padStart() {
    assertThat(Strings.padStart("abc", 5, '0')).isEqualTo("00abc");
  }

  @Test
  void padStart_empty_string() {
    assertThat(Strings.padStart("", 3, '0')).isEqualTo("000");
  }

  @Test
  void padStart_different_padding() {
    assertThat(Strings.padStart("abc", 5, ' ')).isEqualTo("  abc");
  }

  @Test
  void padStart_already_at_length() {
    assertThat(Strings.padStart("abc", 3, '0')).isEqualTo("abc");
  }

  @Test
  void padStart_exceeds_length() {
    assertThat(Strings.padStart("abcde", 3, '0')).isEqualTo("abcde");
  }

  @Test
  void padEnd() {
    assertThat(Strings.padEnd("abc", 5, '0')).isEqualTo("abc00");
  }

  @Test
  void padEnd_empty_string() {
    assertThat(Strings.padEnd("", 3, '0')).isEqualTo("000");
  }

  @Test
  void padEnd_different_padding() {
    assertThat(Strings.padEnd("abc", 5, ' ')).isEqualTo("abc  ");
  }

  @Test
  void padEnd_already_at_length() {
    assertThat(Strings.padEnd("abc", 3, '0')).isEqualTo("abc");
  }

  @Test
  void padEnd_exceeds_length() {
    assertThat(Strings.padEnd("abcde", 3, '0')).isEqualTo("abcde");
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
}
