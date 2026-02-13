package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TaggedObjectCodecTest {

  private static final Codec<String> TAG_CODEC = FixedCodePointStringCodec.builder(4).build();

  @Test
  void roundtrip_single_field() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
            .build();

    TestTagged original = new TestTagged();
    original.add("code", 42);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("code");
    assertThat(decoded.<Integer>getAll("code")).containsExactly(42);
  }

  @Test
  void roundtrip_multiple_different_tags() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .field("name", FixedCodePointStringCodec.builder(5).build())
            .factory(TestTagged::new)
            .build();

    TestTagged original = new TestTagged();
    original.add("code", 100);
    original.add("name", "hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("code", "name");
    assertThat(decoded.<Integer>getAll("code")).containsExactly(100);
    assertThat(decoded.<String>getAll("name")).containsExactly("hello");
  }

  @Test
  void roundtrip_duplicate_tags() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
            .build();

    TestTagged original = new TestTagged();
    original.add("code", 1);
    original.add("code", 2);
    original.add("code", 3);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("code");
    assertThat(decoded.<Integer>getAll("code")).containsExactly(1, 2, 3);
  }

  @Test
  void roundtrip_with_default_codec() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .defaultCodec(new BinaryCodec(2))
            .factory(TestTagged::new)
            .build();

    TestTagged original = new TestTagged();
    original.add("abcd", new byte[] {1, 2});

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("abcd");
    assertThat(decoded.<byte[]>getAll("abcd")).hasSize(1);
    assertThat(decoded.<byte[]>getAll("abcd").get(0)).isEqualTo(new byte[] {1, 2});
  }

  @Test
  void roundtrip_mixed_registered_and_default() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .defaultCodec(new BinaryCodec(3))
            .factory(TestTagged::new)
            .build();

    TestTagged original = new TestTagged();
    original.add("code", 42);
    original.add("data", new byte[] {10, 20, 30});

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("code", "data");
    assertThat(decoded.<Integer>getAll("code")).containsExactly(42);
    assertThat(decoded.<byte[]>getAll("data")).hasSize(1);
    assertThat(decoded.<byte[]>getAll("data").get(0)).isEqualTo(new byte[] {10, 20, 30});
  }

  @Test
  void roundtrip_no_registered_codecs_all_go_to_default() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .defaultCodec(new BinaryCodec(2))
            .factory(TestTagged::new)
            .build();

    TestTagged original = new TestTagged();
    original.add("aaaa", new byte[] {1, 2});
    original.add("bbbb", new byte[] {3, 4});

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).containsExactly("aaaa", "bbbb");
    assertThat(decoded.<byte[]>getAll("aaaa")).hasSize(1);
    assertThat(decoded.<byte[]>getAll("aaaa").get(0)).isEqualTo(new byte[] {1, 2});
    assertThat(decoded.<byte[]>getAll("bbbb")).hasSize(1);
    assertThat(decoded.<byte[]>getAll("bbbb").get(0)).isEqualTo(new byte[] {3, 4});
  }

  @Test
  void roundtrip_empty_fields() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
            .build();

    TestTagged original = new TestTagged();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    assertThat(output.toByteArray()).isEmpty();

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).isEmpty();
  }

  @Test
  void encode_unknown_tag_without_default_throws() {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
            .build();

    TestTagged obj = new TestTagged();
    obj.add("xxxx", 99);

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("xxxx")
        .hasMessageContaining("codec not implemented");
  }

  @Test
  void encode_error_includes_tag() {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new NotImplementedCodec<>())
            .factory(TestTagged::new)
            .build();

    TestTagged obj = new TestTagged();
    obj.add("code", 42);

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("code")
        .hasMessageContaining("codec not implemented");
  }

  @Test
  void encode_non_codec_exception_wraps_with_tag() {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new AlwaysThrowsCodec<>())
            .factory(TestTagged::new)
            .build();

    TestTagged obj = new TestTagged();
    obj.add("code", 42);

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(obj, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("code")
        .hasMessageContaining("always fails");
  }

  @Test
  void decode_empty_stream() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
            .build();

    TestTagged decoded = codec.decode(new ByteArrayInputStream(new byte[0]));

    assertThat(decoded.tags()).isEmpty();
  }

  @Test
  void decode_max_fields_zero_decodes_nothing() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .maxFields(0)
            .factory(TestTagged::new)
            .build();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes("code".getBytes(UTF_8));
    output.writeBytes(new byte[] {0, 1});

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.tags()).isEmpty();
  }

  @Test
  void decode_respects_max_fields() throws IOException {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .maxFields(1)
            .factory(TestTagged::new)
            .build();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes("code".getBytes(UTF_8));
    output.writeBytes(new byte[] {0, 1});
    output.writeBytes("code".getBytes(UTF_8));
    output.writeBytes(new byte[] {0, 2});

    TestTagged decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.<Integer>getAll("code")).containsExactly(1);
  }

  @Test
  void decode_unknown_tag_without_default_throws() {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
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
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
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
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(new NotImplementedCodec<>())
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("codec not implemented");
  }

  @Test
  void decode_tag_codec_throws_non_codec_exception() {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(new AlwaysThrowsCodec<>())
            .field("code", new UnsignedShortCodec())
            .factory(TestTagged::new)
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("always fails");
  }

  @Test
  void builder_null_tag_codec() {
    TaggedObjectCodec.Builder<TestTagged> builder = TaggedObjectCodec.builder();

    assertThatThrownBy(() -> builder.tagCodec(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tagCodec");
  }

  @Test
  void builder_null_tag() {
    TaggedObjectCodec.Builder<TestTagged> builder = TaggedObjectCodec.builder();

    Codec<Integer> codec = new UnsignedShortCodec();

    assertThatThrownBy(() -> builder.field(null, codec))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tag");
  }

  @Test
  void builder_null_codec() {
    TaggedObjectCodec.Builder<TestTagged> builder = TaggedObjectCodec.builder();

    assertThatThrownBy(() -> builder.field("code", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void builder_null_default_codec() {
    TaggedObjectCodec.Builder<TestTagged> builder = TaggedObjectCodec.builder();

    assertThatThrownBy(() -> builder.defaultCodec(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("defaultCodec");
  }

  @Test
  void builder_null_factory() {
    TaggedObjectCodec.Builder<TestTagged> builder = TaggedObjectCodec.builder();

    assertThatThrownBy(() -> builder.factory(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("factory");
  }

  @Test
  void builder_negative_max_fields() {
    TaggedObjectCodec.Builder<TestTagged> builder = TaggedObjectCodec.builder();

    assertThatThrownBy(() -> builder.maxFields(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }

  @Test
  void builder_missing_tag_codec() {
    TaggedObjectCodec.Builder<TestTagged> builder =
        TaggedObjectCodec.<TestTagged>builder().factory(TestTagged::new);

    assertThatThrownBy(builder::build)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tagCodec");
  }

  @Test
  void builder_missing_factory() {
    TaggedObjectCodec.Builder<TestTagged> builder =
        TaggedObjectCodec.<TestTagged>builder().tagCodec(TAG_CODEC);

    assertThatThrownBy(builder::build)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("factory");
  }

  @Test
  void factory_returns_null() {
    TaggedObjectCodec<TestTagged> codec =
        TaggedObjectCodec.<TestTagged>builder()
            .tagCodec(TAG_CODEC)
            .field("code", new UnsignedShortCodec())
            .factory(() -> null)
            .build();

    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("factory.get()");
  }

  @Test
  void add_returns_same_instance() {
    TestTagged obj = new TestTagged();

    TestTagged result = obj.add("code", 42);

    assertThat(result).isSameAs(obj);
  }

  @Test
  void getAll_absent_tag_returns_empty_list() {
    TestTagged obj = new TestTagged();

    assertThat(obj.<Integer>getAll("missing")).isEmpty();
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

  static class TestTagged implements Tagged<TestTagged> {
    private final Map<String, List<Object>> fields = new LinkedHashMap<>();

    @Override
    public Set<String> tags() {
      return fields.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> List<V> getAll(String tag) {
      return (List<V>) fields.getOrDefault(tag, List.of());
    }

    @Override
    public <V> TestTagged add(String tag, V value) {
      fields.computeIfAbsent(tag, k -> new ArrayList<>()).add(value);
      return this;
    }
  }
}
