package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimpleDataObjectTest {

  @Test
  void get_returns_null_for_absent_key() {
    SimpleDataObject obj = new SimpleDataObject();
    assertThat(obj.<String>get("missing")).isNull();
  }

  @Test
  void set_and_get_by_key() {
    SimpleDataObject obj = new SimpleDataObject();
    obj.set("name", "hello");
    assertThat(obj.<String>get("name")).isEqualTo("hello");
  }

  @Test
  void set_null_removes_key() {
    SimpleDataObject obj = new SimpleDataObject();
    obj.set("name", "hello");
    obj.set("name", null);
    assertThat(obj.<String>get("name")).isNull();
  }

  @Test
  void set_and_get_by_field_spec() {
    FieldSpec<SimpleDataObject, Integer> spec = SimpleDataObject.field("id", Codecs.uint16());
    SimpleDataObject obj = new SimpleDataObject();
    obj.set(spec, 42);
    assertThat(obj.get(spec)).isEqualTo(42);
  }

  @Test
  void field_spec_set_null_removes() {
    FieldSpec<SimpleDataObject, String> spec = SimpleDataObject.field("name", Codecs.ascii(5));
    SimpleDataObject obj = new SimpleDataObject();
    obj.set(spec, "hello");
    obj.set(spec, null);
    assertThat(obj.get(spec)).isNull();
  }

  @Test
  void field_spec_name_and_codec() {
    FieldSpec<SimpleDataObject, Integer> spec = SimpleDataObject.field("id", Codecs.uint16());
    assertThat(spec.name()).isEqualTo("id");
    assertThat(spec.codec()).isNotNull();
  }

  @Test
  void equals_same_instance() {
    SimpleDataObject obj = new SimpleDataObject();
    obj.set("id", 1);
    assertThat(obj).isEqualTo(obj);
  }

  @Test
  void equals_same_content() {
    SimpleDataObject a = new SimpleDataObject();
    a.set("id", 1);
    SimpleDataObject b = new SimpleDataObject();
    b.set("id", 1);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equals_different_content() {
    SimpleDataObject a = new SimpleDataObject();
    a.set("id", 1);
    SimpleDataObject b = new SimpleDataObject();
    b.set("id", 2);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void equals_not_equal_to_other_type() {
    SimpleDataObject obj = new SimpleDataObject();
    obj.set("id", 1);
    assertThat(obj).isNotEqualTo("not a SimpleDataObject");
  }

  @Test
  void equals_empty() {
    assertThat(new SimpleDataObject()).isEqualTo(new SimpleDataObject());
  }

  @Test
  void to_string_delegates_to_map() {
    SimpleDataObject obj = new SimpleDataObject();
    obj.set("id", 1);
    assertThat(obj).hasToString("{id=1}");
  }
}
