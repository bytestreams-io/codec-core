package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class ValidatorTest {

  private static final String MUST_BE_POSITIVE = "value must be positive";
  private static final String MUST_BE_SMALL = "value must be small";

  private static final Validator<Integer> POSITIVE = Validator.of(v -> v > 0, MUST_BE_POSITIVE);

  @Test
  void of_message_passes() {
    assertThat(POSITIVE.check(1)).isEmpty();
  }

  @Test
  void of_message_fails() {
    assertThat(POSITIVE.check(0)).contains(MUST_BE_POSITIVE);
  }

  @Test
  void of_message_function_describes_the_rejected_value() {
    Validator<Integer> validator =
        Validator.of(v -> v > 0, "value [%d] must be positive"::formatted);

    assertThat(validator.check(-5)).contains("value [-5] must be positive");
  }

  @Test
  void of_message_function_not_applied_when_check_passes() {
    AtomicInteger calls = new AtomicInteger();
    Validator<Integer> validator =
        Validator.of(
            v -> v > 0,
            v -> {
              calls.incrementAndGet();
              return MUST_BE_POSITIVE;
            });

    assertThat(validator.check(1)).isEmpty();
    assertThat(calls).hasValue(0);
  }

  @Test
  void and_passes_when_both_pass() {
    Validator<Integer> validator = POSITIVE.and(Validator.of(v -> v < 100, MUST_BE_SMALL));

    assertThat(validator.check(50)).isEmpty();
  }

  @Test
  void and_reports_the_first_failure() {
    Validator<Integer> validator = POSITIVE.and(Validator.of(v -> v < 100, MUST_BE_SMALL));

    assertThat(validator.check(-1)).contains(MUST_BE_POSITIVE);
  }

  @Test
  void and_reports_the_second_failure_when_the_first_passes() {
    Validator<Integer> validator = POSITIVE.and(Validator.of(v -> v < 100, MUST_BE_SMALL));

    assertThat(validator.check(500)).contains(MUST_BE_SMALL);
  }

  @Test
  void and_does_not_run_the_second_check_after_a_failure() {
    AtomicInteger calls = new AtomicInteger();
    Validator<Integer> second =
        value -> {
          calls.incrementAndGet();
          return Optional.empty();
        };

    assertThat(POSITIVE.and(second).check(-1)).contains(MUST_BE_POSITIVE);
    assertThat(calls).hasValue(0);
  }

  @Test
  void of_message_function_returning_null_is_reported() {
    // Optional.ofNullable here would read as "no failure" and pass a value that failed the check
    Validator<Integer> validator = Validator.of(v -> v > 0, v -> null);

    assertThatThrownBy(() -> validator.check(0))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("message function returned null");
  }

  @Test
  void of_reports_null_arguments_in_declaration_order() {
    Predicate<Integer> noCheck = null;
    assertThatThrownBy(() -> Validator.of(noCheck, (String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("check");
    assertThatThrownBy(() -> Validator.of(noCheck, (Function<Integer, String>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("check");
  }

  @Test
  void of_null_check() {
    assertThatThrownBy(() -> Validator.of(null, "message"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("check");
  }

  @Test
  void of_null_message() {
    Predicate<Integer> check = v -> true;
    assertThatThrownBy(() -> Validator.of(check, (String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("message");
  }

  @Test
  void of_message_function_null_check() {
    Function<Integer, String> message = String::valueOf;
    assertThatThrownBy(() -> Validator.of(null, message))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("check");
  }

  @Test
  void of_message_function_null_message() {
    Predicate<Integer> check = v -> true;
    assertThatThrownBy(() -> Validator.of(check, (Function<Integer, String>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("message");
  }

  @Test
  void and_null_other() {
    assertThatThrownBy(() -> POSITIVE.and(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("other");
  }
}
