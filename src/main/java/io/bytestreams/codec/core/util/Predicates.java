package io.bytestreams.codec.core.util;

import java.util.function.Predicate;

/** Utility methods for creating common predicates. */
public final class Predicates {

  private Predicates() {}

  private static final Predicate<Object> ALWAYS_TRUE = value -> true;
  private static final Predicate<Object> ALWAYS_FALSE = value -> false;

  /**
   * Returns a predicate that always returns true.
   *
   * @param <T> the type of the input to the predicate
   * @return a predicate that always returns true
   */
  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> alwaysTrue() {
    return (Predicate<T>) ALWAYS_TRUE;
  }

  /**
   * Returns a predicate that always returns false.
   *
   * @param <T> the type of the input to the predicate
   * @return a predicate that always returns false
   */
  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> alwaysFalse() {
    return (Predicate<T>) ALWAYS_FALSE;
  }
}
