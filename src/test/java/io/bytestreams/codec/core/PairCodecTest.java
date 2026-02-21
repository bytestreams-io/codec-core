package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.bytestreams.codec.core.util.Pair;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class PairCodecTest {

  @Test
  void encode() throws IOException {
    PairCodec<Integer, Integer> codec = Codecs.pair(Codecs.uint8(), Codecs.uint16());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(new Pair<>(42, 1000), output);

    assertThat(output.toByteArray()).containsExactly(42, 0x03, 0xE8);
  }

  @Test
  void decode() throws IOException {
    PairCodec<Integer, Integer> codec = Codecs.pair(Codecs.uint8(), Codecs.uint16());
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {42, 0x03, (byte) 0xE8});

    var pair = codec.decode(input);

    assertThat(pair.first()).isEqualTo(42);
    assertThat(pair.second()).isEqualTo(1000);
  }

  @Test
  void encode_result() throws IOException {
    PairCodec<Integer, Integer> codec = Codecs.pair(Codecs.uint8(), Codecs.uint16());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(new Pair<>(42, 1000), output);

    assertThat(result.count()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(3); // 1 + 2
  }

  @Test
  void as() throws IOException {
    Codec<Point> codec =
        Codecs.pair(Codecs.uint8(), Codecs.uint16()).as(Point::new, p -> p.x, p -> p.y);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(new Point(10, 500), output);
    Point decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.x).isEqualTo(10);
    assertThat(decoded.y).isEqualTo(500);
  }

  @Test
  void constructor_null_first() {
    var second = Codecs.uint8();
    assertThatThrownBy(() -> new PairCodec<>(null, second))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("first");
  }

  @Test
  void constructor_null_second() {
    var first = Codecs.uint8();
    assertThatThrownBy(() -> new PairCodec<>(first, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("second");
  }

  static class Point {
    final int x;
    final int y;

    Point(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }
}
