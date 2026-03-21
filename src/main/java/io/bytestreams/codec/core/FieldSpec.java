package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Predicates;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
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

  /**
   * Creates a FieldSpec from explicit getter and setter functions.
   *
   * @param name the field name
   * @param codec the codec for the field value
   * @param getter function to extract the field value
   * @param setter consumer to set the field value
   * @param <T> the object type
   * @param <V> the field value type
   * @return a new FieldSpec
   */
  static <T, V> FieldSpec<T, V> of(
      String name, Codec<V> codec, Function<T, V> getter, BiConsumer<T, V> setter) {
    return of(name, codec, getter, setter, Predicates.alwaysTrue());
  }

  /**
   * Creates a FieldSpec from explicit getter, setter, and presence predicate.
   *
   * @param name the field name
   * @param codec the codec for the field value
   * @param getter function to extract the field value
   * @param setter consumer to set the field value
   * @param presence predicate to determine if field is present
   * @param <T> the object type
   * @param <V> the field value type
   * @return a new FieldSpec
   */
  static <T, V> FieldSpec<T, V> of(
      String name,
      Codec<V> codec,
      Function<T, V> getter,
      BiConsumer<T, V> setter,
      Predicate<T> presence) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(codec, "codec");
    Objects.requireNonNull(getter, "getter");
    Objects.requireNonNull(setter, "setter");
    Objects.requireNonNull(presence, "presence");
    return new FieldSpec<>() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public Codec<V> codec() {
        return codec;
      }

      @Override
      public V get(T object) {
        return getter.apply(object);
      }

      @Override
      public void set(T object, V value) {
        setter.accept(object, value);
      }

      @Override
      public Predicate<T> presence() {
        return presence;
      }
    };
  }
}
