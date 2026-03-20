package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Predicates;
import java.util.function.Predicate;

/**
 * Describes how to access a field on an object — its name, codec, getter, setter, and presence
 * predicate.
 *
 * @param <T> the object type containing this field
 * @param <V> the field value type
 */
public interface FieldSpec<T, V> {
  String name();

  Codec<V> codec();

  V get(T object);

  void set(T object, V value);

  /**
   * Returns the presence predicate for this field. If the predicate returns false, the field is
   * skipped during encoding and decoding. Defaults to always true (field is always present).
   *
   * @return the presence predicate
   */
  default Predicate<T> presence() {
    return Predicates.alwaysTrue();
  }
}
