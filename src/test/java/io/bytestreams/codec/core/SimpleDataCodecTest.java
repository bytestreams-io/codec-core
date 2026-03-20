package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class SimpleDataCodecTest {

  @Test
  void encode_decode_with_inline_field_specs() throws IOException {
    Codec<SimpleData> codec =
        Codecs.<SimpleData>sequential(SimpleData::new)
            .field(SimpleData.field("id", Codecs.uint16()))
            .field(SimpleData.field("name", Codecs.ascii(5)))
            .build();

    SimpleData original = new SimpleData();
    original.set("id", 42);
    original.set("name", "hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    SimpleData decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.<Integer>get("id")).isEqualTo(42);
    assertThat(decoded.<String>get("name")).isEqualTo("hello");
  }

  @Test
  void encode_decode_equals() throws IOException {
    Codec<SimpleData> codec =
        Codecs.<SimpleData>sequential(SimpleData::new)
            .field(SimpleData.field("id", Codecs.uint16()))
            .field(SimpleData.field("name", Codecs.ascii(5)))
            .build();

    SimpleData original = new SimpleData();
    original.set("id", 42);
    original.set("name", "hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    SimpleData decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded).isEqualTo(original);
  }
}
