package io.bytestreams.codec.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A map-backed data object for use with sequential codecs.
 *
 * <p>Fields are stored by string key in insertion order. Use {@link #field(String, Codec)} to
 * create {@link FieldSpec} instances that provide type-safe access and integrate with {@link
 * SequentialObjectCodec.Builder}.
 *
 * <p>Subclass to add typed accessors:
 *
 * <pre>{@code
 * class Message extends SimpleDataObject {
 *   static final FieldSpec<Message, Integer> ID = field("id", Codecs.uint16());
 *
 *   public int getId() { return get(ID); }
 *   public void setId(int id) { set(ID, id); }
 * }
 * }</pre>
 */
public class SimpleDataObject {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  @SuppressWarnings("unchecked")
  public <V> V get(String key) {
    return (V) fields.get(key);
  }

  public void set(String key, Object value) {
    if (value == null) {
      fields.remove(key);
    } else {
      fields.put(key, value);
    }
  }

  public <V> V get(FieldSpec<? extends SimpleDataObject, V> spec) {
    return get(spec.name());
  }

  public <V> void set(FieldSpec<? extends SimpleDataObject, V> spec, V value) {
    set(spec.name(), value);
  }

  /**
   * Creates a FieldSpec backed by this map.
   *
   * @param name the field name (used as map key)
   * @param codec the codec for the field value
   * @param <T> the SimpleDataObject subclass type
   * @param <V> the field value type
   * @return a new FieldSpec
   */
  public static <T extends SimpleDataObject, V> FieldSpec<T, V> field(String name, Codec<V> codec) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(codec, "codec");
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
      public V get(T obj) {
        return obj.get(name);
      }

      @Override
      public void set(T obj, V value) {
        obj.set(name, value);
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SimpleDataObject that)) return false;
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
