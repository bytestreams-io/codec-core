package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DataObjectTest {

  static class Message extends DataObject {
    static final FieldSpec<Message, Integer> ID = field("id", Codecs.uint16());
    static final FieldSpec<Message, String> NAME = field("name", Codecs.ascii(5));
    static final FieldSpec<Message, String> TAG =
        field("tag", Codecs.ascii(3), msg -> msg.getId() > 0);

    public int getId() {
      return get(ID);
    }

    public String getName() {
      return get(NAME);
    }

    public void setName(String name) {
      set(NAME, name);
    }
  }

  @Test
  void typed_subclass_getter() {
    Message msg = new Message();
    msg.setName("hello");
    assertThat(msg.getName()).isEqualTo("hello");
  }

  @Test
  void codec_can_set_via_field_spec() throws IOException {
    Codec<Message> codec =
        Codecs.<Message>sequential(Message::new).field(Message.ID).field(Message.NAME).build();

    Message original = new Message();
    original.set(Message.ID, 42);
    original.setName("hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    Message decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.getId()).isEqualTo(42);
    assertThat(decoded.getName()).isEqualTo("hello");
  }

  @Test
  void roundtrip_equals() throws IOException {
    Codec<Message> codec =
        Codecs.<Message>sequential(Message::new).field(Message.ID).field(Message.NAME).build();

    Message original = new Message();
    original.set(Message.ID, 42);
    original.setName("hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    Message decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded).isEqualTo(original);
  }

  @Test
  void equals_same_instance() {
    Message msg = new Message();
    msg.setName("hello");
    assertThat(msg).isEqualTo(msg);
  }

  @Test
  void equals_same_content() {
    Message a = new Message();
    a.setName("hello");
    Message b = new Message();
    b.setName("hello");
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equals_different_content() {
    Message a = new Message();
    a.setName("hello");
    Message b = new Message();
    b.setName("world");
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void equals_not_equal_to_other_type() {
    Message msg = new Message();
    msg.setName("hello");
    assertThat(msg).isNotEqualTo("not a DataObject");
  }

  @Test
  void field_with_presence_present() throws IOException {
    Codec<Message> codec =
        Codecs.<Message>sequential(Message::new).field(Message.ID).field(Message.TAG).build();

    Message original = new Message();
    original.set(Message.ID, 1);
    original.set(Message.TAG, "abc");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    Message decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.getId()).isEqualTo(1);
    assertThat(decoded.<String>get("tag")).isEqualTo("abc");
  }

  @Test
  void field_with_presence_absent() throws IOException {
    Codec<Message> codec =
        Codecs.<Message>sequential(Message::new).field(Message.ID).field(Message.TAG).build();

    Message original = new Message();
    original.set(Message.ID, 0);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    Message decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.getId()).isZero();
    assertThat(decoded.<String>get("tag")).isNull();
  }

  @Test
  void to_string_delegates_to_map() {
    Message msg = new Message();
    msg.setName("hello");
    assertThat(msg).hasToString("{name=hello}");
  }
}
