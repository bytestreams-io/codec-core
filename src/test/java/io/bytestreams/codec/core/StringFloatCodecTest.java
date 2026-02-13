package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class StringFloatCodecTest {

  @Test
  void encode(@Randomize(floatMin = 0, floatMax = Float.MAX_VALUE) float value) throws IOException {
    String string = Float.toString(value);
    FixedCodePointStringCodec codePointCodec =
        FixedCodePointStringCodec.builder(string.length()).build();
    StringFloatCodec codec = new StringFloatCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toString(UTF_8)).isEqualTo(string);
  }

  @Test
  void encode_overflow() {
    FixedCodePointStringCodec codePointCodec = FixedCodePointStringCodec.builder(3).build();
    StringFloatCodec codec = new StringFloatCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    float value = 1234.5f;
    assertThatThrownBy(() -> codec.encode(value, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decode() throws IOException {
    String string = "3.14";
    FixedCodePointStringCodec codePointCodec =
        FixedCodePointStringCodec.builder(string.length()).build();
    StringFloatCodec codec = new StringFloatCodec(codePointCodec);
    ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes(UTF_8));
    assertThat(codec.decode(input)).isEqualTo(3.14f);
  }

  @Test
  void decode_invalid() {
    FixedCodePointStringCodec codePointCodec = FixedCodePointStringCodec.builder(3).build();
    StringFloatCodec codec = new StringFloatCodec(codePointCodec);
    ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(UTF_8));
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("failed to parse number from string")
        .hasCauseInstanceOf(NumberFormatException.class);
  }

  @Test
  void roundtrip(@Randomize(floatMin = 0, floatMax = Float.MAX_VALUE) float value)
      throws IOException {
    String string = Float.toString(value);
    FixedCodePointStringCodec codePointCodec =
        FixedCodePointStringCodec.builder(string.length()).build();
    StringFloatCodec codec = new StringFloatCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertThat(codec.decode(input)).isEqualTo(value);
  }
}
