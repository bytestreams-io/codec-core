package io.bytestreams.codec.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A ready-made {@link Tagged} implementation backed by a map of tag to value list.
 *
 * <p>This is a convenience class that eliminates the need to write boilerplate {@link Tagged}
 * implementations. It is intentionally final — for typed accessors or custom behavior, implement
 * {@link Tagged} directly.
 *
 * @param <K> the tag key type
 */
public final class TaggedDataObject<K> implements Tagged<TaggedDataObject<K>, K> {

  private final Map<K, List<Object>> fields = new LinkedHashMap<>();

  @Override
  public Set<K> tags() {
    return Collections.unmodifiableSet(fields.keySet());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> List<V> getAll(K tag) {
    return Collections.unmodifiableList((List<V>) fields.getOrDefault(tag, List.of()));
  }

  @Override
  public <V> TaggedDataObject<K> add(K tag, V value) {
    fields.computeIfAbsent(tag, k -> new ArrayList<>()).add(value);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TaggedDataObject<?> that)) return false;
    return fields.equals(that.fields);
  }

  @Override
  public int hashCode() {
    return fields.hashCode();
  }

  @Override
  public String toString() {
    return fields.toString();
  }
}
