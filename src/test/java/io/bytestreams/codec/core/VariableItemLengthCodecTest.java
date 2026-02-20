package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.bytestreams.codec.core.util.Strings;
import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
class VariableItemLengthCodecTest {

  private static Codec<String> codePointCodec(Charset charset) {
    return Codecs.prefixed(
        Codecs.uint8(), Strings::codePointCount, length -> Codecs.ofCharset(charset, length));
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_code_point_count(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    Codec<String> codec = codePointCodec(charset);
    int codePoints = (int) value.codePoints().count();

    for (int length = 0; length <= codePoints; length++) {
      String substring = value.substring(0, value.offsetByCodePoints(0, length));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      EncodeResult returned = codec.encode(substring, output);
      assertThat(returned.count()).isEqualTo(length);
      assertThat(returned.bytes()).isEqualTo(1 + substring.getBytes(charset).length);

      byte[] bytes = output.toByteArray();
      assertThat(bytes[0] & 0xFF).isEqualTo(length);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_code_point_count(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    Codec<String> codec = codePointCodec(charset);
    int codePoints = (int) value.codePoints().count();

    for (int length = 0; length <= codePoints; length++) {
      String expected = value.substring(0, value.offsetByCodePoints(0, length));
      byte[] content = expected.getBytes(charset);
      byte[] inputBytes = new byte[content.length + 1];
      inputBytes[0] = (byte) length;
      System.arraycopy(content, 0, inputBytes, 1, content.length);

      ByteArrayInputStream input = new ByteArrayInputStream(inputBytes);
      String decoded = codec.decode(input);

      assertThat(decoded).isEqualTo(expected);
      assertThat(input.available()).isZero();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void roundtrip_code_point_count(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    Codec<String> codec = codePointCodec(charset);
    int codePoints = (int) value.codePoints().count();

    for (int length = 0; length <= codePoints; length++) {
      String original = value.substring(0, value.offsetByCodePoints(0, length));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(original, output);
      String decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
      assertThat(decoded).isEqualTo(original);
    }
  }

  private static Codec<String> hexDigitCodec() {
    return Codecs.prefixed(Codecs.uint8(), String::length, Codecs::hex);
  }

  @Test
  void encode_hex_with_odd_digit_count() throws IOException {
    Codec<String> codec = hexDigitCodec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("abc", output);

    byte[] bytes = output.toByteArray();
    // Length prefix encodes digit count (3), not byte count (2)
    assertThat(bytes[0] & 0xFF).isEqualTo(3);
    // Value bytes: "abc" left-padded to "0abc" → [0x0a, 0xbc]
    assertThat(bytes[1]).isEqualTo((byte) 0x0a);
    assertThat(bytes[2]).isEqualTo((byte) 0xbc);
    assertThat(result.count()).isEqualTo(3);
    assertThat(result.bytes()).isEqualTo(3);
  }

  @Test
  void decode_hex_with_odd_digit_count() throws IOException {
    Codec<String> codec = hexDigitCodec();
    // [digit count = 3] [0x0a, 0xbc] → 3 digits from 2 bytes
    byte[] inputBytes = new byte[] {3, 0x0a, (byte) 0xbc};

    String decoded = codec.decode(new ByteArrayInputStream(inputBytes));

    assertThat(decoded).isEqualTo("ABC");
  }

  @Test
  void decode_insufficient_data() {
    Codec<String> codec = codePointCodec(UTF_8);
    byte[] inputBytes = new byte[] {5, 'a', 'b'}; // count=5 but only 2 bytes of content
    ByteArrayInputStream input = new ByteArrayInputStream(inputBytes);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_empty_stream() {
    Codec<String> codec = codePointCodec(Charset.defaultCharset());
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void constructor_null_length_codec() {
    assertThatThrownBy(
            () -> new VariableItemLengthCodec<>(null, Strings::codePointCount, Codecs::ascii))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("lengthCodec");
  }

  @Test
  void constructor_null_length_of() {
    Codec<Integer> uint8 = Codecs.uint8();
    assertThatThrownBy(() -> new VariableItemLengthCodec<>(uint8, null, Codecs::ascii))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("lengthOf");
  }

  @Test
  void constructor_null_codec_factory() {
    Codec<Integer> uint8 = Codecs.uint8();
    assertThatThrownBy(
            () -> new VariableItemLengthCodec<String>(uint8, Strings::codePointCount, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("codecFactory");
  }
}
