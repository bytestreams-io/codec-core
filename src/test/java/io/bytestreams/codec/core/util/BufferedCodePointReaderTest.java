package io.bytestreams.codec.core.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharsetDecoder;
import org.junit.jupiter.api.Test;

class BufferedCodePointReaderTest extends AbstractCodePointReaderTest {

  @Override
  InputStream createInputStream(byte[] data) {
    return new ByteArrayInputStream(data);
  }

  @Override
  CodePointReader createReader(InputStream input, CharsetDecoder decoder) {
    return new BufferedCodePointReader(input, decoder);
  }

  @Test
  void read_eof_mid_sequence() {
    byte[] incomplete = new byte[] {(byte) 0xE4, (byte) 0xB8};
    InputStream input = createInputStream(incomplete);
    CodePointReader reader = createReader(input, getDecoder(UTF_8));

    assertThatThrownBy(() -> reader.read(1))
        .isInstanceOf(EOFException.class)
        .hasMessage("Read 0 code point(s), expected 1");
  }

  @Test
  void read_skip_fails() throws IOException {
    try (InputStream input = new LimitedSkipInputStream("hello".getBytes(UTF_8))) {
      CodePointReader reader = createReader(input, getDecoder(UTF_8));

      assertThatThrownBy(() -> reader.read(1))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("Failed to skip");
    }
  }

  private static class LimitedSkipInputStream extends FilterInputStream {
    LimitedSkipInputStream(byte[] data) {
      super(new ByteArrayInputStream(data));
    }

    @Override
    public long skip(long n) {
      return 0;
    }
  }
}
