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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
class VariableLengthCodecTest {

  private static Codec<EncodeResult> lengthCodec() {
    UnsignedByteCodec byteCodec = new UnsignedByteCodec();
    return new Codec<>() {
      @Override
      public EncodeResult encode(EncodeResult value, OutputStream output) throws IOException {
        return byteCodec.encode(value.bytes(), output);
      }

      @Override
      public EncodeResult decode(InputStream input) throws IOException {
        return EncodeResult.ofBytes(byteCodec.decode(input));
      }
    };
  }

  private static VariableLengthCodec<String> variableLengthCodec(Charset charset) {
    return new VariableLengthCodec<>(
        lengthCodec(), StreamCodePointStringCodec.builder().charset(charset).build());
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
      EncodeResult returned = codec.encode(substring, output);
      assertThat(returned.length()).isEqualTo(length);
      assertThat(returned.bytes()).isEqualTo(1 + substring.getBytes(charset).length);

      byte[] bytes = output.toByteArray();
      assertThat(bytes[0] & 0xFF).isEqualTo(substring.getBytes(charset).length);
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

  private static Codec<EncodeResult> hexDigitCountLengthCodec() {
    UnsignedByteCodec byteCodec = new UnsignedByteCodec();
    return new Codec<>() {
      @Override
      public EncodeResult encode(EncodeResult value, OutputStream output) throws IOException {
        return byteCodec.encode(value.length(), output);
      }

      @Override
      public EncodeResult decode(InputStream input) throws IOException {
        int digitCount = byteCodec.decode(input);
        return new EncodeResult(digitCount, (digitCount + 1) / 2);
      }
    };
  }

  @Test
  void encode_hex_with_odd_digit_count() throws IOException {
    VariableLengthCodec<String> codec =
        new VariableLengthCodec<>(
            hexDigitCountLengthCodec(), StreamHexStringCodec.builder().build());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("abc", output);

    byte[] bytes = output.toByteArray();
    // Length prefix encodes digit count (3), not byte count (2)
    assertThat(bytes[0] & 0xFF).isEqualTo(3);
    // Value bytes: "abc" left-padded to "0abc" → [0x0a, 0xbc]
    assertThat(bytes[1]).isEqualTo((byte) 0x0a);
    assertThat(bytes[2]).isEqualTo((byte) 0xbc);
    assertThat(result.length()).isEqualTo(3);
    assertThat(result.bytes()).isEqualTo(3);
  }

  @Test
  void decode_hex_with_odd_digit_count() throws IOException {
    VariableLengthCodec<String> codec =
        new VariableLengthCodec<>(
            hexDigitCountLengthCodec(), StreamHexStringCodec.builder().build());
    // [digit count = 3] [0x0a, 0xbc] → (3+1)/2 = 2 bytes to read
    byte[] inputBytes = new byte[] {3, 0x0a, (byte) 0xbc};

    String decoded = codec.decode(new ByteArrayInputStream(inputBytes));

    assertThat(decoded).isEqualTo("0abc");
  }

  @Test
  void constructor_null_length_codec() {
    Codec<String> valueCodec = variableLengthCodec(UTF_8);

    assertThatThrownBy(() -> new VariableLengthCodec<>(null, valueCodec))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("lengthCodec");
  }

  @Test
  void constructor_null_value_codec() {
    Codec<EncodeResult> lengthCodec = lengthCodec();

    assertThatThrownBy(() -> new VariableLengthCodec<>(lengthCodec, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("valueCodec");
  }
}
