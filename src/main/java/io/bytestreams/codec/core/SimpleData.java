package io.bytestreams.codec.core;

/**
 * A {@link DataObject} with public access to all fields.
 *
 * <p>Use this when you want open, untyped access without subclassing:
 *
 * <pre>{@code
 * Codec<SimpleData> codec = Codecs.<SimpleData>sequential(SimpleData::new)
 *     .field(SimpleData.field("id", Codecs.uint16()))
 *     .field(SimpleData.field("name", Codecs.ascii(20)))
 *     .build();
 *
 * SimpleData obj = codec.decode(input);
 * int id = obj.get("id");
 * }</pre>
 *
 * <p>For controlled access with typed getters/setters, subclass {@link DataObject} directly.
 */
public class SimpleData extends DataObject {

  @Override
  public <V> V get(String key) {
    return super.get(key);
  }

  @Override
  public void set(String key, Object value) {
    super.set(key, value);
  }

  @Override
  public <V> V get(FieldSpec<? extends DataObject, V> spec) {
    return super.get(spec);
  }

  @Override
  public <V> void set(FieldSpec<? extends DataObject, V> spec, V value) {
    super.set(spec, value);
  }
}
