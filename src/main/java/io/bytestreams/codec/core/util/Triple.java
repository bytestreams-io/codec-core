package io.bytestreams.codec.core.util;

import java.util.Objects;

/**
 * An immutable triple of three values. No value may be null.
 *
 * @param first the first value
 * @param second the second value
 * @param third the third value
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 * @param <C> the type of the third value
 */
public record Triple<A, B, C>(A first, B second, C third) {

  public Triple {
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(second, "second");
    Objects.requireNonNull(third, "third");
  }
}
