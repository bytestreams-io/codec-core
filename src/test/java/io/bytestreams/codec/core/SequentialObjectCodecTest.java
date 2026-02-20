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
    SequentialObjectCodec<InnerObject> innerCodec =
        SequentialObjectCodec.<InnerObject>builder(InnerObject::new)
            .field("value", Codecs.uint8(), InnerObject::getValue, InnerObject::setValue)
            .build();

    SequentialObjectCodec<OuterObject> outerCodec =
        SequentialObjectCodec.<OuterObject>builder(OuterObject::new)
            .field("id", Codecs.uint16(), OuterObject::getId, OuterObject::setId)
            .field("inner", innerCodec, OuterObject::getInner, OuterObject::setInner)
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

    SequentialObjectCodec<InnerObject> innerCodec =
        SequentialObjectCodec.<InnerObject>builder(InnerObject::new)
            .field("value", throwingCodec, InnerObject::getValue, InnerObject::setValue)
            .build();

    SequentialObjectCodec<OuterObject> outerCodec =
        SequentialObjectCodec.<OuterObject>builder(OuterObject::new)
            .field("id", Codecs.uint16(), OuterObject::getId, OuterObject::setId)
            .field("inner", innerCodec, OuterObject::getInner, OuterObject::setInner)
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
    SequentialObjectCodec<InnerObject> innerCodec =
        SequentialObjectCodec.<InnerObject>builder(InnerObject::new)
            .field("value", Codecs.uint8(), InnerObject::getValue, InnerObject::setValue)
            .build();

    SequentialObjectCodec<OuterObject> outerCodec =
        SequentialObjectCodec.<OuterObject>builder(OuterObject::new)
            .field("id", Codecs.uint16(), OuterObject::getId, OuterObject::setId)
            .field("inner", innerCodec, OuterObject::getInner, OuterObject::setInner)
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
