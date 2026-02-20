package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CodecsTest {

  @Nested
  class Numbers {

    @Test
    void uint8_returns_1_byte_codec() {
      FixedLengthCodec<Integer> codec = Codecs.uint8();
      assertThat(codec).isInstanceOf(BinaryNumberCodec.class);
      assertThat(codec.getLength()).isEqualTo(1);
    }

    @Test
    void uint16_returns_2_byte_codec() {
      FixedLengthCodec<Integer> codec = Codecs.uint16();
      assertThat(codec).isInstanceOf(BinaryNumberCodec.class);
      assertThat(codec.getLength()).isEqualTo(2);
    }

    @Test
    void uint32_returns_4_byte_codec() {
      FixedLengthCodec<Long> codec = Codecs.uint32();
      assertThat(codec).isInstanceOf(BinaryNumberCodec.class);
      assertThat(codec.getLength()).isEqualTo(4);
    }

    @Test
    void int16_returns_2_byte_codec() {
      FixedLengthCodec<Short> codec = Codecs.int16();
      assertThat(codec).isInstanceOf(BinaryNumberCodec.class);
      assertThat(codec.getLength()).isEqualTo(2);
    }

    @Test
    void int32_returns_4_byte_codec() {
      FixedLengthCodec<Integer> codec = Codecs.int32();
      assertThat(codec).isInstanceOf(BinaryNumberCodec.class);
      assertThat(codec.getLength()).isEqualTo(4);
    }

    @Test
    void int64_returns_8_byte_codec() {
      FixedLengthCodec<Long> codec = Codecs.int64();
      assertThat(codec).isInstanceOf(BinaryNumberCodec.class);
      assertThat(codec.getLength()).isEqualTo(8);
    }

    @Test
    void float32_returns_4_byte_codec() {
      FixedLengthCodec<Float> codec = Codecs.float32();
      assertThat(codec).isInstanceOf(BinaryNumberCodec.class);
      assertThat(codec.getLength()).isEqualTo(4);
    }

    @Test
    void float64_returns_8_byte_codec() {
      FixedLengthCodec<Double> codec = Codecs.float64();
      assertThat(codec).isInstanceOf(BinaryNumberCodec.class);
      assertThat(codec.getLength()).isEqualTo(8);
    }
  }

  @Nested
  class Strings {

    @Test
    void ascii_fixed_returns_fixed_codec() {
      FixedLengthCodec<String> codec = Codecs.ascii(5);
      assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
      assertThat(codec.getLength()).isEqualTo(5);
    }

    @Test
    void ascii_stream_returns_stream_codec() {
      assertThat(Codecs.ascii()).isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void utf8_fixed_returns_fixed_codec() {
      FixedLengthCodec<String> codec = Codecs.utf8(5);
      assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
      assertThat(codec.getLength()).isEqualTo(5);
    }

    @Test
    void utf8_stream_returns_stream_codec() {
      assertThat(Codecs.utf8()).isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void latin1_fixed_returns_fixed_codec() {
      FixedLengthCodec<String> codec = Codecs.latin1(5);
      assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
      assertThat(codec.getLength()).isEqualTo(5);
    }

    @Test
    void latin1_stream_returns_stream_codec() {
      assertThat(Codecs.latin1()).isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void ebcdic_fixed_returns_fixed_codec() {
      FixedLengthCodec<String> codec = Codecs.ebcdic(5);
      assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
      assertThat(codec.getLength()).isEqualTo(5);
    }

    @Test
    void ebcdic_stream_returns_stream_codec() {
      assertThat(Codecs.ebcdic()).isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void ofCharset_fixed_returns_fixed_codec() {
      FixedLengthCodec<String> codec = Codecs.ofCharset(StandardCharsets.UTF_16, 3);
      assertThat(codec).isInstanceOf(FixedCodePointStringCodec.class);
      assertThat(codec.getLength()).isEqualTo(3);
    }

    @Test
    void ofCharset_stream_returns_stream_codec() {
      assertThat(Codecs.ofCharset(StandardCharsets.UTF_16))
          .isInstanceOf(StreamCodePointStringCodec.class);
    }
  }

  @Nested
  class Hex {

    @Test
    void hex_fixed_returns_fixed_codec() {
      FixedLengthCodec<String> codec = Codecs.hex(4);
      assertThat(codec).isInstanceOf(FixedHexStringCodec.class);
      assertThat(codec.getLength()).isEqualTo(4);
    }

    @Test
    void hex_stream_returns_stream_codec() {
      assertThat(Codecs.hex()).isInstanceOf(StreamHexStringCodec.class);
    }
  }

  @Nested
  class Composition {

    @Test
    void prefixed_byte_length_encodes_length_prefix_and_value() throws IOException {
      Codec<String> codec = Codecs.prefixed(Codecs.uint8(), Codecs.ascii());

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.encode("hi", out);

      byte[] bytes = out.toByteArray();
      assertThat(bytes[0]).isEqualTo((byte) 2); // length prefix
      assertThat(new String(bytes, 1, 2, StandardCharsets.US_ASCII)).isEqualTo("hi");
    }

    @Test
    void prefixed_byte_length_decodes_length_prefix_and_value() throws IOException {
      Codec<String> codec = Codecs.prefixed(Codecs.uint8(), Codecs.ascii());

      byte[] data = new byte[] {0x03, 'a', 'b', 'c'};
      String result = codec.decode(new ByteArrayInputStream(data));
      assertThat(result).isEqualTo("abc");
    }

    @Test
    void prefixed_item_length_encodes_count_prefix_and_value() throws IOException {
      Codec<String> codec =
          Codecs.prefixed(Codecs.uint8(), s -> s.codePointCount(0, s.length()), Codecs::ascii);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.encode("hi", out);

      byte[] bytes = out.toByteArray();
      assertThat(bytes[0]).isEqualTo((byte) 2); // item count prefix
      assertThat(new String(bytes, 1, 2, StandardCharsets.US_ASCII)).isEqualTo("hi");
    }

    @Test
    void prefixed_item_length_decodes_count_prefix_and_value() throws IOException {
      Codec<String> codec =
          Codecs.prefixed(Codecs.uint8(), s -> s.codePointCount(0, s.length()), Codecs::ascii);

      byte[] data = new byte[] {0x03, 'a', 'b', 'c'};
      String result = codec.decode(new ByteArrayInputStream(data));
      assertThat(result).isEqualTo("abc");
    }

    @Test
    void listOf_fixed_returns_fixed_list_codec() throws IOException {
      FixedLengthCodec<List<Integer>> codec = Codecs.listOf(Codecs.uint8(), 3);
      assertThat(codec).isInstanceOf(FixedListCodec.class);
      assertThat(codec.getLength()).isEqualTo(3);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.encode(List.of(1, 2, 3), out);
      assertThat(out.toByteArray()).containsExactly(1, 2, 3);
    }

    @Test
    void listOf_stream_returns_stream_list_codec() throws IOException {
      Codec<List<Integer>> codec = Codecs.listOf(Codecs.uint8());
      assertThat(codec).isInstanceOf(StreamListCodec.class);

      List<Integer> result = codec.decode(new ByteArrayInputStream(new byte[] {1, 2, 3}));
      assertThat(result).containsExactly(1, 2, 3);
    }
  }

  @Nested
  class Objects {

    @Test
    void sequential_returns_builder_that_builds_working_codec() throws IOException {
      SequentialObjectCodec<TestObject> codec =
          Codecs.<TestObject>sequential(TestObject::new)
              .field("value", Codecs.ascii(5), TestObject::getValue, TestObject::setValue)
              .build();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      TestObject obj = new TestObject();
      obj.setValue("hello");
      codec.encode(obj, out);

      TestObject decoded = codec.decode(new ByteArrayInputStream(out.toByteArray()));
      assertThat(decoded.getValue()).isEqualTo("hello");
    }

    @Test
    void tagged_returns_builder() {
      assertThat(Codecs.<TestTagged, String>tagged(TestTagged::new, Codecs.ascii(4)))
          .isInstanceOf(TaggedObjectCodec.Builder.class);
    }
  }

  @Nested
  class Other {

    @Test
    void binary_returns_binary_codec() {
      FixedLengthCodec<byte[]> codec = Codecs.binary(8);
      assertThat(codec).isInstanceOf(BinaryCodec.class);
      assertThat(codec.getLength()).isEqualTo(8);
    }

    @Test
    void bool_returns_boolean_codec() throws IOException {
      FixedLengthCodec<Boolean> codec = Codecs.bool();
      assertThat(codec).isInstanceOf(BooleanCodec.class);
      assertThat(codec.getLength()).isEqualTo(1);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.encode(true, out);
      assertThat(out.toByteArray()).containsExactly(0x01);
    }
  }

  static class TestObject {
    private String value;

    String getValue() {
      return value;
    }

    void setValue(String value) {
      this.value = value;
    }
  }

  static class TestTagged implements Tagged<TestTagged, String> {
    private final java.util.LinkedHashMap<String, java.util.List<Object>> data =
        new java.util.LinkedHashMap<>();

    @Override
    public java.util.Set<String> tags() {
      return data.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> java.util.List<V> getAll(String tag) {
      return (java.util.List<V>) data.getOrDefault(tag, java.util.List.of());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> TestTagged add(String tag, V value) {
      data.computeIfAbsent(tag, k -> new java.util.ArrayList<>()).add(value);
      return this;
    }
  }
}
