package io.bytestreams.codec.core;

/**
 * Extension point for custom codecs to participate in field inspection.
 *
 * <p>Built-in codecs do not implement this interface — the {@link Inspector} utility handles them
 * directly. Custom codec implementations can implement this interface to provide structured
 * representations of their values during inspection.
 *
 * @param <T> the type of the value to inspect
 */
public interface Inspectable<T> {

  /**
   * Returns a structured representation of the given value.
   *
   * @param value the decoded value to inspect
   * @return a structured representation (Map, List, or scalar)
   */
  Object inspect(T value);
}
