package io.bytestreams.codec.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable bidirectional map that maintains a one-to-one mapping between keys and values.
 *
 * <p>Both keys and values must be unique. Lookups can be performed in either direction: by key
 * using {@link #to(Object)} or by value using {@link #from(Object)}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * BiMap<Integer, String> biMap = BiMap.of(
 *     Map.entry(1, "one"),
 *     Map.entry(2, "two"),
 *     Map.entry(3, "three")
 * );
 * biMap.to(1);        // returns "one"
 * biMap.from("two"); // returns 2
 * }</pre>
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class BiMap<K, V> implements Converter<K, V> {
  private final Map<K, V> forward;
  private final Map<V, K> inverse;

  private BiMap(Map<K, V> forward, Map<V, K> inverse) {
    this.forward = Map.copyOf(forward);
    this.inverse = Map.copyOf(inverse);
  }

  /**
   * Creates a new {@code BiMap} from the given entries.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param entries the key-value entries
   * @return a new {@code BiMap}
   * @throws IllegalArgumentException if entries is empty, or contains duplicate keys or values
   * @throws NullPointerException if any entry, key, or value is null
   */
  @SafeVarargs
  public static <K, V> BiMap<K, V> of(Map.Entry<K, V>... entries) {
    Preconditions.check(entries.length > 0, "entries must not be empty");
    Map<K, V> forward = new HashMap<>();
    Map<V, K> inverse = new HashMap<>();
    for (Map.Entry<K, V> entry : entries) {
      Objects.requireNonNull(entry, "entry");
      K key = Objects.requireNonNull(entry.getKey(), "key");
      V value = Objects.requireNonNull(entry.getValue(), "value");
      Preconditions.check(!forward.containsKey(key), "duplicate key: %s", key);
      Preconditions.check(!inverse.containsKey(value), "duplicate value: %s", value);
      forward.put(key, value);
      inverse.put(value, key);
    }
    return new BiMap<>(forward, inverse);
  }

  /**
   * Returns the value mapped to the given key.
   *
   * @param key the key to look up
   * @return the value mapped to the key
   * @throws IllegalArgumentException if the key is not present
   */
  @Override
  public V to(K key) {
    V value = forward.get(key);
    Preconditions.check(value != null, "no value for key: %s", key);
    return value;
  }

  /**
   * Returns the key mapped to the given value.
   *
   * @param value the value to look up
   * @return the key mapped to the value
   * @throws IllegalArgumentException if the value is not present
   */
  @Override
  public K from(V value) {
    K key = inverse.get(value);
    Preconditions.check(key != null, "no key for value: %s", value);
    return key;
  }
}
