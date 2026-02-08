package io.bytestreams.codec.core.util;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
class CodePointStreamReaderTest {
  private static CharsetDecoder getDecoder(Charset charset) {
    return charset
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  @Test
  void read_utf8_ascii(@Randomize String value) throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(UTF_8));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThat(reader.read(1)).isEqualTo(value.substring(0, 1));
    assertThat(input.available()).isEqualTo(4);
  }

  @Test
  void read_utf8_multi_byte(@Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(UTF_8));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThat(reader.read(1)).isEqualTo(value.substring(0, value.offsetByCodePoints(0, 1)));
    String remaining = value.substring(value.offsetByCodePoints(0, 1));
    assertThat(input.available()).isEqualTo(remaining.getBytes(UTF_8).length);
  }

  @Test
  void read_utf8_surrogate_pair(@Randomize(unicodeBlocks = "EMOTICONS") String value)
      throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(UTF_8));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThat(reader.read(1)).isEqualTo(value.substring(0, value.offsetByCodePoints(0, 1)));
    String remaining = value.substring(value.offsetByCodePoints(0, 1));
    assertThat(input.available()).isEqualTo(remaining.getBytes(UTF_8).length);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-16BE", "UTF-16LE"})
  void read_utf16_multi_byte(
      String charsetName, @Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(charset));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(charset));

    assertThat(reader.read(1)).isEqualTo(value.substring(0, value.offsetByCodePoints(0, 1)));
    String remaining = value.substring(value.offsetByCodePoints(0, 1));
    assertThat(input.available()).isEqualTo(remaining.codePoints().count() * 2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-16BE", "UTF-16LE"})
  void read_utf16_surrogate_pair(
      String charsetName, @Randomize(unicodeBlocks = "EMOTICONS") String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(charset));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(charset));

    assertThat(reader.read(1)).isEqualTo(value.substring(0, value.offsetByCodePoints(0, 1)));
    String remaining = value.substring(value.offsetByCodePoints(0, 1));
    assertThat(input.available()).isEqualTo(remaining.codePoints().count() * 4);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16BE", "UTF-16LE"})
  void read_empty_stream(String charsetName) throws IOException {
    Charset charset = Charset.forName(charsetName);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(charset));

    assertThat(reader.read(1)).isNull();
  }

  @Test
  void read_eof_mid_sequence() {
    byte[] incomplete = new byte[] {(byte) 0xE4, (byte) 0xB8};
    ByteArrayInputStream input = new ByteArrayInputStream(incomplete);
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThatThrownBy(() -> reader.read(1))
        .isInstanceOf(EOFException.class)
        .hasMessageContaining("2 byte(s)");
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16BE", "UTF-16LE"})
  void read_invalid_surrogate_pair(
      String charsetName, @Randomize(unicodeBlocks = "EMOTICONS", length = 1) String value)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    byte[] bytes = value.getBytes(charset);
    bytes[bytes.length - 2] = bytes[bytes.length - 4];
    bytes[bytes.length - 1] = bytes[bytes.length - 3];

    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(charset));

    String replacement = charset.newDecoder().replacement();
    assertThat(reader.read(1)).isEqualTo(replacement);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16BE", "UTF-16LE"})
  void read_invalid_surrogate_pair_without_replacement(
      String charsetName, @Randomize(unicodeBlocks = "EMOTICONS", length = 1) String value) {
    Charset charset = Charset.forName(charsetName);
    byte[] bytes = value.getBytes(charset);
    bytes[bytes.length - 2] = bytes[bytes.length - 4];
    bytes[bytes.length - 1] = bytes[bytes.length - 3];

    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    CodePointStreamReader reader = new CodePointStreamReader(input, charset.newDecoder());

    assertThatThrownBy(() -> reader.read(1)).isInstanceOf(MalformedInputException.class);
  }

  @Test
  void read_multiple(@Randomize String value) throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(UTF_8));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThat(reader.read(5)).isEqualTo(value);
  }

  @Test
  void read_multiple_multi_byte(@Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(UTF_8));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThat(reader.read(5)).isEqualTo(value);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void read_non_positive(int count) {
    ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(UTF_8));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThatThrownBy(() -> reader.read(count))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(String.valueOf(count));
  }

  @Test
  void read_eof_partial() {
    ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(UTF_8));
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThatThrownBy(() -> reader.read(5))
        .isInstanceOf(EOFException.class)
        .hasMessage("Read 3 code point(s), expected 5");
  }

  @Test
  void read_eof_mid_sequence_partial() {
    byte[] data = new byte[] {'a', (byte) 0xE4, (byte) 0xB8};
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    CodePointStreamReader reader = new CodePointStreamReader(input, getDecoder(UTF_8));

    assertThatThrownBy(() -> reader.read(2))
        .isInstanceOf(EOFException.class)
        .hasMessageContaining("2 byte(s)");
  }
}
