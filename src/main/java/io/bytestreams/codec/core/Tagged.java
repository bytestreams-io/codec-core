package io.bytestreams.codec.core;

import java.util.List;
import java.util.Set;

/**
 * Interface for objects with tag-identified fields.
 *
 * <p>Implementations should use insertion-ordered storage (e.g. {@link java.util.LinkedHashMap}) so
 * that {@link #tags()} preserves the order in which tags were first added.
 *
 * @param <T> the concrete type (self-type for fluent chaining)
 */
public interface Tagged<T extends Tagged<T>> {

  /**
   * Returns the set of tag names.
   *
   * @return the tag names
   */
  Set<String> tags();

  /**
   * Returns all values for the given tag. Implementations must return an empty list if the tag is
   * absent.
   *
   * @param tag the tag name
   * @param <V> the value type
   * @return the values for the tag
   */
  <V> List<V> getAll(String tag);

  /**
   * Appends a value for the given tag.
   *
   * @param tag the tag name
   * @param value the value
   * @param <V> the value type
   * @return this object for fluent chaining
   */
  <V> T add(String tag, V value);
}
