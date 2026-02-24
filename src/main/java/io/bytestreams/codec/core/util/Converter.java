package io.bytestreams.codec.core.util;

import java.util.Objects;

/**
 * A bidirectional conversion between two types.
 *
 * <p>Converters transform values in both directions: {@link #to(Object)} converts from {@code V}
 * to {@code U}, and {@link #from(Object)} converts from {@code U} back to {@code V}. Unlike a
 * mathematical bijection, conversions are not required to be strictly one-to-one.
 *
 * <p>Converters can be composed using {@link #andThen(Converter)} or created from function pairs
 * using {@link Converters#of(java.util.function.Function, java.util.function.Function)
 * Converters.of}.
 *
 * @param <V> the source type
 * @param <U> the target type
 */
public interface Converter<V, U> {

  /**
   * Converts a value from the source type to the target type.
   *
   * @param value the source value
   * @return the converted target value
   */
  U to(V value);

  /**
   * Converts a value from the target type back to the source type.
   *
   * @param value the target value
   * @return the converted source value
   */
  V from(U value);

  /**
   * Returns a composed converter that first applies this converter, then applies the {@code after}
   * converter.
   *
   * @param <T> the target type of the resulting converter
   * @param after the converter to apply after this one
   * @return the composed converter
   */
  default <T> Converter<V, T> andThen(Converter<U, T> after) {
    Objects.requireNonNull(after, "after");
    Converter<V, U> self = this;
    return new Converter<>() {
      @Override
      public T to(V value) {
        return after.to(self.to(value));
      }

      @Override
      public V from(T value) {
        return self.from(after.from(value));
      }
    };
  }
}
