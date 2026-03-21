package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
      FieldSpec.of("id", Codecs.uint8(), TestObject::getId, TestObject::setId);

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
    FieldSpec<TestObject, Integer> spec =
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
    assertThat(spec.presence().test(new TestObject())).isTrue();
  }

  @Test
  void get_and_set() {
    TestObject obj = new TestObject();
    ID.set(obj, 42);
    assertThat(ID.get(obj)).isEqualTo(42);
  }

  // -- FieldSpec.of() factory --

  static final FieldSpec<TestObject, Integer> ID_OF =
      FieldSpec.of("id", Codecs.uint8(), TestObject::getId, TestObject::setId);

  @Test
  void of_name() {
    assertThat(ID_OF.name()).isEqualTo("id");
  }

  @Test
  void of_codec() {
    assertThat(ID_OF.codec()).isNotNull();
  }

  @Test
  void of_get_and_set() {
    TestObject obj = new TestObject();
    ID_OF.set(obj, 42);
    assertThat(ID_OF.get(obj)).isEqualTo(42);
  }

  @Test
  void of_presence_defaults_to_always_true() {
    assertThat(ID_OF.presence().test(new TestObject())).isTrue();
  }

  @Test
  void of_with_presence() {
    FieldSpec<TestObject, Integer> spec =
        FieldSpec.of(
            "id", Codecs.uint8(), TestObject::getId, TestObject::setId, obj -> obj.getId() > 0);
    TestObject obj = new TestObject();
    assertThat(spec.presence().test(obj)).isFalse();
    obj.setId(1);
    assertThat(spec.presence().test(obj)).isTrue();
  }

  @Test
  void of_null_name_rejected() {
    Codec<Integer> codec = Codecs.uint8();
    assertThatThrownBy(() -> FieldSpec.of(null, codec, TestObject::getId, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("name");
  }

  @Test
  void of_null_codec_rejected() {
    assertThatThrownBy(() -> FieldSpec.of("id", null, TestObject::getId, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void of_null_getter_rejected() {
    Codec<Integer> codec = Codecs.uint8();
    assertThatThrownBy(() -> FieldSpec.of("id", codec, null, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("getter");
  }

  @Test
  void of_null_setter_rejected() {
    Codec<Integer> codec = Codecs.uint8();
    assertThatThrownBy(() -> FieldSpec.of("id", codec, TestObject::getId, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("setter");
  }

  @Test
  void of_null_presence_rejected() {
    Codec<Integer> codec = Codecs.uint8();
    assertThatThrownBy(() -> FieldSpec.of("id", codec, TestObject::getId, TestObject::setId, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("presence");
  }
}
