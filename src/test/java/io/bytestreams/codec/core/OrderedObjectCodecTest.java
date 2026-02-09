package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

class OrderedObjectCodecTest {

  @Test
  void roundtrip_required_fields_only() throws IOException {
    OrderedObjectCodec<TestObject> codec =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId)
            .field(
                "name",
                new CodePointStringCodec(5, UTF_8),
                TestObject::getName,
                TestObject::setName)
            .supplier(TestObject::new)
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
    OrderedObjectCodec<TestObject> codec =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId)
            .field(
                "tag",
                new CodePointStringCodec(3, UTF_8),
                TestObject::getTag,
                TestObject::setTag,
                obj -> obj.getId() > 0)
            .supplier(TestObject::new)
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
    OrderedObjectCodec<TestObject> codec =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId)
            .field(
                "tag",
                new CodePointStringCodec(3, UTF_8),
                TestObject::getTag,
                TestObject::setTag,
                obj -> obj.getId() > 0)
            .supplier(TestObject::new)
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
    OrderedObjectCodec<TestObject> codec =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId)
            .field("data", new BinaryCodec(5), obj -> new byte[3], (obj, v) -> {})
            .supplier(TestObject::new)
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
    OrderedObjectCodec<TestObject> codec =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId)
            .supplier(TestObject::new)
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("id")
        .hasCauseInstanceOf(EOFException.class);
  }

  @Test
  void decode_insufficient_data() {
    OrderedObjectCodec<TestObject> codec =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId)
            .field(
                "name",
                new CodePointStringCodec(5, UTF_8),
                TestObject::getName,
                TestObject::setName)
            .supplier(TestObject::new)
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
    OrderedObjectCodec.Builder<TestObject> builder = OrderedObjectCodec.builder();
    Codec<Integer> codec = new UnsignedShortCodec();

    assertThatThrownBy(() -> builder.field(null, codec, TestObject::getId, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("name");
  }

  @Test
  void builder_null_codec() {
    OrderedObjectCodec.Builder<TestObject> builder = OrderedObjectCodec.builder();

    assertThatThrownBy(() -> builder.field("id", null, TestObject::getId, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void builder_null_getter() {
    OrderedObjectCodec.Builder<TestObject> builder = OrderedObjectCodec.builder();
    Codec<Integer> codec = new UnsignedShortCodec();

    assertThatThrownBy(() -> builder.field("id", codec, null, TestObject::setId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("getter");
  }

  @Test
  void builder_null_setter() {
    OrderedObjectCodec.Builder<TestObject> builder = OrderedObjectCodec.builder();
    Codec<Integer> codec = new UnsignedShortCodec();

    assertThatThrownBy(() -> builder.field("id", codec, TestObject::getId, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("setter");
  }

  @Test
  void builder_null_presence() {
    OrderedObjectCodec.Builder<TestObject> builder = OrderedObjectCodec.builder();
    Codec<Integer> codec = new UnsignedShortCodec();

    assertThatThrownBy(() -> builder.field("id", codec, TestObject::getId, TestObject::setId, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("presence");
  }

  @Test
  void builder_null_supplier() {
    OrderedObjectCodec.Builder<TestObject> builder =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId);

    assertThatThrownBy(() -> builder.supplier(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("supplier");
  }

  @Test
  void builder_missing_supplier() {
    OrderedObjectCodec.Builder<TestObject> builder =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId);

    assertThatThrownBy(builder::build)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("supplier");
  }

  @Test
  void builder_no_fields() {
    OrderedObjectCodec.Builder<TestObject> builder =
        OrderedObjectCodec.<TestObject>builder().supplier(TestObject::new);

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one field");
  }

  @Test
  void supplier_returns_null() {
    OrderedObjectCodec<TestObject> codec =
        OrderedObjectCodec.<TestObject>builder()
            .field("id", new UnsignedShortCodec(), TestObject::getId, TestObject::setId)
            .supplier(() -> null)
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0, 1});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("supplier.get()");
  }

  @Test
  void decode_nested_codec_error_includes_full_field_path() {
    OrderedObjectCodec<InnerObject> innerCodec =
        OrderedObjectCodec.<InnerObject>builder()
            .field("value", new UnsignedByteCodec(), InnerObject::getValue, InnerObject::setValue)
            .supplier(InnerObject::new)
            .build();

    OrderedObjectCodec<OuterObject> outerCodec =
        OrderedObjectCodec.<OuterObject>builder()
            .field("id", new UnsignedShortCodec(), OuterObject::getId, OuterObject::setId)
            .field("inner", innerCodec, OuterObject::getInner, OuterObject::setInner)
            .supplier(OuterObject::new)
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
          public void encode(Integer value, OutputStream output) {
            throw new CodecException("inner encode failed", null);
          }

          @Override
          public Integer decode(InputStream input) {
            return 0;
          }
        };

    OrderedObjectCodec<InnerObject> innerCodec =
        OrderedObjectCodec.<InnerObject>builder()
            .field("value", throwingCodec, InnerObject::getValue, InnerObject::setValue)
            .supplier(InnerObject::new)
            .build();

    OrderedObjectCodec<OuterObject> outerCodec =
        OrderedObjectCodec.<OuterObject>builder()
            .field("id", new UnsignedShortCodec(), OuterObject::getId, OuterObject::setId)
            .field("inner", innerCodec, OuterObject::getInner, OuterObject::setInner)
            .supplier(OuterObject::new)
            .build();

    OuterObject obj = new OuterObject();
    obj.setId(1);
    obj.setInner(new InnerObject());

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> outerCodec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("inner.value");
  }

  @Test
  void nested_object_codec() throws IOException {
    OrderedObjectCodec<InnerObject> innerCodec =
        OrderedObjectCodec.<InnerObject>builder()
            .field("value", new UnsignedByteCodec(), InnerObject::getValue, InnerObject::setValue)
            .supplier(InnerObject::new)
            .build();

    OrderedObjectCodec<OuterObject> outerCodec =
        OrderedObjectCodec.<OuterObject>builder()
            .field("id", new UnsignedShortCodec(), OuterObject::getId, OuterObject::setId)
            .field("inner", innerCodec, OuterObject::getInner, OuterObject::setInner)
            .supplier(OuterObject::new)
            .build();

    OuterObject original = new OuterObject();
    original.setId(999);
    InnerObject inner = new InnerObject();
    inner.setValue(42);
    original.setInner(inner);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    outerCodec.encode(original, output);

    OuterObject decoded = outerCodec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.getId()).isEqualTo(999);
    assertThat(decoded.getInner().getValue()).isEqualTo(42);
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

  static class InnerObject {
    private int value;

    public int getValue() {
      return value;
    }

    public void setValue(int value) {
      this.value = value;
    }
  }

  static class OuterObject {
    private int id;
    private InnerObject inner;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public InnerObject getInner() {
      return inner;
    }

    public void setInner(InnerObject inner) {
      this.inner = inner;
    }
  }
}
