package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class SequentialObjectCodecLoggingTest {

  @Test
  void decode_nested_codec_sets_mdc_field_path() throws IOException {
    List<String> observedPaths = new ArrayList<>();

    Codec<Integer> capturingCodec =
        new Codec<>() {
          @Override
          public EncodeResult encode(Integer value, OutputStream output) throws IOException {
            output.write(value);
            return EncodeResult.ofBytes(1);
          }

          @Override
          public Integer decode(InputStream input) throws IOException {
            observedPaths.add(MDC.get("codec.field"));
            return input.read();
          }
        };

    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
            .field(
                "value", capturingCodec, TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();

    SequentialObjectCodec<TestFixtures.Outer> outerCodec =
        SequentialObjectCodec.<TestFixtures.Outer>builder(TestFixtures.Outer::new)
            .field("id", Codecs.uint16(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
            .field("inner", innerCodec, TestFixtures.Outer::getInner, TestFixtures.Outer::setInner)
            .build();

    TestFixtures.Outer original = new TestFixtures.Outer();
    original.setId(1);
    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(42);
    original.setInner(inner);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    outerCodec.encode(original, out);
    outerCodec.decode(new ByteArrayInputStream(out.toByteArray()));

    assertThat(observedPaths).contains("inner.value");
    assertThat(MDC.get("codec.field")).isNull();
  }

  @Test
  void encode_nested_codec_sets_mdc_field_path() throws IOException {
    List<String> observedPaths = new ArrayList<>();

    Codec<Integer> capturingCodec =
        new Codec<>() {
          @Override
          public EncodeResult encode(Integer value, OutputStream output) throws IOException {
            observedPaths.add(MDC.get("codec.field"));
            output.write(value);
            return EncodeResult.ofBytes(1);
          }

          @Override
          public Integer decode(InputStream input) throws IOException {
            return input.read();
          }
        };

    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
            .field(
                "value", capturingCodec, TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();

    SequentialObjectCodec<TestFixtures.Outer> outerCodec =
        SequentialObjectCodec.<TestFixtures.Outer>builder(TestFixtures.Outer::new)
            .field("id", Codecs.uint16(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
            .field("inner", innerCodec, TestFixtures.Outer::getInner, TestFixtures.Outer::setInner)
            .build();

    TestFixtures.Outer obj = new TestFixtures.Outer();
    obj.setId(1);
    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(42);
    obj.setInner(inner);

    outerCodec.encode(obj, new ByteArrayOutputStream());

    assertThat(observedPaths).contains("inner.value");
    assertThat(MDC.get("codec.field")).isNull();
  }

  @Test
  void encode_decode_works_with_logging_disabled() throws Exception {
    try (var ignored = TestFixtures.disableLogging(SequentialObjectCodec.class)) {
      SequentialObjectCodec<TestFixtures.Outer> codec =
          SequentialObjectCodec.<TestFixtures.Outer>builder(TestFixtures.Outer::new)
              .field("id", Codecs.uint16(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
              .field(
                  "inner",
                  SequentialObjectCodec.<TestFixtures.Inner>builder(TestFixtures.Inner::new)
                      .field(
                          "value",
                          Codecs.uint8(),
                          TestFixtures.Inner::getValue,
                          TestFixtures.Inner::setValue)
                      .build(),
                  TestFixtures.Outer::getInner,
                  TestFixtures.Outer::setInner,
                  obj -> obj.getId() > 0)
              .build();

      TestFixtures.Outer obj = new TestFixtures.Outer();
      obj.setId(1);
      TestFixtures.Inner inner = new TestFixtures.Inner();
      inner.setValue(42);
      obj.setInner(inner);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.encode(obj, out);
      TestFixtures.Outer decoded = codec.decode(new ByteArrayInputStream(out.toByteArray()));

      assertThat(decoded.getId()).isEqualTo(1);
      assertThat(decoded.getInner().getValue()).isEqualTo(42);

      // Also test absent field path (id=0, inner skipped)
      TestFixtures.Outer absent = new TestFixtures.Outer();
      absent.setId(0);
      ByteArrayOutputStream out2 = new ByteArrayOutputStream();
      codec.encode(absent, out2);
      TestFixtures.Outer decodedAbsent = codec.decode(new ByteArrayInputStream(out2.toByteArray()));
      assertThat(decodedAbsent.getId()).isZero();
    }
  }
}
