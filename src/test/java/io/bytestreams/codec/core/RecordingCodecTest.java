package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecordingCodecTest {

  @Test
  void decode_returns_value_and_raw_bytes() throws IOException {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value", Codecs.uint8(), TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();
    RecordingCodec<TestFixtures.Inner> codec = RecordingCodec.of(innerCodec);

    byte[] raw = {42};
    Recorded<TestFixtures.Inner> result = codec.decode(new ByteArrayInputStream(raw));

    assertThat(result.value().getValue()).isEqualTo(42);
    assertThat(result.rawBytes()).isEqualTo(raw);
  }

  @Test
  void encode_delegates_to_inner_codec() throws IOException {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value", Codecs.uint8(), TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();
    RecordingCodec<TestFixtures.Inner> codec = new RecordingCodec<>(innerCodec);

    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(42);
    Recorded<TestFixtures.Inner> recorded = new Recorded<>(inner, new byte[] {42});

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    codec.encode(recorded, out);

    assertThat(out.toByteArray()).containsExactly(42);
  }

  @Test
  void roundtrip() throws IOException {
    SequentialObjectCodec<TestFixtures.Outer> innerCodec =
        Codecs.<TestFixtures.Outer>sequential(TestFixtures.Outer::new)
            .field("id", Codecs.uint16(), TestFixtures.Outer::getId, TestFixtures.Outer::setId)
            .field(
                "inner",
                Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
                    .field(
                        "value",
                        Codecs.uint8(),
                        TestFixtures.Inner::getValue,
                        TestFixtures.Inner::setValue)
                    .build(),
                TestFixtures.Outer::getInner,
                TestFixtures.Outer::setInner)
            .build();
    RecordingCodec<TestFixtures.Outer> codec = new RecordingCodec<>(innerCodec);

    TestFixtures.Outer obj = new TestFixtures.Outer();
    obj.setId(1);
    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(42);
    obj.setInner(inner);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    innerCodec.encode(obj, out);
    byte[] originalBytes = out.toByteArray();

    Recorded<TestFixtures.Outer> result = codec.decode(new ByteArrayInputStream(originalBytes));

    assertThat(result.value().getId()).isEqualTo(1);
    assertThat(result.value().getInner().getValue()).isEqualTo(42);
    assertThat(result.rawBytes()).isEqualTo(originalBytes);
  }

  @Test
  void inspect_delegates_to_inner_codec() {
    SequentialObjectCodec<TestFixtures.Inner> innerCodec =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value", Codecs.uint8(), TestFixtures.Inner::getValue, TestFixtures.Inner::setValue)
            .build();
    RecordingCodec<TestFixtures.Inner> codec = new RecordingCodec<>(innerCodec);

    TestFixtures.Inner inner = new TestFixtures.Inner();
    inner.setValue(42);
    Recorded<TestFixtures.Inner> recorded = new Recorded<>(inner, new byte[] {42});

    Object result = codec.inspect(recorded);

    assertThat(result).isEqualTo(Map.of("value", 42));
  }
}
