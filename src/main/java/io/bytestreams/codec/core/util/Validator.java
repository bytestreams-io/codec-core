package io.bytestreams.codec.core.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A named check on a value, pairing the condition with the message that describes its failure.
 *
 * <p>A validator <em>reports</em> a failure rather than raising one, which is what lets the same
 * check be applied in both directions: a codec throws {@link IllegalArgumentException} on encode
 * and {@code CodecException} on decode from a single validator. A validator that threw could not
 * honour that split, since it cannot know which side it is on.
 *
 * <p>Keeping the condition and its wording together is what makes a check shareable — the pairing
 * is stated once and named, rather than restated at every call site:
 *
 * <pre>{@code
 * static final Validator<Integer> POSITIVE_AMOUNT =
 *     Validator.of(v -> v > 0, "amount must be positive");
 *
 * Codec<Integer> amount = Codecs.uint16().validate(POSITIVE_AMOUNT);
 * Codec<Integer> fee = Codecs.uint32().validate(POSITIVE_AMOUNT.and(UNDER_LIMIT));
 * }</pre>
 *
 * @param <V> the value type
 */
@FunctionalInterface
public interface Validator<V> {

  /**
   * Checks a value.
   *
   * @param value the value to check
   * @return the failure message, or empty if the value is acceptable
   */
  Optional<String> check(V value);

  /**
   * Creates a validator from a condition and a fixed message.
   *
   * @param check the condition a value must satisfy
   * @param message the failure message
   * @param <V> the value type
   * @return a new validator
   * @throws NullPointerException if check or message is null
   */
  static <V> Validator<V> of(Predicate<V> check, String message) {
    Objects.requireNonNull(check, "check");
    Objects.requireNonNull(message, "message");
    return of(check, value -> message);
  }

  /**
   * Creates a validator from a condition and a message computed from the rejected value.
   *
   * <p>The message function runs only on failure, so it is free to be expensive — {@code expected
   * [AABB] but got [CCDD]} costs nothing on the values that pass.
   *
   * @param check the condition a value must satisfy
   * @param message computes the failure message from the rejected value
   * @param <V> the value type
   * @return a new validator
   * @throws NullPointerException if check or message is null
   */
  static <V> Validator<V> of(Predicate<V> check, Function<V, String> message) {
    Objects.requireNonNull(check, "check");
    Objects.requireNonNull(message, "message");
    return value -> {
      if (check.test(value)) {
        return Optional.empty();
      }
      // Not ofNullable: a null message would read as "no failure" and pass a rejected value.
      return Optional.of(
          Objects.requireNonNull(message.apply(value), "message function returned null"));
    };
  }

  /**
   * Returns a validator that applies this check and then the other, reporting the first failure.
   *
   * <p>The second check does not run once the first has failed: a codec raises on the first
   * problem, so there is nowhere for a second message to go.
   *
   * @param other the check to apply after this one
   * @return the composed validator
   * @throws NullPointerException if other is null
   */
  default Validator<V> and(Validator<V> other) {
    Objects.requireNonNull(other, "other");
    Validator<V> self = this;
    return value -> {
      Optional<String> failure = self.check(value);
      return failure.isPresent() ? failure : other.check(value);
    };
  }
}
