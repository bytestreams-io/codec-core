package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
class StreamCodePointStringCodecTest {

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16"})
  void encode(String charsetName, @Randomize(unicodeBlocks = "BASIC_LATIN") String value)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamCodePointStringCodec codec =
        StreamCodePointStringCodec.builder().charset(charset).build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode(value, output);

    assertThat(output.toByteArray()).isEqualTo(value.getBytes(charset));
  }

  @Test
  void encode_result(@Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    StreamCodePointStringCodec codec = StreamCodePointStringCodec.builder().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(value, output);

    assertThat(result.length()).isEqualTo(value.codePointCount(0, value.length()));
    assertThat(result.bytes()).isEqualTo(value.getBytes(UTF_8).length);
  }

  @Test
  void encode_empty_string() throws IOException {
    StreamCodePointStringCodec codec = StreamCodePointStringCodec.builder().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode("", output);

    assertThat(output.toByteArray()).isEmpty();
    assertThat(result).isEqualTo(EncodeResult.EMPTY);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16"})
  void decode(String charsetName, @Randomize(unicodeBlocks = "BASIC_LATIN") String value)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamCodePointStringCodec codec =
        StreamCodePointStringCodec.builder().charset(charset).build();
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(charset));

    assertThat(codec.decode(input)).isEqualTo(value);
    assertThat(input.available()).isZero();
  }

  @Test
  void decode_multi_byte(@Randomize(unicodeBlocks = "CJK_UNIFIED_IDEOGRAPHS") String value)
      throws IOException {
    StreamCodePointStringCodec codec = StreamCodePointStringCodec.builder().build();
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(UTF_8));

    assertThat(codec.decode(input)).isEqualTo(value);
  }

  @Test
  void decode_empty_stream() throws IOException {
    StreamCodePointStringCodec codec = StreamCodePointStringCodec.builder().build();
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThat(codec.decode(input)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ISO-8859-1", "UTF-8", "UTF-16"})
  void roundtrip(String charsetName, @Randomize(unicodeBlocks = "BASIC_LATIN") String value)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamCodePointStringCodec codec =
        StreamCodePointStringCodec.builder().charset(charset).build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);

    assertThat(codec.decode(new ByteArrayInputStream(output.toByteArray()))).isEqualTo(value);
  }

  @Test
  void builder_default_charset() throws IOException {
    StreamCodePointStringCodec codec = StreamCodePointStringCodec.builder().build();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    codec.encode("hello", output);

    assertThat(output.toByteArray()).isEqualTo("hello".getBytes(Charset.defaultCharset()));
  }

  @Test
  void builder_charset_null() {
    StreamCodePointStringCodec.Builder builder = StreamCodePointStringCodec.builder();
    assertThatThrownBy(() -> builder.charset(null)).isInstanceOf(NullPointerException.class);
  }
}
