package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.*;
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
class StringDoubleCodecTest {

  @Test
  void encode(@Randomize(doubleMin = 0, doubleMax = Double.MAX_VALUE) double value)
      throws IOException {
    String string = Double.toString(value);
    CodePointStringCodec codePointCodec = new CodePointStringCodec(string.length(), UTF_8);
    StringDoubleCodec codec = new StringDoubleCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toString(UTF_8)).isEqualTo(string);
  }

  @Test
  void encode_overflow() {
    CodePointStringCodec codePointCodec = new CodePointStringCodec(3, UTF_8);
    StringDoubleCodec codec = new StringDoubleCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    double value = 1234.5;
    assertThatThrownBy(() -> codec.encode(value, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decode() throws IOException {
    String string = "3.14";
    CodePointStringCodec codePointCodec = new CodePointStringCodec(string.length(), UTF_8);
    StringDoubleCodec codec = new StringDoubleCodec(codePointCodec);
    ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes(UTF_8));
    assertThat(codec.decode(input)).isEqualTo(3.14);
  }

  @Test
  void decode_invalid() {
    CodePointStringCodec codePointCodec = new CodePointStringCodec(3, UTF_8);
    StringDoubleCodec codec = new StringDoubleCodec(codePointCodec);
    ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(UTF_8));
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("failed to parse number from string")
        .hasCauseInstanceOf(NumberFormatException.class);
  }

  @Test
  void roundtrip(@Randomize(doubleMin = 0, doubleMax = Double.MAX_VALUE) double value)
      throws IOException {
    String string = Double.toString(value);
    CodePointStringCodec codePointCodec = new CodePointStringCodec(string.length(), UTF_8);
    StringDoubleCodec codec = new StringDoubleCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertThat(codec.decode(input)).isEqualTo(value);
  }
}
