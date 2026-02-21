package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.bytestreams.codec.core.util.BiMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChoiceCodecTest {

  private static final BiMap<Integer, Class<? extends Shape>> TAGS =
      BiMap.of(Map.entry(1, Circle.class), Map.entry(2, Rectangle.class));
  private static final Codec<Class<? extends Shape>> CLASS_CODEC = Codecs.uint8().xmap(TAGS);
  private static final Codec<Circle> CIRCLE_CODEC = Codecs.uint8().xmap(Circle::new, c -> c.radius);
  private static final Codec<Rectangle> RECTANGLE_CODEC =
      Codecs.pair(Codecs.uint8(), Codecs.uint8()).as(Rectangle::new, r -> r.width, r -> r.height);

  private static Codec<Shape> shapeCodec() {
    return Codecs.choice(CLASS_CODEC)
        .on(Circle.class, CIRCLE_CODEC)
        .on(Rectangle.class, RECTANGLE_CODEC)
        .build();
  }

  @Test
  void encode() throws IOException {
    Codec<Shape> codec = shapeCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(new Circle(42), output);

    assertThat(output.toByteArray()).containsExactly(1, 42);
  }

  @Test
  void decode() throws IOException {
    Codec<Shape> codec = shapeCodec();
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {2, 10, 20});

    Shape shape = codec.decode(input);

    assertThat(shape).isInstanceOf(Rectangle.class);
    Rectangle rect = (Rectangle) shape;
    assertThat(rect.width).isEqualTo(10);
    assertThat(rect.height).isEqualTo(20);
  }

  @Test
  void encode_result() throws IOException {
    Codec<Shape> codec = shapeCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(new Rectangle(10, 20), output);

    assertThat(result.count()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(3); // 1 tag + 2 value bytes
  }

  @Test
  void encode_unregistered_class() {
    Codec<Shape> codec = shapeCodec();
    var triangle = new Triangle();
    var output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(triangle, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("no codec registered for");
  }

  @Test
  void decode_unregistered_class() {
    BiMap<Integer, Class<? extends Shape>> tagsWithTriangle =
        BiMap.of(
            Map.entry(1, Circle.class),
            Map.entry(2, Rectangle.class),
            Map.entry(3, Triangle.class));
    Codec<Shape> codec =
        Codecs.<Shape>choice(Codecs.uint8().xmap(tagsWithTriangle))
            .on(Circle.class, CIRCLE_CODEC)
            .on(Rectangle.class, RECTANGLE_CODEC)
            .build();

    var input = new ByteArrayInputStream(new byte[] {3});
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("no codec registered for");
  }

  @Test
  void builder_duplicate_class() {
    var builder = Codecs.choice(CLASS_CODEC).on(Circle.class, CIRCLE_CODEC);
    assertThatThrownBy(() -> builder.on(Circle.class, CIRCLE_CODEC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duplicate");
  }

  @Test
  void builder_empty() {
    var builder = Codecs.choice(CLASS_CODEC);
    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one");
  }

  @Test
  void builder_null_type() {
    var builder = Codecs.choice(CLASS_CODEC);
    assertThatThrownBy(() -> builder.on(null, CIRCLE_CODEC))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("type");
  }

  @Test
  void builder_null_codec() {
    var builder = Codecs.choice(CLASS_CODEC);
    assertThatThrownBy(() -> builder.on(Circle.class, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void builder_null_classCodec() {
    assertThatThrownBy(() -> Codecs.<Shape>choice(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("classCodec");
  }

  abstract static class Shape {}

  static class Circle extends Shape {
    final int radius;

    Circle(int radius) {
      this.radius = radius;
    }
  }

  static class Rectangle extends Shape {
    final int width;
    final int height;

    Rectangle(int width, int height) {
      this.width = width;
      this.height = height;
    }
  }

  static class Triangle extends Shape {}
}
