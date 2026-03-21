package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IntrospectableTest {

  @Test
  void sequential_inspect_returns_map_of_field_values() {
    SequentialObjectCodec<TestMessage> codec =
        SequentialObjectCodec.<TestMessage>builder(TestMessage::new)
            .field("name", Codecs.ascii(5), TestMessage::getName, TestMessage::setName)
            .field("age", Codecs.uint8(), TestMessage::getAge, TestMessage::setAge)
            .build();

    TestMessage msg = new TestMessage();
    msg.setName("Alice");
    msg.setAge(30);

    Object result = codec.inspect(msg);

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("name", "Alice");
    expected.put("age", 30);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void sequential_inspect_skips_absent_fields() {
    SequentialObjectCodec<TestMessage> codec =
        SequentialObjectCodec.<TestMessage>builder(TestMessage::new)
            .field("name", Codecs.ascii(5), TestMessage::getName, TestMessage::setName)
            .field("age", Codecs.uint8(), TestMessage::getAge, TestMessage::setAge)
            .field(
                "tag",
                Codecs.ascii(3),
                TestMessage::getTag,
                TestMessage::setTag,
                msg -> msg.getAge() > 0)
            .build();

    TestMessage msg = new TestMessage();
    msg.setName("Alice");
    msg.setAge(0);
    msg.setTag("xyz");

    Object result = codec.inspect(msg);

    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) result;
    assertThat(map).containsKeys("name", "age");
    assertThat(map).doesNotContainKey("tag");
  }

  @Test
  void sequential_inspect_recurses_into_nested_sequential() {
    SequentialObjectCodec<Inner> innerCodec =
        SequentialObjectCodec.<Inner>builder(Inner::new)
            .field("value", Codecs.uint8(), Inner::getValue, Inner::setValue)
            .build();

    SequentialObjectCodec<Outer> outerCodec =
        SequentialObjectCodec.<Outer>builder(Outer::new)
            .field("id", Codecs.uint8(), Outer::getId, Outer::setId)
            .field("inner", innerCodec, Outer::getInner, Outer::setInner)
            .build();

    Inner inner = new Inner();
    inner.setValue(42);

    Outer outer = new Outer();
    outer.setId(1);
    outer.setInner(inner);

    Object result = outerCodec.inspect(outer);

    Map<String, Object> expectedInner = new LinkedHashMap<>();
    expectedInner.put("value", 42);

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("id", 1);
    expected.put("inner", expectedInner);

    assertThat(result).isEqualTo(expected);
  }

  static class TestMessage {
    private String name;
    private int age;
    private String tag;

    String getName() {
      return name;
    }

    void setName(String name) {
      this.name = name;
    }

    int getAge() {
      return age;
    }

    void setAge(int age) {
      this.age = age;
    }

    String getTag() {
      return tag;
    }

    void setTag(String tag) {
      this.tag = tag;
    }
  }

  static class Inner {
    private int value;

    int getValue() {
      return value;
    }

    void setValue(int value) {
      this.value = value;
    }
  }

  static class Outer {
    private int id;
    private Inner inner;

    int getId() {
      return id;
    }

    void setId(int id) {
      this.id = id;
    }

    Inner getInner() {
      return inner;
    }

    void setInner(Inner inner) {
      this.inner = inner;
    }
  }
}
