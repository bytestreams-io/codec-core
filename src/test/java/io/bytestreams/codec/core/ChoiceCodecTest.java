package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ChoiceCodecTest {

  private static final Codec<Circle> CIRCLE_CODEC = Codecs.uint8().xmap(Circle::new, c -> c.radius);
  private static final Codec<Rectangle> RECTANGLE_CODEC =
      Codecs.pair(Codecs.uint8(), Codecs.uint8()).as(Rectangle::new, r -> r.width, r -> r.height);

  private static Codec<Shape> shapeCodec() {
    return Codecs.<Integer, Shape>choice(Codecs.uint8())
        .on(1, Circle.class, CIRCLE_CODEC)
        .on(2, Rectangle.class, RECTANGLE_CODEC)
        .build();
  }

  /**
   * An unrecognised alternative covers the whole span, tag included, so it is built from ordinary
   * combinators rather than being told which tag it saw.
   */
  private static Codec<UnknownShape> unknownCodec() {
    return Codecs.pair(Codecs.uint8(), Codecs.binary())
        .as(UnknownShape::new, u -> u.tag, u -> u.body);
  }

  private static Codec<Shape> shapeCodecWithRawFallback() {
    return Codecs.<Integer, Shape>choice(Codecs.uint8())
        .on(1, Circle.class, CIRCLE_CODEC)
        .otherwise(RawShape.class, Codecs.binary().xmap(RawShape::new, r -> r.raw))
        .build();
  }

  private static Codec<Shape> shapeCodecWithFallback() {
    return Codecs.<Integer, Shape>choice(Codecs.uint8())
        .on(1, Circle.class, CIRCLE_CODEC)
        .on(2, Rectangle.class, RECTANGLE_CODEC)
        .otherwise(UnknownShape.class, unknownCodec())
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

    assertThat(result.count()).isEqualTo(2); // delegated from the branch codec
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
  void decode_unknown_tag() {
    Codec<Shape> codec = shapeCodec();
    var input = new ByteArrayInputStream(new byte[] {3});

    // Previously an IllegalArgumentException raised inside the BiMap, with no field path
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("no codec registered for tag 3");
  }

  @Test
  void decode_unknown_tag_uses_fallback() throws IOException {
    Codec<Shape> codec = shapeCodecWithFallback();
    var input = new ByteArrayInputStream(new byte[] {9, 0x0A, 0x0B});

    Shape shape = codec.decode(input);

    assertThat(shape).isInstanceOf(UnknownShape.class);
    UnknownShape unknown = (UnknownShape) shape;
    assertThat(unknown.tag).isEqualTo(9);
    assertThat(unknown.body).containsExactly(0x0A, 0x0B);
  }

  @Test
  void encode_unregistered_class_uses_fallback() throws IOException {
    Codec<Shape> codec = shapeCodecWithFallback();
    var output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(new UnknownShape(9, new byte[] {0x0A, 0x0B}), output);

    assertThat(output.toByteArray()).containsExactly(9, 0x0A, 0x0B);
    assertThat(result.count()).isEqualTo(2); // delegated from the fallback body
    assertThat(result.bytes()).isEqualTo(3); // 1 tag + 2 body bytes
  }

  @Test
  void registered_class_still_encodes_when_a_fallback_is_present() throws IOException {
    Codec<Shape> codec = shapeCodecWithFallback();
    var output = new ByteArrayOutputStream();

    codec.encode(new Circle(42), output);

    assertThat(output.toByteArray()).containsExactly(1, 42);
  }

  @Test
  void registered_class_still_decodes_when_a_fallback_is_present() throws IOException {
    Codec<Shape> codec = shapeCodecWithFallback();
    var input = new ByteArrayInputStream(new byte[] {1, 42});

    Shape shape = codec.decode(input);

    assertThat(shape).isInstanceOf(Circle.class);
    assertThat(((Circle) shape).radius).isEqualTo(42);
  }

  @Test
  void fallback_round_trips() throws IOException {
    Codec<Shape> codec = shapeCodecWithFallback();
    byte[] wire = {9, 0x0A, 0x0B};

    Shape decoded = codec.decode(new ByteArrayInputStream(wire));
    var output = new ByteArrayOutputStream();
    codec.encode(decoded, output);

    assertThat(output.toByteArray()).isEqualTo(wire);
  }

  @Test
  void fallback_does_not_close_the_caller_stream() throws IOException {
    // SequenceInputStream closes each stream as it is exhausted; no codec here closes the
    // caller's stream, and a read-to-end fallback must not become the exception
    Codec<Shape> codec = shapeCodecWithRawFallback();
    var closed = new AtomicBoolean();
    var input =
        new FilterInputStream(new ByteArrayInputStream(new byte[] {9, 0x0A, 0x0B})) {
          @Override
          public void close() {
            closed.set(true);
          }
        };

    assertThat(codec.decode(input)).isInstanceOf(RawShape.class);
    assertThat(closed).isFalse();
  }

  @Test
  void read_to_end_fallback_inside_a_bounded_scope() throws IOException {
    // The documented pattern: an unknown alternative has unknown extent, so a read-to-end
    // fallback needs prefixed or terminated to bound it
    Codec<Shape> scoped = Codecs.prefixed(Codecs.uint8(), shapeCodecWithRawFallback());
    byte[] wire = {3, 9, 0x0A, 0x0B, 0x77};
    var input = new ByteArrayInputStream(wire);

    Shape shape = scoped.decode(input);

    assertThat(shape).isInstanceOf(RawShape.class);
    assertThat(((RawShape) shape).raw).containsExactly(9, 0x0A, 0x0B);
    assertThat(input.read()).isEqualTo(0x77); // the scope ended where the length said it would

    var output = new ByteArrayOutputStream();
    scoped.encode(shape, output);
    assertThat(output.toByteArray()).containsExactly(3, 9, 0x0A, 0x0B);
  }

  @Test
  void end_of_stream_at_the_tag_is_not_an_unknown_alternative() {
    Codec<Shape> codec = shapeCodecWithRawFallback();
    var input = new ByteArrayInputStream(new byte[0]);

    // A truncated stream must stay an EOFException rather than decoding as an unknown
    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void fallback_does_not_mask_a_corrupt_known_type() {
    Codec<Circle> failing =
        new Codec<>() {
          @Override
          public EncodeResult encode(Circle value, OutputStream output) {
            // not used in this test
            return new EncodeResult(0, 0);
          }

          @Override
          public Circle decode(InputStream input) {
            throw new CodecException("corrupt circle", null);
          }
        };
    Codec<Shape> codec =
        Codecs.<Integer, Shape>choice(Codecs.uint8())
            .on(1, Circle.class, failing)
            .otherwise(UnknownShape.class, unknownCodec())
            .build();

    // A registered tag whose body fails must stay an error, not silently become an UnknownShape
    var input = new ByteArrayInputStream(new byte[] {1, 42});
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("corrupt circle");
  }

  @Test
  void builder_duplicate_tag() {
    var builder = Codecs.<Integer, Shape>choice(Codecs.uint8()).on(1, Circle.class, CIRCLE_CODEC);
    assertThatThrownBy(() -> builder.on(1, Rectangle.class, RECTANGLE_CODEC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duplicate tag");
  }

  @Test
  void builder_duplicate_class() {
    var builder = Codecs.<Integer, Shape>choice(Codecs.uint8()).on(1, Circle.class, CIRCLE_CODEC);
    assertThatThrownBy(() -> builder.on(2, Circle.class, CIRCLE_CODEC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duplicate type");
  }

  @Test
  void builder_rejects_array_tag() {
    var builder = Codecs.<byte[], Shape>choice(Codecs.binary(2));
    // byte[] keys compare by identity, so a lookup by a freshly decoded tag would never match
    assertThatThrownBy(() -> builder.on(new byte[] {1}, Circle.class, CIRCLE_CODEC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("array");
  }

  @Test
  void builder_empty() {
    var builder = Codecs.<Integer, Shape>choice(Codecs.uint8());
    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one");
  }

  @Test
  void builder_null_tag() {
    var builder = Codecs.<Integer, Shape>choice(Codecs.uint8());
    assertThatThrownBy(() -> builder.on(null, Circle.class, CIRCLE_CODEC))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tag");
  }

  @Test
  void builder_null_type() {
    var builder = Codecs.<Integer, Shape>choice(Codecs.uint8());
    assertThatThrownBy(() -> builder.on(1, null, CIRCLE_CODEC))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("type");
  }

  @Test
  void builder_null_codec() {
    var builder = Codecs.<Integer, Shape>choice(Codecs.uint8());
    assertThatThrownBy(() -> builder.on(1, Circle.class, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void builder_null_otherwise_type() {
    var builder = Codecs.<Integer, Shape>choice(Codecs.uint8());
    Codec<UnknownShape> fallback = unknownCodec();
    assertThatThrownBy(() -> builder.otherwise(null, fallback))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("type");
  }

  @Test
  void builder_null_otherwise_codec() {
    var builder = Codecs.<Integer, Shape>choice(Codecs.uint8());
    assertThatThrownBy(() -> builder.otherwise(UnknownShape.class, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void builder_duplicate_otherwise() {
    var builder =
        Codecs.<Integer, Shape>choice(Codecs.uint8())
            .on(1, Circle.class, CIRCLE_CODEC)
            .otherwise(UnknownShape.class, unknownCodec());
    Codec<UnknownShape> another = unknownCodec();
    assertThatThrownBy(() -> builder.otherwise(UnknownShape.class, another))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fallback");
  }

  @Test
  void builder_on_type_already_used_by_fallback() {
    var builder =
        Codecs.<Integer, Shape>choice(Codecs.uint8()).otherwise(UnknownShape.class, unknownCodec());
    Codec<UnknownShape> another = unknownCodec();
    assertThatThrownBy(() -> builder.on(1, UnknownShape.class, another))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duplicate type");
  }

  @Test
  void builder_otherwise_type_already_registered() {
    var builder =
        Codecs.<Integer, Shape>choice(Codecs.uint8()).on(1, UnknownShape.class, unknownCodec());
    Codec<UnknownShape> another = unknownCodec();
    // Registered would win on encode; rejecting is better than resolving it silently
    assertThatThrownBy(() -> builder.otherwise(UnknownShape.class, another))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duplicate type");
  }

  @Test
  void builder_null_tagCodec() {
    assertThatThrownBy(() -> Codecs.<Integer, Shape>choice(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tagCodec");
  }

  @Test
  void inspect_delegates_to_matched_branch() {
    SequentialObjectCodec<InnerCircle> introspectableCircleCodec =
        SequentialObjectCodec.<InnerCircle>builder(InnerCircle::new)
            .field("radius", Codecs.uint8(), InnerCircle::getRadius, InnerCircle::setRadius)
            .build();
    Codec<Shape> codec =
        Codecs.<Integer, Shape>choice(Codecs.uint8())
            .on(1, InnerCircle.class, introspectableCircleCodec)
            .on(2, Rectangle.class, RECTANGLE_CODEC)
            .build();

    InnerCircle circle = new InnerCircle();
    circle.setRadius(42);

    Object result = Inspector.inspect(codec, circle);

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("radius", 42);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void inspect_returns_raw_when_no_codec_matched() {
    Codec<Shape> codec = shapeCodec();

    Object result = Inspector.inspect(codec, new Triangle());

    assertThat(result).isInstanceOf(Triangle.class);
  }

  @Test
  void inspect_delegates_to_the_fallback_codec() {
    Codec<UnknownShape> fallback = unknownCodec();
    Codec<Shape> codec =
        Codecs.<Integer, Shape>choice(Codecs.uint8())
            .on(1, Circle.class, CIRCLE_CODEC)
            .otherwise(UnknownShape.class, fallback)
            .build();
    UnknownShape value = new UnknownShape(9, new byte[] {0x0A, 0x0B});

    assertThat(Inspector.inspect(codec, value)).isEqualTo(Inspector.inspect(fallback, value));
  }

  @Test
  void encode_unregistered_class_the_fallback_does_not_cover() {
    Codec<Shape> codec = shapeCodecWithFallback();
    var output = new ByteArrayOutputStream();

    var triangle = new Triangle();

    // A Triangle is neither registered nor an UnknownShape, so it must not reach the fallback
    assertThatThrownBy(() -> codec.encode(triangle, output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("Triangle");
  }

  @Test
  void inspect_returns_raw_for_a_class_the_fallback_does_not_cover() {
    Codec<Shape> codec = shapeCodecWithFallback();

    Object result = Inspector.inspect(codec, new Triangle());

    assertThat(result).isInstanceOf(Triangle.class);
  }

  @Test
  void inspect_returns_raw_when_codec_not_introspectable() {
    Codec<Circle> plainCircleCodec =
        new Codec<>() {
          @Override
          public EncodeResult encode(Circle value, OutputStream output) {
            // not used in this test
            return new EncodeResult(0, 0);
          }

          @Override
          public Circle decode(InputStream input) {
            // not used in this test
            return null;
          }
        };
    Codec<Shape> codec =
        Codecs.<Integer, Shape>choice(Codecs.uint8())
            .on(1, Circle.class, plainCircleCodec)
            .on(2, Rectangle.class, RECTANGLE_CODEC)
            .build();

    Object result = Inspector.inspect(codec, new Circle(42));

    assertThat(result).isInstanceOf(Circle.class);
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

  /** Captures an unrecognised alternative as its whole span, tag included. */
  static class RawShape extends Shape {
    final byte[] raw;

    RawShape(byte[] raw) {
      this.raw = Arrays.copyOf(raw, raw.length);
    }
  }

  static class UnknownShape extends Shape {
    final int tag;
    final byte[] body;

    UnknownShape(int tag, byte[] body) {
      this.tag = tag;
      this.body = Arrays.copyOf(body, body.length);
    }
  }

  static class InnerCircle extends Shape {
    private int radius;

    int getRadius() {
      return radius;
    }

    void setRadius(int radius) {
      this.radius = radius;
    }
  }
}
