package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaggedObjectCodecTest {

  private static final Codec<String> TAG_CODEC = Codecs.ofCharset(Charset.defaultCharset(), 4);

  @Test
  void roundtrip_single_field() throws IOException {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();
    original.add("code", 42);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("code");
    assertThat(decoded.<Integer>getAll("code")).containsExactly(42);
  }

  @Test
  void roundtrip_multiple_different_tags() throws IOException {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .tag("name", Codecs.ofCharset(Charset.defaultCharset(), 5))
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();
    original.add("code", 100);
    original.add("name", "hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("code", "name");
    assertThat(decoded.<Integer>getAll("code")).containsExactly(100);
    assertThat(decoded.<String>getAll("name")).containsExactly("hello");
  }

  @Test
  void roundtrip_duplicate_tags() throws IOException {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();
    original.add("code", 1);
    original.add("code", 2);
    original.add("code", 3);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("code");
    assertThat(decoded.<Integer>getAll("code")).containsExactly(1, 2, 3);
  }

  @Test
  void roundtrip_with_default_codec() throws IOException {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .defaultCodec(new BinaryCodec(2))
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();
    original.add("abcd", new byte[] {1, 2});

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("abcd");
    assertThat(decoded.<byte[]>getAll("abcd")).hasSize(1);
    assertThat(decoded.<byte[]>getAll("abcd").get(0)).isEqualTo(new byte[] {1, 2});
  }

  @Test
  void roundtrip_mixed_registered_and_default() throws IOException {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .defaultCodec(new BinaryCodec(3))
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();
    original.add("code", 42);
    original.add("data", new byte[] {10, 20, 30});

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("code", "data");
    assertThat(decoded.<Integer>getAll("code")).containsExactly(42);
    assertThat(decoded.<byte[]>getAll("data")).hasSize(1);
    assertThat(decoded.<byte[]>getAll("data").get(0)).isEqualTo(new byte[] {10, 20, 30});
  }

  @Test
  void roundtrip_no_registered_codecs_all_go_to_default() throws IOException {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .defaultCodec(new BinaryCodec(2))
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();
    original.add("aaaa", new byte[] {1, 2});
    original.add("bbbb", new byte[] {3, 4});

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("aaaa", "bbbb");
    assertThat(decoded.<byte[]>getAll("aaaa")).hasSize(1);
    assertThat(decoded.<byte[]>getAll("aaaa").get(0)).isEqualTo(new byte[] {1, 2});
    assertThat(decoded.<byte[]>getAll("bbbb")).hasSize(1);
    assertThat(decoded.<byte[]>getAll("bbbb").get(0)).isEqualTo(new byte[] {3, 4});
  }

  @Test
  void roundtrip_empty_fields() throws IOException {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    assertThat(output.toByteArray()).isEmpty();

    TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).isEmpty();
  }

  @Test
  void encode_unknown_tag_without_default_throws() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    TestFixtures.TestTagged obj = new TestFixtures.TestTagged();
    obj.add("xxxx", 99);

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("xxxx")
        .hasMessageContaining("codec not implemented");
  }

  @Test
  void encode_error_includes_tag() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", new NotImplementedCodec<>())
            .build();

    TestFixtures.TestTagged obj = new TestFixtures.TestTagged();
    obj.add("code", 42);

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("code")
        .hasMessageContaining("codec not implemented");
  }

  @Test
  void encode_non_codec_exception_wraps_with_tag() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", new AlwaysThrowsCodec<>())
            .build();

    TestFixtures.TestTagged obj = new TestFixtures.TestTagged();
    obj.add("code", 42);

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("code")
        .hasMessageContaining("always fails");
  }

  @Test
  void decode_empty_stream() throws IOException {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(new byte[0]));

    assertThat(decoded.tags()).isEmpty();
  }

  @Test
  void decode_unknown_tag_without_default_throws() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes("xxxx".getBytes(UTF_8));
    output.writeBytes(new byte[] {0, 1});

    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("xxxx")
        .hasMessageContaining("codec not implemented");
  }

  @Test
  void decode_error_includes_tag() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes("code".getBytes(UTF_8));

    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("code");
  }

  @Test
  void decode_tag_codec_throws_codec_exception() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, new NotImplementedCodec<>())
            .tag("code", Codecs.uint16())
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("codec not implemented");
  }

  @Test
  void decode_tag_codec_throws_non_codec_exception() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, new AlwaysThrowsCodec<>())
            .tag("code", Codecs.uint16())
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("always fails");
  }

  @Test
  void builder_null_tag_codec() {
    assertThatThrownBy(() -> TaggedObjectCodec.builder(TestFixtures.TestTagged::new, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tagCodec");
  }

  @Test
  void builder_null_tag() {
    TaggedObjectCodec.Builder<TestFixtures.TestTagged, String> builder =
        TaggedObjectCodec.builder(TestFixtures.TestTagged::new, TAG_CODEC);

    Codec<Integer> codec = Codecs.uint16();

    assertThatThrownBy(() -> builder.tag(null, codec))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tag");
  }

  @Test
  void builder_null_codec() {
    TaggedObjectCodec.Builder<TestFixtures.TestTagged, String> builder =
        TaggedObjectCodec.builder(TestFixtures.TestTagged::new, TAG_CODEC);

    assertThatThrownBy(() -> builder.tag("code", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void builder_null_default_codec() {
    TaggedObjectCodec.Builder<TestFixtures.TestTagged, String> builder =
        TaggedObjectCodec.builder(TestFixtures.TestTagged::new, TAG_CODEC);

    assertThatThrownBy(() -> builder.defaultCodec(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("defaultCodec");
  }

  @Test
  void builder_null_factory() {
    assertThatThrownBy(() -> TaggedObjectCodec.builder(null, TAG_CODEC))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("factory");
  }

  @Test
  void factory_returns_null() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(() -> null, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("factory.get()");
  }

  @Test
  void add_returns_same_instance() {
    TestFixtures.TestTagged obj = new TestFixtures.TestTagged();

    TestFixtures.TestTagged result = obj.add("code", 42);

    assertThat(result).isSameAs(obj);
  }

  @Test
  void getAll_absent_tag_returns_empty_list() {
    TestFixtures.TestTagged obj = new TestFixtures.TestTagged();

    assertThat(obj.<Integer>getAll("missing")).isEmpty();
  }

  @Test
  void inspect_returns_map_of_tag_to_values() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .tag("name", Codecs.ascii(5))
            .build();

    TestFixtures.TestTagged obj = new TestFixtures.TestTagged();
    obj.add("code", 42);
    obj.add("name", "hello");

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) Inspector.inspect(codec, obj);

    assertThat(result).containsEntry("code", List.of(42)).containsEntry("name", List.of("hello"));
  }

  @Test
  void inspect_recurses_into_introspectable_tag_codec() {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value",
                Codecs.uint16(),
                TestFixtures.Inner::getValue,
                TestFixtures.Inner::setValue)
            .build();

    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("data", innerCodec)
            .build();

    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(99);

    TestFixtures.TestTagged obj = new TestFixtures.TestTagged();
    obj.add("data", inner);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) Inspector.inspect(codec, obj);

    assertThat(result).containsKey("data");
    @SuppressWarnings("unchecked")
    List<Object> dataList = (List<Object>) result.get("data");
    assertThat(dataList).hasSize(1);
    assertThat(dataList.get(0)).isEqualTo(Map.of("value", 99));
  }

  @Test
  void inspect_handles_duplicate_tags() {
    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", Codecs.uint16())
            .build();

    TestFixtures.TestTagged obj = new TestFixtures.TestTagged();
    obj.add("code", 1);
    obj.add("code", 2);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) Inspector.inspect(codec, obj);

    assertThat(result).containsEntry("code", List.of(1, 2));
  }

  static class AlwaysThrowsCodec<V> implements Codec<V> {
    @Override
    public EncodeResult encode(V value, OutputStream output) {
      throw new IllegalStateException("always fails");
    }

    @Override
    public V decode(InputStream input) {
      throw new IllegalStateException("always fails");
    }
  }
}
