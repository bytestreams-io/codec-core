package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
class VariableByteLengthCodecTest {

  private static Codec<String> variableByteLengthCodec(Charset charset) {
    return Codecs.prefixed(Codecs.uint8(), Codecs.ofCharset(charset));
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_variable_lengths(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    Codec<String> codec = variableByteLengthCodec(charset);
    int codePoints = (int) value.codePoints().count();

    for (int length = 0; length <= codePoints; length++) {
      String substring = value.substring(0, value.offsetByCodePoints(0, length));
      byte[] encoded = substring.getBytes(charset);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      EncodeResult returned = codec.encode(substring, output);
      assertThat(returned.length()).isEqualTo(length);
      assertThat(returned.bytes()).isEqualTo(1 + encoded.length);

      byte[] bytes = output.toByteArray();
      assertThat(bytes[0] & 0xFF).isEqualTo(encoded.length);
      assertThat(Arrays.copyOfRange(bytes, 1, bytes.length)).isEqualTo(encoded);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_variable_lengths(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    Codec<String> codec = variableByteLengthCodec(charset);
    int codePoints = (int) value.codePoints().count();

    for (int length = 0; length <= codePoints; length++) {
      String expected = value.substring(0, value.offsetByCodePoints(0, length));
      byte[] content = expected.getBytes(charset);
      byte[] inputBytes = new byte[content.length + 1];
      inputBytes[0] = (byte) content.length;
      System.arraycopy(content, 0, inputBytes, 1, content.length);

      ByteArrayInputStream input = new ByteArrayInputStream(inputBytes);
      String decoded = codec.decode(input);

      assertThat(decoded).isEqualTo(expected);
      assertThat(input.available()).isZero();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void roundtrip_variable_lengths(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    Codec<String> codec = variableByteLengthCodec(charset);
    int codePoints = (int) value.codePoints().count();

    for (int length = 0; length <= codePoints; length++) {
      String original = value.substring(0, value.offsetByCodePoints(0, length));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(original, output);
      String decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));
      assertThat(decoded).isEqualTo(original);
    }
  }

  @Test
  void decode_insufficient_data() {
    Codec<String> codec = variableByteLengthCodec(UTF_8);
    byte[] inputBytes = new byte[] {5, 'a', 'b'}; // length=5 but only 2 bytes of content
    ByteArrayInputStream input = new ByteArrayInputStream(inputBytes);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_empty_stream() {
    Codec<String> codec = variableByteLengthCodec(UTF_8);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void constructor_null_length_codec() {
    Codec<String> ascii = Codecs.ascii();
    assertThatThrownBy(() -> new VariableByteLengthCodec<>(null, ascii))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("lengthCodec");
  }

  @Test
  void constructor_null_value_codec() {
    Codec<Integer> uint8 = Codecs.uint8();
    assertThatThrownBy(() -> new VariableByteLengthCodec<>(uint8, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("valueCodec");
  }
}
