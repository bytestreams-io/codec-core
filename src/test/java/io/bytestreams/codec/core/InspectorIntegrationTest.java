package io.bytestreams.codec.core;

import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class InspectorIntegrationTest {

  @Test
  void inspect_delegates_to_inspectable_codec() {
    Codec<String> codec = new InspectableStringCodec();

    Object result = Inspector.inspect(codec, "hello");

    assertThat(result).isEqualTo("HELLO");
  }

  static class InspectableStringCodec implements Codec<String>, Inspectable<String> {
    @Override
    public EncodeResult encode(String value, OutputStream output) {
      // not used in this test
      return EncodeResult.EMPTY;
    }

    @Override
    public String decode(InputStream input) {
      // not used in this test
      return null;
    }

    @Override
    public Object inspect(String value) {
      return value.toUpperCase();
    }
  }

  @Test
  void inspect_recurses_through_prefixed_wrapper() {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value",
                Codecs.uint16(),
                TestFixtures.Inner::getValue,
                TestFixtures.Inner::setValue)
            .build();

    Codec<List<TestFixtures.Inner>> prefixedList =
        Codecs.prefixed(Codecs.uint16(), List::size, length -> Codecs.listOf(innerCodec, length));

    TestFixtures.Inner a = new TestFixtures.Inner();
    a.setValue(10);
    TestFixtures.Inner b = new TestFixtures.Inner();
    b.setValue(20);

    Object result = Inspector.inspect(prefixedList, List.of(a, b));

    assertThat(result).isEqualTo(List.of(Map.of("value", 10), Map.of("value", 20)));
  }

  @Test
  void inspect_recurses_through_xmap_wrapper() {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value",
                Codecs.uint16(),
                TestFixtures.Inner::getValue,
                TestFixtures.Inner::setValue)
            .build();

    Codec<TestFixtures.Inner> mapped = innerCodec.xmap(identity(), identity());

    TestFixtures.Inner obj = new TestFixtures.Inner();
    obj.setValue(42);

    Object result = Inspector.inspect(mapped, obj);

    assertThat(result).isEqualTo(Map.of("value", 42));
  }

  @Test
  void mdc_path_tracks_tagged_codec_nested_inside_sequential() throws IOException {
    List<String> observedPaths = new ArrayList<>();

    Codec<Integer> capturingCodec =
        new Codec<>() {
          @Override
          public EncodeResult encode(Integer value, OutputStream output) throws IOException {
            observedPaths.add(MDC.get("codec.field"));
            output.write(value >> 8);
            output.write(value);
            return EncodeResult.ofBytes(2);
          }

          @Override
          public Integer decode(InputStream input) throws IOException {
            observedPaths.add(MDC.get("codec.field"));
            return (input.read() << 8) | input.read();
          }
        };

    Codec<String> tagCodec = Codecs.ofCharset(Charset.defaultCharset(), 4);
    TaggedObjectCodec<TestFixtures.TestTagged, String> taggedCodec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, tagCodec)
            .tag("code", capturingCodec)
            .build();

    SequentialObjectCodec<TestFixtures.Outer> outerCodec =
        SequentialObjectCodec.<TestFixtures.Outer>builder(TestFixtures.Outer::new)
            .field("id", Codecs.uint16(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
            .field("tags", taggedCodec, TestFixtures.Outer::getTags, TestFixtures.Outer::setTags)
            .build();

    TestFixtures.TestTagged tagged = new TestFixtures.TestTagged();
    tagged.add("code", 42);

    TestFixtures.Outer obj = new TestFixtures.Outer();
    obj.setId(1);
    obj.setTags(tagged);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    outerCodec.encode(obj, out);
    outerCodec.decode(new ByteArrayInputStream(out.toByteArray()));

    assertThat(observedPaths).contains("tags.code");
    assertThat(MDC.get("codec.field")).isNull();
  }
}
