package io.bytestreams.codec.core.api;

import java.util.Set;

/**
 * Interface for generic data objects that store field-value pairs.
 *
 * @param <T> the concrete type of the data object for method chaining
 */
public interface DataObject<T extends DataObject<T>> {

  /**
   * Returns the set of field names present in this data object.
   *
   * @return the set of field names
   */
  Set<String> fields();

  /**
   * Returns the value of the specified field.
   *
   * @param field the field name
   * @return the value of the field
   * @param <V> the type of the value
   */
  <V> V get(String field);

  /**
   * Sets the value of the specified field.
   *
   * @param field the field name
   * @param value the value to set
   * @return this data object for method chaining
   * @param <V> the type of the value
   */
  <V> T set(String field, V value);

  /**
   * Clears the value of the specified field.
   *
   * @param field the field name
   * @return this data object for method chaining
   */
  T clear(String field);
}
