package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class TaggedDataObjectCodecTest {

  @Test
  void encode_decode_single_tag() throws IOException {
    Codec<TaggedDataObject<String>> codec =
        Codecs.<String>tagged(Codecs.ascii(4)).tag("code", Codecs.uint16()).build();

    TaggedDataObject<String> original = new TaggedDataObject<>();
    original.add("code", 42);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TaggedDataObject<String> decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.<Integer>getAll("code")).containsExactly(42);
  }

  @Test
  void encode_decode_multiple_tags() throws IOException {
    Codec<TaggedDataObject<String>> codec =
        Codecs.<String>tagged(Codecs.ascii(4))
            .tag("code", Codecs.uint16())
            .tag("name", Codecs.ascii(5))
            .build();

    TaggedDataObject<String> original = new TaggedDataObject<>();
    original.add("code", 42);
    original.add("name", "hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TaggedDataObject<String> decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.<Integer>getAll("code")).containsExactly(42);
    assertThat(decoded.<String>getAll("name")).containsExactly("hello");
  }

  @Test
  void encode_decode_duplicate_tags() throws IOException {
    Codec<TaggedDataObject<String>> codec =
        Codecs.<String>tagged(Codecs.ascii(4)).tag("code", Codecs.uint16()).build();

    TaggedDataObject<String> original = new TaggedDataObject<>();
    original.add("code", 1);
    original.add("code", 2);
    original.add("code", 3);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TaggedDataObject<String> decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.<Integer>getAll("code")).containsExactly(1, 2, 3);
  }

  @Test
  void encode_decode_equals() throws IOException {
    Codec<TaggedDataObject<String>> codec =
        Codecs.<String>tagged(Codecs.ascii(4))
            .tag("code", Codecs.uint16())
            .tag("name", Codecs.ascii(5))
            .build();

    TaggedDataObject<String> original = new TaggedDataObject<>();
    original.add("code", 42);
    original.add("name", "hello");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);

    TaggedDataObject<String> decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded).isEqualTo(original);
  }
}
