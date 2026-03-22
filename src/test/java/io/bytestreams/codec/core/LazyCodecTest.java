package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LazyCodecTest {

  @Test
  void encode() throws IOException {
    Codec<Integer> codec = Codecs.lazy(Codecs::uint8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(42, output);

    assertThat(output.toByteArray()).containsExactly(42);
  }

  @Test
  void decode() throws IOException {
    Codec<Integer> codec = Codecs.lazy(Codecs::uint8);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {42});

    assertThat(codec.decode(input)).isEqualTo(42);
  }

  @Test
  void encode_result() throws IOException {
    Codec<Integer> codec = Codecs.lazy(Codecs::uint8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(42, output);

    assertThat(result.count()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void recursive_codec() throws IOException {
    // A simple recursive structure: value + list of children
    Codec<Node>[] holder = new Codec[1];
    Codec<Node> nodeCodec =
        Codecs.pair(
                Codecs.uint8(),
                Codecs.prefixed(Codecs.uint8(), Codecs.listOf(Codecs.lazy(() -> holder[0]))))
            .as(Node::new, n -> n.value, n -> n.children);
    holder[0] = nodeCodec;

    Node tree = new Node(1, List.of(new Node(2, List.of()), new Node(3, List.of())));

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    nodeCodec.encode(tree, output);
    Node decoded = nodeCodec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.value).isEqualTo(1);
    assertThat(decoded.children).hasSize(2);
    assertThat(decoded.children.get(0).value).isEqualTo(2);
    assertThat(decoded.children.get(1).value).isEqualTo(3);
  }

  @Test
  void constructor_null_supplier() {
    assertThatThrownBy(() -> new LazyCodec<>(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("supplier");
  }

  @Test
  void supplier_returns_null() {
    Codec<Integer> codec = Codecs.lazy(() -> null);

    var output = new ByteArrayOutputStream();
    assertThatThrownBy(() -> codec.encode(1, output))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("supplier.get()");
  }

  @Test
  void inspect_delegates_to_resolved_codec() {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
            .field(
                "value", Codecs.uint8(), TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();

    Codec<TestFixtures.Inner> codec = Codecs.lazy(() -> innerCodec);

    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(55);

    Object result = Inspector.inspect(codec, inner);

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("value", 55);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void inspect_returns_raw_when_resolved_not_introspectable() {
    Codec<Integer> codec = Codecs.lazy(Codecs::uint8);

    Object result = Inspector.inspect(codec, 10);

    assertThat(result).isEqualTo(10);
  }

  static class Node {
    final int value;
    final List<Node> children;

    Node(int value, List<Node> children) {
      this.value = value;
      this.children = children;
    }
  }
}
