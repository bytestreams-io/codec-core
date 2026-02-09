package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PredicatesTest {

  @Test
  void alwaysTrue_returns_true() {
    assertThat(Predicates.alwaysTrue().test("any")).isTrue();
    assertThat(Predicates.alwaysTrue().test(null)).isTrue();
    assertThat(Predicates.alwaysTrue().test(123)).isTrue();
  }

  @Test
  void alwaysFalse_returns_false() {
    assertThat(Predicates.alwaysFalse().test("any")).isFalse();
    assertThat(Predicates.alwaysFalse().test(null)).isFalse();
    assertThat(Predicates.alwaysFalse().test(123)).isFalse();
  }

  @Test
  void alwaysTrue_returns_same_instance() {
    assertThat(Predicates.alwaysTrue()).isSameAs(Predicates.alwaysTrue());
  }

  @Test
  void alwaysFalse_returns_same_instance() {
    assertThat(Predicates.alwaysFalse()).isSameAs(Predicates.alwaysFalse());
  }
}
