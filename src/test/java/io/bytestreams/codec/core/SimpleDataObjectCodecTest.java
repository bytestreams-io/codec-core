package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class SimpleDataObjectCodecTest {

  @Test
  void encode_decode_with_inline_field_specs() throws IOException {
    Codec<SimpleDataObject> codec =
        Codecs.<SimpleDataObject>sequential(SimpleDataObject::new)
            .field(SimpleDataObject.field("id", Codecs.uint16()))
            .field(SimpleDataObject.field("name", Codecs.ascii(5)))
            .build();

    SimpleDataObject original = new SimpleDataObject();
    original.set("id", 42);
    original.set("name", "hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    SimpleDataObject decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.<Integer>get("id")).isEqualTo(42);
    assertThat(decoded.<String>get("name")).isEqualTo("hello");
  }

  @Test
  void encode_decode_with_subclass() throws IOException {
    Codec<TestMessage> codec =
        Codecs.<TestMessage>sequential(TestMessage::new)
            .field(TestMessage.ID)
            .field(TestMessage.NAME)
            .build();

    TestMessage original = new TestMessage();
    original.setId(42);
    original.setName("hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestMessage decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.getId()).isEqualTo(42);
    assertThat(decoded.getName()).isEqualTo("hello");
  }

  @Test
  void encode_decode_equals() throws IOException {
    Codec<TestMessage> codec =
        Codecs.<TestMessage>sequential(TestMessage::new)
            .field(TestMessage.ID)
            .field(TestMessage.NAME)
            .build();

    TestMessage original = new TestMessage();
    original.setId(42);
    original.setName("hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestMessage decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded).isEqualTo(original);
  }

  static class TestMessage extends SimpleDataObject {
    static final FieldSpec<TestMessage, Integer> ID = field("id", Codecs.uint16());
    static final FieldSpec<TestMessage, String> NAME = field("name", Codecs.ascii(5));

    public int getId() {
      return get(ID);
    }

    public void setId(int id) {
      set(ID, id);
    }

    public String getName() {
      return get(NAME);
    }

    public void setName(String name) {
      set(NAME, name);
    }
  }
}
