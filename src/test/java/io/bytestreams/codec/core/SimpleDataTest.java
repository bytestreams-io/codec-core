package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimpleDataTest {

  @Test
  void get_returns_null_for_absent_key() {
    SimpleData obj = new SimpleData();
    assertThat(obj.<String>get("missing")).isNull();
  }

  @Test
  void set_and_get_by_key() {
    SimpleData obj = new SimpleData();
    obj.set("name", "hello");
    assertThat(obj.<String>get("name")).isEqualTo("hello");
  }

  @Test
  void set_null_removes_key() {
    SimpleData obj = new SimpleData();
    obj.set("name", "hello");
    obj.set("name", null);
    assertThat(obj.<String>get("name")).isNull();
  }

  @Test
  void set_and_get_by_field_spec() {
    FieldSpec<SimpleData, Integer> spec = SimpleData.field("id", Codecs.uint16());
    SimpleData obj = new SimpleData();
    obj.set(spec, 42);
    assertThat(obj.get(spec)).isEqualTo(42);
  }

  @Test
  void field_spec_set_null_removes() {
    FieldSpec<SimpleData, String> spec = SimpleData.field("name", Codecs.ascii(5));
    SimpleData obj = new SimpleData();
    obj.set(spec, "hello");
    obj.set(spec, null);
    assertThat(obj.get(spec)).isNull();
  }

  @Test
  void field_spec_name_and_codec() {
    FieldSpec<SimpleData, Integer> spec = SimpleData.field("id", Codecs.uint16());
    assertThat(spec.name()).isEqualTo("id");
    assertThat(spec.codec()).isNotNull();
  }

  @Test
  void equals_same_instance() {
    SimpleData obj = new SimpleData();
    obj.set("id", 1);
    assertThat(obj).isEqualTo(obj);
  }

  @Test
  void equals_same_content() {
    SimpleData a = new SimpleData();
    a.set("id", 1);
    SimpleData b = new SimpleData();
    b.set("id", 1);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equals_different_content() {
    SimpleData a = new SimpleData();
    a.set("id", 1);
    SimpleData b = new SimpleData();
    b.set("id", 2);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void equals_not_equal_to_other_type() {
    SimpleData obj = new SimpleData();
    obj.set("id", 1);
    assertThat(obj).isNotEqualTo("not a SimpleData");
  }

  @Test
  void equals_empty() {
    assertThat(new SimpleData()).isEqualTo(new SimpleData());
  }

  @Test
  void to_string_delegates_to_map() {
    SimpleData obj = new SimpleData();
    obj.set("id", 1);
    assertThat(obj).hasToString("{id=1}");
  }
}
