package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.bytestreams.codec.core.util.Triple;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TripleCodecTest {

  @Test
  void encode() throws IOException {
    TripleCodec<Integer, Integer, Integer> codec =
        Codecs.triple(Codecs.uint8(), Codecs.uint8(), Codecs.uint8());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(new Triple<>(10, 20, 30), output);

    assertThat(output.toByteArray()).containsExactly(10, 20, 30);
  }

  @Test
  void decode() throws IOException {
    TripleCodec<Integer, Integer, Integer> codec =
        Codecs.triple(Codecs.uint8(), Codecs.uint8(), Codecs.uint8());
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {10, 20, 30});

    var triple = codec.decode(input);

    assertThat(triple.first()).isEqualTo(10);
    assertThat(triple.second()).isEqualTo(20);
    assertThat(triple.third()).isEqualTo(30);
  }

  @Test
  void encode_result() throws IOException {
    TripleCodec<Integer, Integer, Integer> codec =
        Codecs.triple(Codecs.uint8(), Codecs.uint16(), Codecs.int32());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(new Triple<>(1, 2, 3), output);

    assertThat(result.count()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(7); // 1 + 2 + 4
  }

  @Test
  void as() throws IOException {
    Codec<Color> codec =
        Codecs.triple(Codecs.uint8(), Codecs.uint8(), Codecs.uint8())
            .as(Color::new, c -> c.r, c -> c.g, c -> c.b);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(new Color(255, 128, 0), output);
    Color decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.r).isEqualTo(255);
    assertThat(decoded.g).isEqualTo(128);
    assertThat(decoded.b).isZero();
  }

  @Test
  void constructor_null_first() {
    var second = Codecs.uint8();
    var third = Codecs.uint8();
    assertThatThrownBy(() -> new TripleCodec<>(null, second, third))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("first");
  }

  @Test
  void constructor_null_second() {
    var first = Codecs.uint8();
    var third = Codecs.uint8();
    assertThatThrownBy(() -> new TripleCodec<>(first, null, third))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("second");
  }

  @Test
  void constructor_null_third() {
    var first = Codecs.uint8();
    var second = Codecs.uint8();
    assertThatThrownBy(() -> new TripleCodec<>(first, second, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("third");
  }

  @Test
  void inspect_returns_map() {
    TripleCodec<Integer, Integer, Integer> codec =
        Codecs.triple(Codecs.uint8(), Codecs.uint8(), Codecs.uint8());

    Object result = Inspector.inspect(codec, new Triple<>(10, 20, 30));

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("first", 10);
    expected.put("second", 20);
    expected.put("third", 30);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void inspect_recurses_into_introspectable_elements() {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
            .field(
                "value", Codecs.uint8(), TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();
    TripleCodec<Integer, TestFixtures.Inner, Integer> codec =
        new TripleCodec<>(Codecs.uint8(), innerCodec, Codecs.uint8());

    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(99);

    Object result = Inspector.inspect(codec, new Triple<>(10, inner, 30));

    Map<String, Object> expectedInner = new LinkedHashMap<>();
    expectedInner.put("value", 99);
    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("first", 10);
    expected.put("second", expectedInner);
    expected.put("third", 30);
    assertThat(result).isEqualTo(expected);
  }

  static class Color {
    final int r;
    final int g;
    final int b;

    Color(int r, int g, int b) {
      this.r = r;
      this.g = g;
      this.b = b;
    }
  }
}
