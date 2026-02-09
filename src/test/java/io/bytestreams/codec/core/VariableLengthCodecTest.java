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
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
class VariableLengthCodecTest {

  private static Codec<Integer> lengthCodec() {
    return new UnsignedByteCodec();
  }

  private static Function<Integer, Codec<String>> valueCodecProvider(Charset charset) {
    return length -> new CodePointStringCodec(length, charset);
  }

  private static VariableLengthCodec<String> variableLengthCodec(Charset charset) {
    return new VariableLengthCodec<>(
        lengthCodec(), valueCodecProvider(charset), value -> (int) value.codePoints().count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_variable_lengths(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    VariableLengthCodec<String> codec = variableLengthCodec(charset);
    int codePoints = (int) value.codePoints().count();

    for (int length = 0; length <= codePoints; length++) {
      String substring = value.substring(0, value.offsetByCodePoints(0, length));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(substring, output);

      byte[] bytes = output.toByteArray();
      assertThat(bytes[0] & 0xFF).isEqualTo(length);
      assertThat(Arrays.copyOfRange(bytes, 1, bytes.length)).isEqualTo(substring.getBytes(charset));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_variable_lengths(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    VariableLengthCodec<String> codec = variableLengthCodec(charset);
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
  void roundtrip_variable_lengths(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    VariableLengthCodec<String> codec = variableLengthCodec(charset);
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
    VariableLengthCodec<String> codec = variableLengthCodec(UTF_8);
    byte[] inputBytes = new byte[] {5, 'a', 'b'}; // length=5 but only 2 bytes of content
    ByteArrayInputStream input = new ByteArrayInputStream(inputBytes);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_empty_stream() {
    VariableLengthCodec<String> codec = variableLengthCodec(UTF_8);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void constructor_null_length_codec() {
    assertThatThrownBy(
            () ->
                new VariableLengthCodec<>(
                    null, length -> new CodePointStringCodec(length, UTF_8), v -> 0))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("lengthCodec");
  }

  @Test
  void constructor_null_value_codec_provider() {
    Codec<Integer> lengthCodec = new UnsignedByteCodec();

    assertThatThrownBy(() -> new VariableLengthCodec<>(lengthCodec, null, v -> 0))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("valueCodecProvider");
  }

  @Test
  void constructor_null_length_provider() {
    Codec<Integer> lengthCodec = new UnsignedByteCodec();
    Function<Integer, Codec<String>> valueCodecProvider =
        length -> new CodePointStringCodec(length, UTF_8);

    assertThatThrownBy(() -> new VariableLengthCodec<>(lengthCodec, valueCodecProvider, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("lengthProvider");
  }
}
