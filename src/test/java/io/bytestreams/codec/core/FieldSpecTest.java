package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FieldSpecTest {

  static class TestObject {
    private int id;

    int getId() {
      return id;
    }

    void setId(int id) {
      this.id = id;
    }
  }

  static final FieldSpec<TestObject, Integer> ID =
      new FieldSpec<>() {
        @Override
        public String name() {
          return "id";
        }

        @Override
        public Codec<Integer> codec() {
          return Codecs.uint8();
        }

        @Override
        public Integer get(TestObject object) {
          return object.getId();
        }

        @Override
        public void set(TestObject object, Integer value) {
          object.setId(value);
        }
      };

  @Test
  void name() {
    assertThat(ID.name()).isEqualTo("id");
  }

  @Test
  void codec() {
    assertThat(ID.codec()).isNotNull();
  }

  @Test
  void presence_defaults_to_always_true() {
    assertThat(ID.presence().test(new TestObject())).isTrue();
  }

  @Test
  void get_and_set() {
    TestObject obj = new TestObject();
    ID.set(obj, 42);
    assertThat(ID.get(obj)).isEqualTo(42);
  }
}
