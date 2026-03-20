package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Predicates;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Abstract map-backed data object for use with sequential codecs.
 *
 * <p>Fields are stored by string key in insertion order. Access is protected — subclasses control
 * which fields are exposed and whether they are mutable.
 *
 * <p>Use {@link #field(String, Codec)} to create {@link FieldSpec} instances that provide type-safe
 * access and integrate with {@link SequentialObjectCodec.Builder}.
 *
 * <p>For open access with no restrictions, use {@link SimpleData}. For typed, controlled access,
 * subclass directly:
 *
 * <pre>{@code
 * class Message extends DataObject {
 *   static final FieldSpec<Message, Integer> ID = field("id", Codecs.uint16());
 *
 *   public int getId() { return get(ID); }
 *   // no setter — read-only to consumers, codec can still set via FieldSpec
 * }
 * }</pre>
 */
public abstract class DataObject {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  @SuppressWarnings("unchecked")
  protected <V> V get(String key) {
    return (V) fields.get(key);
  }

  protected void set(String key, Object value) {
    if (value == null) {
      fields.remove(key);
    } else {
      fields.put(key, value);
    }
  }

  /** Gets a field value using a FieldSpec. Delegates to {@link FieldSpec#get(Object)}. */
  @SuppressWarnings("unchecked")
  protected <V> V get(FieldSpec<? extends DataObject, V> spec) {
    return ((FieldSpec<DataObject, V>) spec).get(this);
  }

  /** Sets a field value using a FieldSpec. Delegates to {@link FieldSpec#set(Object, Object)}. */
  @SuppressWarnings("unchecked")
  protected <V> void set(FieldSpec<? extends DataObject, V> spec, V value) {
    ((FieldSpec<DataObject, V>) spec).set(this, value);
  }

  /**
   * Creates a FieldSpec backed by the object's internal map.
   *
   * @param name the field name (used as map key)
   * @param codec the codec for the field value
   * @param <T> the DataObject subclass type
   * @param <V> the field value type
   * @return a new FieldSpec
   */
  public static <T extends DataObject, V> FieldSpec<T, V> field(String name, Codec<V> codec) {
    return field(name, codec, Predicates.alwaysTrue());
  }

  /**
   * Creates a FieldSpec backed by the object's internal map with a presence predicate.
   *
   * @param name the field name (used as map key)
   * @param codec the codec for the field value
   * @param presence predicate to determine if field is present
   * @param <T> the DataObject subclass type
   * @param <V> the field value type
   * @return a new FieldSpec
   */
  public static <T extends DataObject, V> FieldSpec<T, V> field(
      String name, Codec<V> codec, Predicate<T> presence) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(codec, "codec");
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
      public V get(T obj) {
        return obj.get(name);
      }

      @Override
      public void set(T obj, V value) {
        obj.set(name, value);
      }

      @Override
      public Predicate<T> presence() {
        return presence;
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return fields.equals(((DataObject) o).fields);
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
