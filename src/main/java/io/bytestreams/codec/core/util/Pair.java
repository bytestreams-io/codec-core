package io.bytestreams.codec.core.util;

import java.util.Objects;

/**
 * An immutable pair of two values. Neither value may be null.
 *
 * @param first the first value
 * @param second the second value
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 */
public record Pair<A, B>(A first, B second) {

  public Pair {
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(second, "second");
  }
}
