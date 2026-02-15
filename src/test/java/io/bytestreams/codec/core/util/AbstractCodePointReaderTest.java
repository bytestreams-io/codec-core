package io.bytestreams.codec.core.util;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
abstract class AbstractCodePointReaderTest {

  abstract InputStream createInputStream(byte[] data);

  abstract CodePointReader createReader(InputStream input, Charset charset);

  @Test
  void read_utf8_ascii(@Randomize String value) throws IOException {
    InputStream input = createInputStream(value.getBytes(UTF_8));
    CodePointReader reader = createReader(input, UTF_8);

    assertThat(reader.read(1)).isEqualTo(value.substring(0, 1));
    assertThat(input.available()).isEqualTo(4);
  }

  @Test
  void read_utf8_multi_byte(@Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    verifyReadOneCodePoint(value, UTF_8);
  }

  @Test
  void read_utf8_surrogate_pair(@Randomize(unicodeBlocks = "EMOTICONS") String value)
      throws IOException {
    verifyReadOneCodePoint(value, UTF_8);
  }

  private void verifyReadOneCodePoint(String value, Charset charset) throws IOException {
    InputStream input = createInputStream(value.getBytes(charset));
    CodePointReader reader = createReader(input, charset);
    assertThat(reader.read(1)).isEqualTo(value.substring(0, value.offsetByCodePoints(0, 1)));
    String remaining = value.substring(value.offsetByCodePoints(0, 1));
    assertThat(input.available()).isEqualTo(remaining.getBytes(charset).length);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-16BE", "UTF-16LE"})
  void read_utf16_multi_byte(
      String charsetName, @Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    InputStream input = createInputStream(value.getBytes(charset));
    CodePointReader reader = createReader(input, charset);

    assertThat(reader.read(1)).isEqualTo(value.substring(0, value.offsetByCodePoints(0, 1)));
    String remaining = value.substring(value.offsetByCodePoints(0, 1));
    assertThat(input.available()).isEqualTo(remaining.codePoints().count() * 2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-16BE", "UTF-16LE"})
  void read_utf16_surrogate_pair(
      String charsetName, @Randomize(unicodeBlocks = "EMOTICONS") String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    InputStream input = createInputStream(value.getBytes(charset));
    CodePointReader reader = createReader(input, charset);

    assertThat(reader.read(1)).isEqualTo(value.substring(0, value.offsetByCodePoints(0, 1)));
    String remaining = value.substring(value.offsetByCodePoints(0, 1));
    assertThat(input.available()).isEqualTo(remaining.codePoints().count() * 4);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16BE", "UTF-16LE"})
  void read_empty_stream(String charsetName) {
    Charset charset = Charset.forName(charsetName);
    InputStream input = createInputStream(new byte[0]);
    CodePointReader reader = createReader(input, charset);

    assertThatThrownBy(() -> reader.read(1))
        .isInstanceOf(EOFException.class)
        .hasMessage("Read 0 code point(s), expected 1");
  }

  @Test
  void read_multiple(@Randomize String value) throws IOException {
    verifyReadMultiple(value, UTF_8);
  }

  @Test
  void read_multiple_multi_byte(@Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    verifyReadMultiple(value, UTF_8);
  }

  private void verifyReadMultiple(String value, Charset charset) throws IOException {
    InputStream input = createInputStream(value.getBytes(charset));
    CodePointReader reader = createReader(input, charset);

    assertThat(reader.read(5)).isEqualTo(value);
  }

  @Test
  void read_zero() throws IOException {
    InputStream input = createInputStream("hello".getBytes(UTF_8));
    CodePointReader reader = createReader(input, UTF_8);

    assertThat(reader.read(0)).isEmpty();
    assertThat(input.available()).isEqualTo(5);
  }

  @Test
  void read_negative() {
    InputStream input = createInputStream("hello".getBytes(UTF_8));
    CodePointReader reader = createReader(input, UTF_8);

    assertThatThrownBy(() -> reader.read(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }

  @Test
  void read_eof_partial() {
    InputStream input = createInputStream("abc".getBytes(UTF_8));
    CodePointReader reader = createReader(input, UTF_8);

    assertThatThrownBy(() -> reader.read(5))
        .isInstanceOf(EOFException.class)
        .hasMessage("Read 3 code point(s), expected 5");
  }

  @Test
  void stream_position_after_read(@Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    InputStream input = createInputStream(value.getBytes(UTF_8));
    CodePointReader reader = createReader(input, UTF_8);

    reader.read(3);
    String remaining = value.substring(value.offsetByCodePoints(0, 3));
    assertThat(input.available()).isEqualTo(remaining.getBytes(UTF_8).length);
  }
}
