package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SequentialObjectCodecTest {

  @Test
  void roundtrip_required_fields_only() throws IOException {
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field("id", Codecs.uint16(), TestObject::getId, TestObject::setId)
            .field(
                "name",
                Codecs.ofCharset(Charset.defaultCharset(), 5),
                TestObject::getName,
                TestObject::setName)
            .build();

    TestObject original = new TestObject();
    original.setId(42);
    original.setName("hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestObject decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.getId()).isEqualTo(42);
    assertThat(decoded.getName()).isEqualTo("hello");
  }

  @Test
  void roundtrip_optional_field_present() throws IOException {
    // Presence predicate must check an EARLIER field, not the optional field itself
    // Here, id > 0 indicates tag is present
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field("id", Codecs.uint16(), TestObject::getId, TestObject::setId)
            .field(
                "tag",
                Codecs.ofCharset(Charset.defaultCharset(), 3),
                TestObject::getTag,
                TestObject::setTag,
                obj -> obj.getId() > 0)
            .build();

    TestObject original = new TestObject();
    original.setId(100);
    original.setTag("abc");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestObject decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.getId()).isEqualTo(100);
    assertThat(decoded.getTag()).isEqualTo("abc");
  }

  @Test
  void roundtrip_optional_field_absent() throws IOException {
    // Presence predicate must check an EARLIER field, not the optional field itself
    // Here, id = 0 indicates tag is absent
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field("id", Codecs.uint16(), TestObject::getId, TestObject::setId)
            .field(
                "tag",
                Codecs.ofCharset(Charset.defaultCharset(), 3),
                TestObject::getTag,
                TestObject::setTag,
                obj -> obj.getId() > 0)
            .build();

    TestObject original = new TestObject();
    original.setId(0);
    original.setTag(null);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    // Should only have 2 bytes (id), no tag bytes
    assertThat(output.toByteArray()).hasSize(2);

    TestObject decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.getId()).isZero();
    assertThat(decoded.getTag()).isNull();
  }

  @Test
  void encode_error_includes_field_name() {
    // BinaryCodec throws IllegalArgumentException when length doesn't match
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field("id", Codecs.uint16(), TestObject::getId, TestObject::setId)
            .field("data", new BinaryCodec(5), obj -> new byte[3], (obj, v) -> {})
            .build();

    TestObject obj = new TestObject();
    obj.setId(1);

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("data");
  }

  @Test
  void decode_empty_stream() {
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field("id", Codecs.uint16(), TestObject::getId, TestObject::setId)
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("id")
        .hasCauseInstanceOf(EOFException.class);
  }

  @Test
  void decode_insufficient_data() {
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field("id", Codecs.uint16(), TestObject::getId, TestObject::setId)
            .field(
                "name",
                Codecs.ofCharset(Charset.defaultCharset(), 5),
                TestObject::getName,
                TestObject::setName)
            .build();

    // Only 2 bytes for id, missing name
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0, 1});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("name")
        .hasCauseInstanceOf(EOFException.class);
  }

  @Test
  void builder_null_name() {
    SequentialObjectCodec.Builder<TestObject> builder =
        SequentialObjectCodec.builder(TestObject::new);
    Codec<Integer> codec = Codecs.uint16();

    assertThatThrownBy(() -> builder.field(null, codec, TestObject::getId, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("name");
  }

  @Test
  void builder_null_codec() {
    SequentialObjectCodec.Builder<TestObject> builder =
        SequentialObjectCodec.builder(TestObject::new);

    assertThatThrownBy(() -> builder.field("id", null, TestObject::getId, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void builder_null_getter() {
    SequentialObjectCodec.Builder<TestObject> builder =
        SequentialObjectCodec.builder(TestObject::new);
    Codec<Integer> codec = Codecs.uint16();

    assertThatThrownBy(() -> builder.field("id", codec, null, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("getter");
  }

  @Test
  void builder_null_setter() {
    SequentialObjectCodec.Builder<TestObject> builder =
        SequentialObjectCodec.builder(TestObject::new);
    Codec<Integer> codec = Codecs.uint16();

    assertThatThrownBy(() -> builder.field("id", codec, TestObject::getId, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("setter");
  }

  @Test
  void builder_null_presence() {
    SequentialObjectCodec.Builder<TestObject> builder =
        SequentialObjectCodec.builder(TestObject::new);
    Codec<Integer> codec = Codecs.uint16();

    assertThatThrownBy(() -> builder.field("id", codec, TestObject::getId, TestObject::setId, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("presence");
  }

  @Test
  void builder_null_factory() {
    assertThatThrownBy(() -> SequentialObjectCodec.builder(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("factory");
  }

  @Test
  void builder_no_fields() {
    SequentialObjectCodec.Builder<TestObject> builder =
        SequentialObjectCodec.<TestObject>builder(TestObject::new);

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one field");
  }

  @Test
  void factory_returns_null() {
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(() -> null)
            .field("id", Codecs.uint16(), TestObject::getId, TestObject::setId)
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0, 1});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("factory.get()");
  }

  @Test
  void decode_nested_codec_error_includes_full_field_path() {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
            .field(
                "value", Codecs.uint8(), TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();

    SequentialObjectCodec<TestFixtures.Outer> outerCodec =
        SequentialObjectCodec.<TestFixtures.Outer>builder(TestFixtures.Outer::new)
            .field("id", Codecs.uint16(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
            .field("inner", innerCodec, TestFixtures.Outer::getInner, TestFixtures.Outer::setInner)
            .build();

    // Only provide id bytes, not inner.value
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0, 1});

    assertThatThrownBy(() -> outerCodec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("inner.value")
        .hasCauseInstanceOf(EOFException.class);
  }

  @Test
  void encode_nested_codec_error_includes_full_field_path() {
    // Inner codec that always throws CodecException on encode
    Codec<Integer> throwingCodec =
        new Codec<>() {
          @Override
          public EncodeResult encode(Integer value, OutputStream output) throws IOException {
            throw new CodecException("inner encode failed", null);
          }

          @Override
          public Integer decode(InputStream input) {
            return 0;
          }
        };

    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
            .field(
                "value", throwingCodec, TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();

    SequentialObjectCodec<TestFixtures.Outer> outerCodec =
        SequentialObjectCodec.<TestFixtures.Outer>builder(TestFixtures.Outer::new)
            .field("id", Codecs.uint16(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
            .field("inner", innerCodec, TestFixtures.Outer::getInner, TestFixtures.Outer::setInner)
            .build();

    TestFixtures.Outer obj = new TestFixtures.Outer();
    obj.setId(1);
    obj.setInner(new TestFixtures.Inner());

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> outerCodec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("inner.value");
  }

  @Test
  void nested_object_codec() throws IOException {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
            .field(
                "value", Codecs.uint8(), TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();

    SequentialObjectCodec<TestFixtures.Outer> outerCodec =
        SequentialObjectCodec.<TestFixtures.Outer>builder(TestFixtures.Outer::new)
            .field("id", Codecs.uint16(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
            .field("inner", innerCodec, TestFixtures.Outer::getInner, TestFixtures.Outer::setInner)
            .build();

    TestFixtures.Outer original = new TestFixtures.Outer();
    original.setId(999);
    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(42);
    original.setInner(inner);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    outerCodec.encode(original, output);

    TestFixtures.Outer decoded = outerCodec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.getId()).isEqualTo(999);
    assertThat(decoded.getInner().getValue()).isEqualTo(42);
  }

  // FieldSpec overload tests

  static final FieldSpec<TestObject, Integer> ID_SPEC =
      FieldSpec.of("id", Codecs.uint16(), TestObject::getId, TestObject::setId);

  static final FieldSpec<TestObject, String> NAME_SPEC =
      FieldSpec.of(
          "name",
          Codecs.ofCharset(Charset.defaultCharset(), 5),
          TestObject::getName,
          TestObject::setName);

  static final FieldSpec<TestObject, String> TAG_SPEC =
      FieldSpec.of(
          "tag",
          Codecs.ofCharset(Charset.defaultCharset(), 3),
          TestObject::getTag,
          TestObject::setTag,
          obj -> obj.getId() > 0);

  @Test
  void encode_with_field_spec() throws IOException {
    TestObject original = new TestObject();
    original.setId(42);
    original.setName("hello");

    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field(ID_SPEC)
            .field(NAME_SPEC)
            .build();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestObject decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.getId()).isEqualTo(42);
    assertThat(decoded.getName()).isEqualTo("hello");
  }

  @Test
  void encode_with_field_spec_and_presence() throws IOException {
    TestObject present = new TestObject();
    present.setId(1);
    present.setTag("abc");

    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field(ID_SPEC)
            .field(TAG_SPEC)
            .build();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(present, output);

    TestObject decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.getId()).isEqualTo(1);
    assertThat(decoded.getTag()).isEqualTo("abc");
  }

  @Test
  void encode_with_field_spec_and_presence_absent() throws IOException {
    TestObject absent = new TestObject();
    absent.setId(0);

    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field(ID_SPEC)
            .field(TAG_SPEC)
            .build();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(absent, output);

    TestObject decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.getId()).isZero();
    assertThat(decoded.getTag()).isNull();
  }

  @Test
  void field_spec_null_rejected() {
    SequentialObjectCodec.Builder<TestObject> builder =
        SequentialObjectCodec.builder(TestObject::new);
    assertThatThrownBy(() -> builder.field((FieldSpec<TestObject, ?>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("spec");
  }

  @Test
  void inspect_returns_map_of_field_values() {
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field("name", Codecs.ascii(5), TestObject::getName, TestObject::setName)
            .field("id", Codecs.uint8(), TestObject::getId, TestObject::setId)
            .build();

    TestObject obj = new TestObject();
    obj.setName("Alice");
    obj.setId(30);

    Object result = Inspector.inspect(codec, obj);

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("name", "Alice");
    expected.put("id", 30);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void inspect_skips_absent_fields() {
    SequentialObjectCodec<TestObject> codec =
        SequentialObjectCodec.<TestObject>builder(TestObject::new)
            .field("name", Codecs.ascii(5), TestObject::getName, TestObject::setName)
            .field("id", Codecs.uint8(), TestObject::getId, TestObject::setId)
            .field(
                "tag",
                Codecs.ascii(3),
                TestObject::getTag,
                TestObject::setTag,
                obj -> obj.getId() > 0)
            .build();

    TestObject obj = new TestObject();
    obj.setName("Alice");
    obj.setId(0);
    obj.setTag("xyz");

    Object result = Inspector.inspect(codec, obj);

    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) result;
    assertThat(map).containsKeys("name", "id").doesNotContainKey("tag");
  }

  @Test
  void inspect_recurses_into_nested_sequential() {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
            .field(
                "value", Codecs.uint8(), TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();

    SequentialObjectCodec<TestFixtures.Outer> outerCodec =
        SequentialObjectCodec.<TestFixtures.Outer>builder(TestFixtures.Outer::new)
            .field("id", Codecs.uint8(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
            .field("inner", innerCodec, TestFixtures.Outer::getInner, TestFixtures.Outer::setInner)
            .build();

    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(42);

    TestFixtures.Outer outer = new TestFixtures.Outer();
    outer.setId(1);
    outer.setInner(inner);

    Object result = Inspector.inspect(outerCodec, outer);

    Map<String, Object> expectedInner = new LinkedHashMap<>();
    expectedInner.put("value", 42);

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("id", 1);
    expected.put("inner", expectedInner);

    assertThat(result).isEqualTo(expected);
  }

  // Test helper classes

  static class TestObject {
    private int id;
    private String name;
    private String tag;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }
  }

  // Specs and functions declared on a supertype

  static class Parent {
    int id;
    String note;

    int getId() {
      return id;
    }

    void setId(int id) {
      this.id = id;
    }
  }

  static class Child extends Parent {
    int extra;
  }

  static class Left extends Parent {}

  static class Right extends Parent {}

  static final FieldSpec<Parent, Integer> PARENT_ID =
      FieldSpec.of("id", Codecs.uint8(), p -> p.id, (p, v) -> p.id = v);

  static final FieldSpec<Parent, String> PARENT_NOTE =
      FieldSpec.of("note", Codecs.ascii(3), p -> p.note, (p, v) -> p.note = v, p -> p.id > 0);

  private static SequentialObjectCodec<Child> childCodec() {
    return SequentialObjectCodec.<Child>builder(Child::new)
        .field(PARENT_ID)
        .field(PARENT_NOTE)
        .field("extra", Codecs.uint8(), c -> c.extra, (c, v) -> c.extra = v)
        .build();
  }

  private static <T> byte[] encode(SequentialObjectCodec<T> codec, T value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    return output.toByteArray();
  }

  @Test
  void supertype_specs_encode_and_decode() throws IOException {
    Child original = new Child();
    original.id = 7;
    original.note = "abc";
    original.extra = 9;

    byte[] bytes = encode(childCodec(), original);
    Child decoded = childCodec().decode(new ByteArrayInputStream(bytes));

    assertThat(bytes).containsExactly(7, 'a', 'b', 'c', 9);
    assertThat(decoded.id).isEqualTo(7);
    assertThat(decoded.note).isEqualTo("abc");
    assertThat(decoded.extra).isEqualTo(9);
  }

  @Test
  void supertype_presence_predicate_skips_field_when_false() throws IOException {
    Child original = new Child();
    original.extra = 9;

    byte[] bytes = encode(childCodec(), original);
    Child decoded = childCodec().decode(new ByteArrayInputStream(bytes));

    assertThat(bytes).containsExactly(0, 9);
    assertThat(decoded.note).isNull();
    assertThat(decoded.extra).isEqualTo(9);
  }

  @Test
  void supertype_specs_inspect() {
    Child present = new Child();
    present.id = 7;
    present.note = "abc";
    present.extra = 9;

    assertThat(childCodec().inspect(present)).isEqualTo(Map.of("id", 7, "note", "abc", "extra", 9));
    assertThat(childCodec().inspect(new Child())).isEqualTo(Map.of("id", 0, "extra", 0));
  }

  @Test
  void supertype_explicit_getter_setter_and_presence() throws IOException {
    SequentialObjectCodec<Child> codec =
        SequentialObjectCodec.<Child>builder(Child::new)
            .field("id", Codecs.uint8(), (Parent p) -> p.id, (Parent p, Integer v) -> p.id = v)
            .field(
                "note",
                Codecs.ascii(3),
                (Parent p) -> p.note,
                (Parent p, String v) -> p.note = v,
                (Parent p) -> p.id > 0)
            .build();
    Child original = new Child();
    original.id = 1;
    original.note = "xyz";

    Child decoded = codec.decode(new ByteArrayInputStream(encode(codec, original)));

    assertThat(decoded.note).isEqualTo("xyz");
  }

  @Test
  void one_supertype_spec_is_shared_by_sibling_subclass_codecs() throws IOException {
    SequentialObjectCodec<Left> left =
        SequentialObjectCodec.<Left>builder(Left::new).field(PARENT_ID).build();
    SequentialObjectCodec<Right> right =
        SequentialObjectCodec.<Right>builder(Right::new)
            .field("id", Codecs.uint8(), Parent::getId, Parent::setId)
            .build();
    Left l = new Left();
    l.id = 3;
    Right r = new Right();
    r.id = 4;

    assertThat(left.decode(new ByteArrayInputStream(encode(left, l))).id).isEqualTo(3);
    assertThat(right.decode(new ByteArrayInputStream(encode(right, r))).id).isEqualTo(4);
  }
}
