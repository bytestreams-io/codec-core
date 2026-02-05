package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PreconditionsTest {

  @Test
  void check() {
    assertThatCode(() -> Preconditions.check(true, "error")).doesNotThrowAnyException();
  }

  @Test
  void check_failed_condition() {
    assertThatThrownBy(() -> Preconditions.check(false, "error message"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("error message");
  }

  @Test
  void check_failed_condition_with_args() {
    assertThatThrownBy(() -> Preconditions.check(false, "expected %d but got %d", 5, 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("expected 5 but got 3");
  }
}
