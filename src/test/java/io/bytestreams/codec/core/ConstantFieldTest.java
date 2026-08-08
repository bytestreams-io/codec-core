package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConstantFieldTest {

  private static Codec<Trailer> trailerCodec() {
    return Codecs.<Trailer>sequential(Trailer::new)
        .constant("type", Codecs.ascii(2), "BT")
        .field("count", Codecs.uint16(), Trailer::getCount, Trailer::setCount)
        .build();
  }

  @Test
  void encode_writes_constant_without_reading_the_object() throws IOException {
    Trailer trailer = new Trailer();
    trailer.setCount(7);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    trailerCodec().encode(trailer, output);

    assertThat(output.toByteArray()).containsExactly('B', 'T', 0x00, 0x07);
  }

  @Test
  void decode_verifies_constant_and_discards_it() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {'B', 'T', 0x00, 0x07});

    Trailer decoded = trailerCodec().decode(input);

    assertThat(decoded.getCount()).isEqualTo(7);
  }

  @Test
  void decode_rejects_mismatched_constant_with_field_path() {
    ByteArrayInputStream input = new ByteArrayInputStream("XX".getBytes(US_ASCII));
    Codec<Trailer> codec = trailerCodec();

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [type]: expected constant [BT] but got [XX]");
  }

  private static Codec<Trailer> magicCodec() {
    return Codecs.<Trailer>sequential(Trailer::new)
        .constant("magic", Codecs.binary(2), new byte[] {(byte) 0xCA, (byte) 0xFE})
        .field("count", Codecs.uint16(), Trailer::getCount, Trailer::setCount)
        .build();
  }

  @Test
  void binary_constant_matches_by_content() throws IOException {
    ByteArrayInputStream input =
        new ByteArrayInputStream(new byte[] {(byte) 0xCA, (byte) 0xFE, 0x00, 0x07});

    assertThat(magicCodec().decode(input).getCount()).isEqualTo(7);
  }

  @Test
  void binary_constant_mismatch_reports_hex() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {(byte) 0xBA, (byte) 0xD0});
    Codec<Trailer> codec = magicCodec();

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [magic]: expected constant [CAFE] but got [BAD0]");
  }

  @Test
  void constant_rejects_null_codec() {
    SequentialObjectCodec.Builder<Trailer> builder = Codecs.sequential(Trailer::new);
    assertThatThrownBy(() -> builder.constant("t", null, "BT"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codec");
  }

  @Test
  void constant_rejects_null_value() {
    SequentialObjectCodec.Builder<Trailer> builder = Codecs.sequential(Trailer::new);
    Codec<String> ascii = Codecs.ascii(2);
    assertThatThrownBy(() -> builder.constant("t", ascii, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }

  private static Codec<Trailer> paddedCodec() {
    return Codecs.<Trailer>sequential(Trailer::new)
        .constant("type", Codecs.ascii(2), "H ", t -> t.strip().equals("H"))
        .field("count", Codecs.uint16(), Trailer::getCount, Trailer::setCount)
        .build();
  }

  @Test
  void predicate_overload_accepts_variants() throws IOException {
    ByteArrayInputStream padded = new ByteArrayInputStream(new byte[] {'H', ' ', 0x00, 0x07});
    ByteArrayInputStream unpadded = new ByteArrayInputStream(new byte[] {'H', 'H', 0x00, 0x07});

    Codec<Trailer> codec = paddedCodec();

    assertThat(codec.decode(padded).getCount()).isEqualTo(7);
    assertThatThrownBy(() -> codec.decode(unpadded)).isInstanceOf(CodecException.class);
  }

  @Test
  void predicate_overload_writes_the_canonical_value() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    paddedCodec().encode(new Trailer(), output);

    assertThat(output.toByteArray()).startsWith('H', ' ');
  }

  @Test
  void inspect_includes_the_constant() {
    Trailer trailer = new Trailer();
    trailer.setCount(7);

    assertThat(Inspector.inspect(trailerCodec(), trailer))
        .isEqualTo(Map.of("type", "BT", "count", 7));
  }

  @Test
  void predicate_overload_rejects_a_value_it_would_not_accept() {
    SequentialObjectCodec.Builder<Trailer> builder = Codecs.sequential(Trailer::new);
    Codec<String> ascii = Codecs.ascii(2);

    assertThatThrownBy(() -> builder.constant("type", ascii, "H ", "H"::equals))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("H ");
  }

  static class Trailer {
    private int count;

    int getCount() {
      return count;
    }

    void setCount(int count) {
      this.count = count;
    }
  }
}
