package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A spec declared on a base class must be usable in the builder of a subclass. */
class SequentialObjectCodecSupertypeSpecTest {

  static class Base {
    int id;
    String note;
  }

  static class Derived extends Base {
    int extra;
  }

  static final FieldSpec<Base, Integer> ID =
      FieldSpec.of("id", Codecs.uint8(), b -> b.id, (b, v) -> b.id = v);

  static final FieldSpec<Base, String> NOTE =
      FieldSpec.of(
          "note",
          Codecs.ascii(3),
          b -> b.note,
          (b, v) -> b.note = v,
          b -> b.id > 0); // present only when id is set

  static SequentialObjectCodec<Derived> codec() {
    return SequentialObjectCodec.<Derived>builder(Derived::new)
        .field(ID)
        .field(NOTE)
        .field("extra", Codecs.uint8(), d -> d.extra, (d, v) -> d.extra = v)
        .build();
  }

  private static byte[] encode(Derived value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec().encode(value, output);
    return output.toByteArray();
  }

  @Test
  void encode_and_decode_with_base_class_specs() throws IOException {
    Derived original = new Derived();
    original.id = 7;
    original.note = "abc";
    original.extra = 9;

    byte[] bytes = encode(original);
    Derived decoded = codec().decode(new ByteArrayInputStream(bytes));

    assertThat(bytes).containsExactly(7, 'a', 'b', 'c', 9);
    assertThat(decoded.id).isEqualTo(7);
    assertThat(decoded.note).isEqualTo("abc");
    assertThat(decoded.extra).isEqualTo(9);
  }

  @Test
  void base_class_presence_predicate_skips_field_when_false() throws IOException {
    Derived original = new Derived();
    original.extra = 9;

    byte[] bytes = encode(original);
    Derived decoded = codec().decode(new ByteArrayInputStream(bytes));

    assertThat(bytes).containsExactly(0, 9);
    assertThat(decoded.note).isNull();
    assertThat(decoded.extra).isEqualTo(9);
  }

  @Test
  void inspect_with_base_class_specs() {
    Derived present = new Derived();
    present.id = 7;
    present.note = "abc";
    present.extra = 9;
    Derived absent = new Derived();

    assertThat(codec().inspect(present)).isEqualTo(Map.of("id", 7, "note", "abc", "extra", 9));
    assertThat(codec().inspect(absent)).isEqualTo(Map.of("id", 0, "extra", 0));
  }

  @Test
  void explicit_getter_setter_and_presence_may_be_declared_on_base_class() throws IOException {
    SequentialObjectCodec<Derived> codec =
        SequentialObjectCodec.<Derived>builder(Derived::new)
            .field("id", Codecs.uint8(), (Base b) -> b.id, (Base b, Integer v) -> b.id = v)
            .field(
                "note",
                Codecs.ascii(3),
                (Base b) -> b.note,
                (Base b, String v) -> b.note = v,
                (Base b) -> b.id > 0)
            .build();
    Derived original = new Derived();
    original.id = 1;
    original.note = "xyz";

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);
    Derived decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded.note).isEqualTo("xyz");
  }

  static class Base2 {
    int id;

    int getId() {
      return id;
    }

    void setId(int id) {
      this.id = id;
    }
  }

  static class Left extends Base2 {}

  static class Right extends Base2 {}

  @Test
  void method_references_on_base_class_and_spec_shared_by_two_subclasses() throws IOException {
    FieldSpec<Base2, Integer> shared =
        FieldSpec.of("id", Codecs.uint8(), Base2::getId, Base2::setId);
    SequentialObjectCodec<Left> left =
        SequentialObjectCodec.<Left>builder(Left::new).field(shared).build();
    SequentialObjectCodec<Right> right =
        SequentialObjectCodec.<Right>builder(Right::new)
            .field("id", Codecs.uint8(), Base2::getId, Base2::setId)
            .build();
    Left l = new Left();
    l.id = 3;
    Right r = new Right();
    r.id = 4;

    ByteArrayOutputStream lo = new ByteArrayOutputStream();
    left.encode(l, lo);
    ByteArrayOutputStream ro = new ByteArrayOutputStream();
    right.encode(r, ro);

    assertThat(left.decode(new ByteArrayInputStream(lo.toByteArray())).id).isEqualTo(3);
    assertThat(right.decode(new ByteArrayInputStream(ro.toByteArray())).id).isEqualTo(4);
  }

  @Test
  void field_spec_of_accepts_base_class_functions() {
    FieldSpec<Derived, Integer> spec =
        FieldSpec.of(
            "id",
            Codecs.uint8(),
            (Base b) -> b.id,
            (Base b, Integer v) -> b.id = v,
            (Base b) -> b.id > 0);
    Derived d = new Derived();

    assertThat(spec.presence().test(d)).isFalse();
    spec.set(d, 5);
    assertThat(spec.get(d)).isEqualTo(5);
    assertThat(spec.presence().test(d)).isTrue();
  }
}
