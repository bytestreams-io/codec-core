package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;

import io.bytestreams.codec.core.util.BiMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
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
  void xmap_bimap_encode() throws IOException {
    BiMap<String, UUID> biMap =
        BiMap.of(
            Map.entry(
                "550e8400-e29b-41d4-a716-446655440000",
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
            Map.entry(
                "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")));
    Codec<UUID> codec = new FixedCodePointStringCodec(36, US_ASCII).xmap(biMap);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    EncodeResult result =
        codec.encode(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), output);
    assertThat(output.toString(US_ASCII)).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    assertThat(result.count()).isEqualTo(36);
    assertThat(result.bytes()).isEqualTo(36);
  }

  @Test
  void xmap_bimap_decode() throws IOException {
    BiMap<String, UUID> biMap =
        BiMap.of(
            Map.entry(
                "550e8400-e29b-41d4-a716-446655440000",
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
            Map.entry(
                "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")));
    Codec<UUID> codec = new FixedCodePointStringCodec(36, US_ASCII).xmap(biMap);
    UUID expected = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
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
