package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.bytestreams.codec.core.util.Triple;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
