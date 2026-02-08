package io.bytestreams.codec.core.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.nio.charset.CharsetDecoder;
import org.junit.jupiter.api.Test;

class UnbufferedCodePointReaderTest extends AbstractCodePointReaderTest {

  @Override
  InputStream createInputStream(byte[] data) {
    return new NonMarkSupportingInputStream(data);
  }

  @Override
  CodePointReader createReader(InputStream input, CharsetDecoder decoder) {
    return new UnbufferedCodePointReader(input, decoder);
  }

  @Test
  void read_eof_mid_sequence() {
    byte[] incomplete = new byte[] {(byte) 0xE4, (byte) 0xB8};
    InputStream input = createInputStream(incomplete);
    CodePointReader reader = createReader(input, getDecoder(UTF_8));

    assertThatThrownBy(() -> reader.read(1))
        .isInstanceOf(EOFException.class)
        .hasMessageContaining("2 byte(s)");
  }

  @Test
  void read_eof_mid_sequence_partial() {
    byte[] data = new byte[] {'a', (byte) 0xE4, (byte) 0xB8};
    InputStream input = createInputStream(data);
    CodePointReader reader = createReader(input, getDecoder(UTF_8));

    assertThatThrownBy(() -> reader.read(2))
        .isInstanceOf(EOFException.class)
        .hasMessageContaining("2 byte(s)");
  }

  private static class NonMarkSupportingInputStream extends FilterInputStream {
    NonMarkSupportingInputStream(byte[] data) {
      super(new ByteArrayInputStream(data));
    }

    @Override
    public boolean markSupported() {
      return false;
    }
  }
}
