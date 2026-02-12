package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class StringBigDecimalCodecTest {

  @Test
  void encode(@Randomize BigDecimal value) throws IOException {
    String string = value.toPlainString();
    FixedCodePointStringCodec codePointCodec =
        new FixedCodePointStringCodec(string.length(), UTF_8);
    StringBigDecimalCodec codec = new StringBigDecimalCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    assertThat(output.toString(UTF_8)).isEqualTo(string);
  }

  @Test
  void encode_overflow() {
    FixedCodePointStringCodec codePointCodec = new FixedCodePointStringCodec(3, UTF_8);
    StringBigDecimalCodec codec = new StringBigDecimalCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    BigDecimal value = new BigDecimal("1234.56");
    assertThatThrownBy(() -> codec.encode(value, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decode() throws IOException {
    String string = "3.14";
    FixedCodePointStringCodec codePointCodec =
        new FixedCodePointStringCodec(string.length(), UTF_8);
    StringBigDecimalCodec codec = new StringBigDecimalCodec(codePointCodec);
    ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes(UTF_8));
    assertThat(codec.decode(input)).isEqualByComparingTo(new BigDecimal("3.14"));
  }

  @Test
  void decode_invalid() {
    FixedCodePointStringCodec codePointCodec = new FixedCodePointStringCodec(3, UTF_8);
    StringBigDecimalCodec codec = new StringBigDecimalCodec(codePointCodec);
    ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(UTF_8));
    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("failed to parse number from string")
        .hasCauseInstanceOf(NumberFormatException.class);
  }

  @Test
  void roundtrip(@Randomize BigDecimal value) throws IOException {
    String string = value.toPlainString();
    FixedCodePointStringCodec codePointCodec =
        new FixedCodePointStringCodec(string.length(), UTF_8);
    StringBigDecimalCodec codec = new StringBigDecimalCodec(codePointCodec);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(value, output);
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertThat(codec.decode(input)).isEqualByComparingTo(value);
  }
}
