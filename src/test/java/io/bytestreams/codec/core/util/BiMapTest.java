package io.bytestreams.codec.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.AbstractMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BiMapTest {

  private final BiMap<Integer, String> biMap =
      BiMap.of(Map.entry(1, "one"), Map.entry(2, "two"), Map.entry(3, "three"));

  @Test
  void to() {
    assertThat(biMap.to(1)).isEqualTo("one");
    assertThat(biMap.to(2)).isEqualTo("two");
    assertThat(biMap.to(3)).isEqualTo("three");
  }

  @Test
  void from() {
    assertThat(biMap.from("one")).isEqualTo(1);
    assertThat(biMap.from("two")).isEqualTo(2);
    assertThat(biMap.from("three")).isEqualTo(3);
  }

  @Test
  void to_missing() {
    assertThatThrownBy(() -> biMap.to(99))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("no value for key: 99");
  }

  @Test
  void from_missing() {
    assertThatThrownBy(() -> biMap.from("missing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("no key for value: missing");
  }

  @Test
  void of_duplicate_key() {
    var entry1 = Map.entry(1, "one");
    var entry2 = Map.entry(1, "uno");
    assertThatThrownBy(() -> BiMap.of(entry1, entry2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("duplicate key: 1");
  }

  @Test
  void of_duplicate_value() {
    var entry1 = Map.entry(1, "one");
    var entry2 = Map.entry(2, "one");
    assertThatThrownBy(() -> BiMap.of(entry1, entry2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("duplicate value: one");
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void of_null_entry() {
    assertThatThrownBy(() -> BiMap.of((Map.Entry<Integer, String>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("entry");
  }

  @Test
  void of_null_key() {
    var entry1 = Map.entry(1, "one");
    var nullKeyEntry = new AbstractMap.SimpleEntry<Integer, String>(null, "two");
    assertThatThrownBy(() -> BiMap.of(entry1, nullKeyEntry))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("key");
  }

  @Test
  void of_null_value() {
    var entry1 = Map.entry(1, "one");
    var nullValueEntry = new AbstractMap.SimpleEntry<Integer, String>(2, null);
    assertThatThrownBy(() -> BiMap.of(entry1, nullValueEntry))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value");
  }

  @Test
  void of_empty() {
    assertThatThrownBy(BiMap::of)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("entries must not be empty");
  }
}
