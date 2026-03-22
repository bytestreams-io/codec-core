package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class TaggedObjectCodecLoggingTest {

  private static final Codec<String> TAG_CODEC = Codecs.ofCharset(Charset.defaultCharset(), 4);

  @Test
  void encode_with_existing_mdc_path_restores_previous_value() throws Exception {
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
            return (input.read() << 8) | input.read();
          }
        };

    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", capturingCodec)
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();
    original.add("code", 42);

    try (var ignored = setMdc("codec.field", "parent")) {
      codec.encode(original, new ByteArrayOutputStream());
    }

    assertThat(observedPaths).containsExactly("parent.code");
  }

  @Test
  void decode_with_existing_mdc_path_restores_previous_value() throws Exception {
    List<String> observedPaths = new ArrayList<>();

    Codec<Integer> capturingCodec =
        new Codec<>() {
          @Override
          public EncodeResult encode(Integer value, OutputStream output) throws IOException {
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

    TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
        TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                TestFixtures.TestTagged::new, TAG_CODEC)
            .tag("code", capturingCodec)
            .build();

    TestFixtures.TestTagged original = new TestFixtures.TestTagged();
    original.add("code", 42);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    codec.encode(original, out);

    try (var ignored = setMdc("codec.field", "parent")) {
      codec.decode(new ByteArrayInputStream(out.toByteArray()));
    }

    assertThat(observedPaths).containsExactly("parent.code");
  }

  @Test
  void encode_decode_works_with_logging_disabled() throws Exception {
    try (var ignored = TestFixtures.disableLogging(TaggedObjectCodec.class)) {
      TaggedObjectCodec<TestFixtures.TestTagged, String> codec =
          TaggedObjectCodec.<TestFixtures.TestTagged, String>builder(
                  TestFixtures.TestTagged::new, TAG_CODEC)
              .tag("code", Codecs.uint16())
              .build();

      TestFixtures.TestTagged obj = new TestFixtures.TestTagged();
      obj.add("code", 42);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.encode(obj, out);
      TestFixtures.TestTagged decoded = codec.decode(new ByteArrayInputStream(out.toByteArray()));

      assertThat(decoded.<Integer>getAll("code")).containsExactly(42);
    }
  }

  private static AutoCloseable setMdc(String key, String value) {
    MDC.put(key, value);
    return () -> MDC.remove(key);
  }
}
