package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
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
class FixedCodePointStringCodecTest {

  @Test
  void constructor_negative_length() {
    assertThatThrownBy(() -> new FixedCodePointStringCodec(-1, UTF_8))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "IBM1047", "ISO-8859-1"})
  void encode_single_byte_charset(
      String charsetName, @Randomize(unicodeBlocks = "BASIC_LATIN") String value)
      throws IOException {
    verifyEncode(value, Charset.forName(charsetName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16"})
  void encode_multi_byte_charset(
      String charsetName,
      @Randomize(unicodeBlocks = {"CJK_UNIFIED_IDEOGRAPHS", "EMOTICONS"}) String value)
      throws IOException {
    verifyEncode(value, Charset.forName(charsetName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ISO-8859-1", "UTF-8"})
  void encode_zero_length(String charsetName) throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(0, charset);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode("", output);

    assertThat(output.toByteArray()).isEmpty();
  }

  @Test
  void encode_wrong_code_point_count(@Randomize String value) {
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(10, Charset.defaultCharset());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(value, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("10")
        .hasMessageContaining("5");
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "IBM1047", "ISO-8859-1"})
  void decode_single_byte_charset(
      String charsetName, @Randomize(unicodeBlocks = "BASIC_LATIN") String value)
      throws IOException {
    verifyDecode(value, Charset.forName(charsetName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16"})
  void decode_multi_byte_charset(
      String charsetName,
      @Randomize(unicodeBlocks = {"CJK_UNIFIED_IDEOGRAPHS", "EMOTICONS"}) String value)
      throws IOException {
    verifyDecode(value, Charset.forName(charsetName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ISO-8859-1", "UTF-8"})
  void decode_zero_length(String charsetName) throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(0, charset);
    ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(charset));

    assertThat(codec.decode(input)).isEmpty();
    assertThat(input.available()).isEqualTo("hello".getBytes(charset).length);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_consumes_only_required_bytes(
      String charsetName, @Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(1, charset);
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(charset));

    String result = codec.decode(input);

    assertThat(result).isEqualTo(value.substring(0, value.offsetByCodePoints(0, 1)));
    assertThat(input.available()).isPositive();
  }

  @Test
  void decode_insufficient_data_single_byte() {
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(10, ISO_8859_1);
    ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(ISO_8859_1));

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_insufficient_data_multi_byte() {
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(10, Charset.defaultCharset());
    ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(UTF_8));

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_empty_stream_single_byte() {
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(5, ISO_8859_1);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_empty_stream_multi_byte() {
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(5, Charset.defaultCharset());
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @Test
  void decode_after_failed_decode() throws IOException {
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(3, UTF_8);

    // First decode fails: truncated 3-byte UTF-8 sequence (only 2 bytes of \u4e16)
    byte[] truncated = {(byte) 0xE4, (byte) 0xB8};
    ByteArrayInputStream failInput = new ByteArrayInputStream(truncated);
    assertThatThrownBy(() -> codec.decode(failInput)).isInstanceOf(EOFException.class);

    // Second decode should succeed despite the previous failure
    String expected = "\u4e16\u754c\u4eba";
    ByteArrayInputStream goodInput = new ByteArrayInputStream(expected.getBytes(UTF_8));
    assertThat(codec.decode(goodInput)).isEqualTo(expected);
  }

  @Test
  void constructor_default_charset() {
    assertThat(new FixedCodePointStringCodec(5, Charset.defaultCharset())).isNotNull();
  }

  @Test
  void constructor_charset_null() {
    assertThatThrownBy(() -> new FixedCodePointStringCodec(5, null))
        .isInstanceOf(NullPointerException.class);
  }

  private void verifyEncode(String value, Charset charset) throws IOException {
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(5, charset);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(value, output);

    assertThat(output.toByteArray()).isEqualTo(value.getBytes(charset));
  }

  private void verifyDecode(String value, Charset charset) throws IOException {
    FixedCodePointStringCodec codec = new FixedCodePointStringCodec(5, charset);
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(charset));

    assertThat(codec.decode(input)).isEqualTo(value);
  }
}
