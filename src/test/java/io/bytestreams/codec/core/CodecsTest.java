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
    void uint8_returns_codec() {
      assertThat(Codecs.uint8()).isInstanceOf(BinaryNumberCodec.class);
    }

    @Test
    void uint16_returns_codec() {
      assertThat(Codecs.uint16()).isInstanceOf(BinaryNumberCodec.class);
    }

    @Test
    void uint32_returns_codec() {
      assertThat(Codecs.uint32()).isInstanceOf(BinaryNumberCodec.class);
    }

    @Test
    void int16_returns_codec() {
      assertThat(Codecs.int16()).isInstanceOf(BinaryNumberCodec.class);
    }

    @Test
    void int32_returns_codec() {
      assertThat(Codecs.int32()).isInstanceOf(BinaryNumberCodec.class);
    }

    @Test
    void int64_returns_codec() {
      assertThat(Codecs.int64()).isInstanceOf(BinaryNumberCodec.class);
    }

    @Test
    void float32_returns_codec() {
      assertThat(Codecs.float32()).isInstanceOf(BinaryNumberCodec.class);
    }

    @Test
    void float64_returns_codec() {
      assertThat(Codecs.float64()).isInstanceOf(BinaryNumberCodec.class);
    }
  }

  @Nested
  class Strings {

    @Test
    void ascii_fixed_returns_fixed_codec() {
      assertThat(Codecs.ascii(5)).isInstanceOf(FixedCodePointStringCodec.class);
    }

    @Test
    void ascii_stream_returns_stream_codec() {
      assertThat(Codecs.ascii()).isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void utf8_fixed_returns_fixed_codec() {
      assertThat(Codecs.utf8(5)).isInstanceOf(FixedCodePointStringCodec.class);
    }

    @Test
    void utf8_stream_returns_stream_codec() {
      assertThat(Codecs.utf8()).isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void latin1_fixed_returns_fixed_codec() {
      assertThat(Codecs.latin1(5)).isInstanceOf(FixedCodePointStringCodec.class);
    }

    @Test
    void latin1_stream_returns_stream_codec() {
      assertThat(Codecs.latin1()).isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void ebcdic_fixed_returns_fixed_codec() {
      assertThat(Codecs.ebcdic(5)).isInstanceOf(FixedCodePointStringCodec.class);
    }

    @Test
    void ebcdic_stream_returns_stream_codec() {
      assertThat(Codecs.ebcdic()).isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void ofCharset_fixed_returns_fixed_codec() {
      assertThat(Codecs.ofCharset(StandardCharsets.UTF_16, 3))
          .isInstanceOf(FixedCodePointStringCodec.class);
    }

    @Test
    void ofCharset_stream_returns_stream_codec() {
      assertThat(Codecs.ofCharset(StandardCharsets.UTF_16))
          .isInstanceOf(StreamCodePointStringCodec.class);
    }

    @Test
    void ascii_prefixed_returns_variable_item_length_codec() {
      assertThat(Codecs.ascii(Codecs.uint8())).isInstanceOf(VariableItemLengthCodec.class);
    }

    @Test
    void utf8_prefixed_returns_variable_item_length_codec() {
      assertThat(Codecs.utf8(Codecs.uint8())).isInstanceOf(VariableItemLengthCodec.class);
    }

    @Test
    void latin1_prefixed_returns_variable_item_length_codec() {
      assertThat(Codecs.latin1(Codecs.uint8())).isInstanceOf(VariableItemLengthCodec.class);
    }

    @Test
    void ebcdic_prefixed_returns_variable_item_length_codec() {
      assertThat(Codecs.ebcdic(Codecs.uint8())).isInstanceOf(VariableItemLengthCodec.class);
    }

    @Test
    void ofCharset_prefixed_returns_variable_item_length_codec() {
      assertThat(Codecs.ofCharset(StandardCharsets.UTF_8, Codecs.uint8()))
          .isInstanceOf(VariableItemLengthCodec.class);
    }

    @Test
    void ofCharset_prefixed_encodes_code_point_count_for_multibyte_charset() throws IOException {
      Codec<String> codec = Codecs.ofCharset(StandardCharsets.UTF_8, Codecs.uint8());

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      // U+1F600 (grinning face) = 1 code point, 4 UTF-8 bytes, 2 Java chars
      codec.encode("\uD83D\uDE00", out);

      byte[] bytes = out.toByteArray();
      assertThat(bytes[0] & 0xFF).isEqualTo(1); // code point count, not String::length (2)
    }

    @Test
    void ofCharset_prefixed_uses_string_length_for_single_byte_charset() throws IOException {
      Codec<String> codec = Codecs.ofCharset(StandardCharsets.US_ASCII, Codecs.uint8());

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.encode("abc", out);

      byte[] bytes = out.toByteArray();
      assertThat(bytes[0] & 0xFF).isEqualTo(3);
      assertThat(new String(bytes, 1, 3, StandardCharsets.US_ASCII)).isEqualTo("abc");
    }
  }

  @Nested
  class Hex {

    @Test
    void hex_fixed_returns_fixed_codec() {
      assertThat(Codecs.hex(4)).isInstanceOf(FixedHexStringCodec.class);
    }

    @Test
    void hex_stream_returns_stream_codec() {
      assertThat(Codecs.hex()).isInstanceOf(StreamHexStringCodec.class);
    }

    @Test
    void hex_prefixed_returns_variable_item_length_codec() {
      assertThat(Codecs.hex(Codecs.uint8())).isInstanceOf(VariableItemLengthCodec.class);
    }

    @Test
    void hex_prefixed_encodes_digit_count() throws IOException {
      Codec<String> codec = Codecs.hex(Codecs.uint8());

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.encode("abc", out);

      byte[] bytes = out.toByteArray();
      assertThat(bytes[0] & 0xFF).isEqualTo(3); // digit count, not byte count (2)
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
      Codec<List<Integer>> codec = Codecs.listOf(Codecs.uint8(), 3);
      assertThat(codec).isInstanceOf(FixedListCodec.class);

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
      assertThat(Codecs.binary(8)).isInstanceOf(BinaryCodec.class);
    }

    @Test
    void constant_returns_constant_codec() {
      assertThat(Codecs.constant(new byte[] {0x4D, 0x5A})).isInstanceOf(ConstantCodec.class);
    }

    @Test
    void bool_returns_boolean_codec() throws IOException {
      Codec<Boolean> codec = Codecs.bool();
      assertThat(codec).isInstanceOf(BooleanCodec.class);

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
