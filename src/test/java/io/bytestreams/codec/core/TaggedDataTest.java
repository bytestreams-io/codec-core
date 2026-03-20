package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaggedDataTest {

  @Test
  void tags_empty_initially() {
    TaggedData<String> obj = new TaggedData<>();
    assertThat(obj.tags()).isEmpty();
  }

  @Test
  void add_and_get_all() {
    TaggedData<String> obj = new TaggedData<>();
    obj.add("code", 42);
    assertThat(obj.<Integer>getAll("code")).containsExactly(42);
  }

  @Test
  void add_multiple_values_same_tag() {
    TaggedData<String> obj = new TaggedData<>();
    obj.add("code", 1);
    obj.add("code", 2);
    obj.add("code", 3);
    assertThat(obj.<Integer>getAll("code")).containsExactly(1, 2, 3);
  }

  @Test
  void get_all_absent_tag_returns_empty() {
    TaggedData<String> obj = new TaggedData<>();
    assertThat(obj.getAll("missing")).isEmpty();
  }

  @Test
  void tags_preserves_insertion_order() {
    TaggedData<String> obj = new TaggedData<>();
    obj.add("b", 1);
    obj.add("a", 2);
    obj.add("c", 3);
    assertThat(obj.tags()).containsExactly("b", "a", "c");
  }

  @Test
  void add_returns_this_for_chaining() {
    TaggedData<String> obj = new TaggedData<>();
    TaggedData<String> result = obj.add("a", 1).add("b", 2);
    assertThat(result).isSameAs(obj);
  }

  @Test
  void equals_same_instance() {
    TaggedData<String> obj = new TaggedData<>();
    obj.add("code", 42);
    assertThat(obj).isEqualTo(obj);
  }

  @Test
  void equals_same_content() {
    TaggedData<String> a = new TaggedData<>();
    a.add("code", 42);
    TaggedData<String> b = new TaggedData<>();
    b.add("code", 42);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equals_different_content() {
    TaggedData<String> a = new TaggedData<>();
    a.add("code", 1);
    TaggedData<String> b = new TaggedData<>();
    b.add("code", 2);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void equals_not_equal_to_other_type() {
    TaggedData<String> obj = new TaggedData<>();
    obj.add("code", 1);
    assertThat(obj).isNotEqualTo("not a TaggedData");
  }

  @Test
  void equals_empty() {
    assertThat(new TaggedData<String>()).isEqualTo(new TaggedData<String>());
  }

  @Test
  void to_string_delegates_to_map() {
    TaggedData<String> obj = new TaggedData<>();
    obj.add("code", 42);
    assertThat(obj).hasToString("{code=[42]}");
  }
}
