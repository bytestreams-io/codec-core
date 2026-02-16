package io.bytestreams.codec.core.util;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
class UnbufferedCodePointReaderTest extends AbstractCodePointReaderTest {

  @Override
  InputStream createInputStream(byte[] data) {
    return new MarkNotSupportedInputStream(data);
  }

  @Override
  CodePointReader createReader(InputStream input, Charset charset) {
    return new UnbufferedCodePointReader(input, charset);
  }

  @Test
  void read_eof_mid_sequence() {
    byte[] incomplete = new byte[] {(byte) 0xE4, (byte) 0xB8};
    InputStream input = createInputStream(incomplete);
    CodePointReader reader = createReader(input, UTF_8);

    assertThatThrownBy(() -> reader.read(1))
        .isInstanceOf(EOFException.class)
        .hasMessageContaining("2 byte(s)");
  }

  @Test
  void read_eof_mid_sequence_partial() {
    byte[] data = new byte[] {'a', (byte) 0xE4, (byte) 0xB8};
    InputStream input = createInputStream(data);
    CodePointReader reader = createReader(input, UTF_8);

    assertThatThrownBy(() -> reader.read(2))
        .isInstanceOf(EOFException.class)
        .hasMessageContaining("2 byte(s)");
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16BE", "UTF-16LE"})
  void read_invalid_surrogate_pair_without_replacement(
      String charsetName, @Randomize(unicodeBlocks = "EMOTICONS", length = 1) String value) {
    Charset charset = Charset.forName(charsetName);
    byte[] bytes = value.getBytes(charset);
    bytes[bytes.length - 2] = bytes[bytes.length - 4];
    bytes[bytes.length - 1] = bytes[bytes.length - 3];

    InputStream input = createInputStream(bytes);
    CodePointReader reader = createReader(input, charset);

    assertThatThrownBy(() -> reader.read(1)).isInstanceOf(MalformedInputException.class);
  }
}
