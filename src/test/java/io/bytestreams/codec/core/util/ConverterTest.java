package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConverterTest {

  private final Converter<String, Integer> parseInt =
      Converter.of(Integer::parseInt, Object::toString);

  private final Converter<String, String> trim =
      Converter.of(String::trim, s -> String.format("%-5s", s));

  @Test
  void to() {
    assertThat(parseInt.to("42")).isEqualTo(42);
  }

  @Test
  void from() {
    assertThat(parseInt.from(42)).isEqualTo("42");
  }

  @Test
  void andThen_to() {
    Converter<String, Integer> trimThenParse = trim.andThen(parseInt);
    assertThat(trimThenParse.to("  42 ")).isEqualTo(42);
  }

  @Test
  void andThen_from() {
    Converter<String, Integer> trimThenParse = trim.andThen(parseInt);
    assertThat(trimThenParse.from(42)).isEqualTo("42   ");
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void of_null_to() {
    assertThatThrownBy(() -> Converter.<String, String>of(null, Object::toString))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("to");
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void of_null_from() {
    assertThatThrownBy(() -> Converter.<String, Integer>of(Integer::parseInt, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("from");
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void andThen_null() {
    assertThatThrownBy(() -> parseInt.andThen(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("after");
  }
}
