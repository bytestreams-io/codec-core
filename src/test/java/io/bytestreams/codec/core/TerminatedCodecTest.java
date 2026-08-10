package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TerminatedCodecTest {

  private static final byte[] LF = {0x0A};
  private static final byte[] CRLF = {0x0D, 0x0A};
  private static final byte[] BACKSLASH = {0x5C};

  @Test
  void decode_reads_up_to_terminator() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), LF);
    ByteArrayInputStream input = new ByteArrayInputStream("hello\n".getBytes(US_ASCII));

    assertThat(codec.decode(input)).isEqualTo("hello");
  }

  @Test
  void decode_consumes_terminator_without_over_reading() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), LF);
    ByteArrayInputStream input = new ByteArrayInputStream("first\nsecond\n".getBytes(US_ASCII));

    assertThat(codec.decode(input)).isEqualTo("first");
    assertThat(codec.decode(input)).isEqualTo("second");
  }

  @Test
  void encode_appends_terminator() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), LF);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode("hello", output);

    assertThat(output.toString(US_ASCII)).isEqualTo("hello\n");
  }

  @Test
  void encode_result_counts_terminator_in_bytes_only() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), CRLF);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("hello", output);

    assertThat(result.count()).isEqualTo(5);
    assertThat(result.bytes()).isEqualTo(7);
  }

  @Test
  void encode_rejects_value_containing_terminator() {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), LF);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode("a\nb", output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("terminator");

    assertThat(output.toByteArray()).isEmpty();
  }

  @Test
  void decode_optional_accepts_missing_terminator_at_eof() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), LF);
    ByteArrayInputStream input = new ByteArrayInputStream("last".getBytes(US_ASCII));

    assertThat(codec.decode(input)).isEqualTo("last");
  }

  @Test
  void decode_required_rejects_missing_terminator_at_eof() {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), LF, Codecs.Termination.REQUIRED);
    ByteArrayInputStream input = new ByteArrayInputStream("last".getBytes(US_ASCII));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("terminator");
  }

  @Test
  void decode_empty_chunk_delegates_to_value_codec() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), LF);
    ByteArrayInputStream input = new ByteArrayInputStream("\n".getBytes(US_ASCII));

    assertThat(codec.decode(input)).isEmpty();
  }

  @Test
  void decode_exhausted_stream_delegates_to_value_codec() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), LF);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThat(codec.decode(input)).isEmpty();
  }

  @Test
  void decode_exhausted_stream_propagates_value_codec_failure() {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(5), LF);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_multi_byte_terminator() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), CRLF);
    ByteArrayInputStream input = new ByteArrayInputStream("a\r\nb\r\n".getBytes(US_ASCII));

    assertThat(codec.decode(input)).isEqualTo("a");
    assertThat(codec.decode(input)).isEqualTo("b");
  }

  @Test
  void decode_self_overlapping_terminator() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), "AAB".getBytes(US_ASCII));
    ByteArrayInputStream input = new ByteArrayInputStream("xAAAB".getBytes(US_ASCII));

    assertThat(codec.decode(input)).isEqualTo("xA");
  }

  @Test
  void decode_bare_terminator_inside_value_is_not_split() throws IOException {
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), CRLF);
    ByteArrayInputStream input = new ByteArrayInputStream("a\rb\r\n".getBytes(US_ASCII));

    assertThat(codec.decode(input)).isEqualTo("a\rb");
  }

  @Test
  void inspect_delegates_to_value_codec() {
    SequentialObjectCodec<TestFixtures.Inner> inner =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value",
                Codecs.uint16(),
                TestFixtures.Inner::getValue,
                TestFixtures.Inner::setValue)
            .build();
    Codec<TestFixtures.Inner> codec = Codecs.terminated(inner, LF);
    TestFixtures.Inner value = new TestFixtures.Inner();
    value.setValue(7);

    assertThat(Inspector.inspect(codec, value)).isEqualTo(Map.of("value", 7));
  }

  @Test
  void decode_list_of_lines_without_trailing_terminator() throws IOException {
    Codec<List<String>> codec = Codecs.listOf(Codecs.terminated(Codecs.ascii(), LF));
    ByteArrayInputStream input = new ByteArrayInputStream("a\nb\nc".getBytes(US_ASCII));

    assertThat(codec.decode(input)).containsExactly("a", "b", "c");
  }

  @Test
  void decode_list_of_lines_without_trailing_terminator_rejected_when_required() {
    Codec<List<String>> codec =
        Codecs.listOf(Codecs.terminated(Codecs.ascii(), LF, Codecs.Termination.REQUIRED));
    ByteArrayInputStream input = new ByteArrayInputStream("a\nb\nc".getBytes(US_ASCII));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("not found before end of stream");
  }

  @Test
  void separated_fields_inside_a_length_prefixed_scope() throws IOException {
    Codec<Address> address =
        Codecs.<Address>sequential(Address::new)
            .field(
                "name",
                Codecs.terminated(Codecs.ascii(), BACKSLASH),
                Address::getName,
                Address::setName)
            .field(
                "city",
                Codecs.terminated(Codecs.ascii(), BACKSLASH),
                Address::getCity,
                Address::setCity)
            .field("country", Codecs.ascii(), Address::getCountry, Address::setCountry)
            .build();
    Codec<Address> field = Codecs.prefixed(Codecs.asciiInt(2), address);
    ByteArrayInputStream input =
        new ByteArrayInputStream("14ACME\\LONDON\\GB rest".getBytes(US_ASCII));

    Address decoded = field.decode(input);

    assertThat(decoded.getName()).isEqualTo("ACME");
    assertThat(decoded.getCity()).isEqualTo("LONDON");
    assertThat(decoded.getCountry()).isEqualTo("GB");
    assertThat(new String(input.readAllBytes(), US_ASCII)).isEqualTo(" rest");
  }

  @Test
  void separated_fields_allow_empty_parts() throws IOException {
    Codec<Address> address =
        Codecs.<Address>sequential(Address::new)
            .field(
                "name",
                Codecs.terminated(Codecs.ascii(), BACKSLASH),
                Address::getName,
                Address::setName)
            .field(
                "city",
                Codecs.terminated(Codecs.ascii(), BACKSLASH),
                Address::getCity,
                Address::setCity)
            .field("country", Codecs.ascii(), Address::getCountry, Address::setCountry)
            .build();
    ByteArrayInputStream input = new ByteArrayInputStream("ACME\\\\GB".getBytes(US_ASCII));

    Address decoded = address.decode(input);

    assertThat(decoded.getCity()).isEmpty();
    assertThat(decoded.getCountry()).isEqualTo("GB");
  }

  @Test
  void decode_failure_carries_field_path() {
    Codec<Address> address =
        Codecs.<Address>sequential(Address::new)
            .field(
                "name",
                Codecs.terminated(Codecs.ascii(4), BACKSLASH),
                Address::getName,
                Address::setName)
            .build();
    ByteArrayInputStream input = new ByteArrayInputStream("AB\\".getBytes(US_ASCII));

    assertThatThrownBy(() -> address.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("field [name]");
  }

  static class Address {
    private String name;
    private String city;
    private String country;

    String getName() {
      return name;
    }

    void setName(String name) {
      this.name = name;
    }

    String getCity() {
      return city;
    }

    void setCity(String city) {
      this.city = city;
    }

    String getCountry() {
      return country;
    }

    void setCountry(String country) {
      this.country = country;
    }
  }

  @Test
  void constructor_rejects_empty_terminator() {
    Codec<String> ascii = Codecs.ascii();
    assertThatThrownBy(() -> Codecs.terminated(ascii, new byte[0]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("terminator");
  }

  @Test
  void constructor_copies_terminator() throws IOException {
    byte[] terminator = {0x0A};
    Codec<String> codec = Codecs.terminated(Codecs.ascii(), terminator);
    terminator[0] = 0x7C;
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode("hello", output);

    assertThat(output.toString(US_ASCII)).isEqualTo("hello\n");
  }

  @Test
  void constructor_rejects_null_terminator() {
    Codec<String> ascii = Codecs.ascii();
    assertThatThrownBy(() -> Codecs.terminated(ascii, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("terminator");
  }

  @Test
  void constructor_rejects_null_value_codec() {
    assertThatThrownBy(() -> Codecs.terminated(null, LF))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("valueCodec");
  }
}
