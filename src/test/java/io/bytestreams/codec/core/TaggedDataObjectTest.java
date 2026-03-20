package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaggedDataObjectTest {

  @Test
  void tags_empty_initially() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    assertThat(obj.tags()).isEmpty();
  }

  @Test
  void add_and_get_all() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    obj.add("code", 42);
    assertThat(obj.<Integer>getAll("code")).containsExactly(42);
  }

  @Test
  void add_multiple_values_same_tag() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    obj.add("code", 1);
    obj.add("code", 2);
    obj.add("code", 3);
    assertThat(obj.<Integer>getAll("code")).containsExactly(1, 2, 3);
  }

  @Test
  void get_all_absent_tag_returns_empty() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    assertThat(obj.getAll("missing")).isEmpty();
  }

  @Test
  void tags_preserves_insertion_order() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    obj.add("b", 1);
    obj.add("a", 2);
    obj.add("c", 3);
    assertThat(obj.tags()).containsExactly("b", "a", "c");
  }

  @Test
  void add_returns_this_for_chaining() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    TaggedDataObject<String> result = obj.add("a", 1).add("b", 2);
    assertThat(result).isSameAs(obj);
  }

  @Test
  void equals_same_instance() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    obj.add("code", 42);
    assertThat(obj).isEqualTo(obj);
  }

  @Test
  void equals_same_content() {
    TaggedDataObject<String> a = new TaggedDataObject<>();
    a.add("code", 42);
    TaggedDataObject<String> b = new TaggedDataObject<>();
    b.add("code", 42);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equals_different_content() {
    TaggedDataObject<String> a = new TaggedDataObject<>();
    a.add("code", 1);
    TaggedDataObject<String> b = new TaggedDataObject<>();
    b.add("code", 2);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void equals_not_equal_to_other_type() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    obj.add("code", 1);
    assertThat(obj).isNotEqualTo("not a TaggedDataObject");
  }

  @Test
  void equals_empty() {
    assertThat(new TaggedDataObject<String>()).isEqualTo(new TaggedDataObject<String>());
  }

  @Test
  void to_string_delegates_to_map() {
    TaggedDataObject<String> obj = new TaggedDataObject<>();
    obj.add("code", 42);
    assertThat(obj).hasToString("{code=[42]}");
  }
}
