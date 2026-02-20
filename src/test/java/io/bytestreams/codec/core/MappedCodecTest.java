package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MappedCodecTest {

  @Test
  void xmap_encode() throws IOException {
    Codec<UUID> codec =
        new FixedCodePointStringCodec(36, US_ASCII).xmap(UUID::fromString, UUID::toString);
    UUID value = UUID.randomUUID();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    EncodeResult result = codec.encode(value, output);
    assertThat(output.toString(US_ASCII)).isEqualTo(value.toString());
    assertThat(result.count()).isEqualTo(36);
    assertThat(result.bytes()).isEqualTo(36);
  }

  @Test
  void xmap_decode() throws IOException {
    Codec<UUID> codec =
        new FixedCodePointStringCodec(36, US_ASCII).xmap(UUID::fromString, UUID::toString);
    UUID expected = UUID.randomUUID();
    ByteArrayInputStream input = new ByteArrayInputStream(expected.toString().getBytes(US_ASCII));
    assertThat(codec.decode(input)).isEqualTo(expected);
  }

  @Test
  void xmap_chained() throws IOException {
    Codec<String> codec =
        new FixedCodePointStringCodec(5, US_ASCII)
            .xmap(String::trim, s -> String.format("%-5s", s))
            .xmap(String::toUpperCase, String::toLowerCase);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode("Hi", output);
    assertThat(output.toString(US_ASCII)).isEqualTo("hi   ");
    ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(US_ASCII));
    assertThat(codec.decode(input)).isEqualTo("HELLO");
  }
}
