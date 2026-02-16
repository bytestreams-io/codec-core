package io.bytestreams.codec.core.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class CodePointReaderTest {

  @Test
  void create_returns_buffered_for_mark_supported_stream() throws IOException {
    try (InputStream input = new ByteArrayInputStream("test".getBytes(UTF_8))) {
      CodePointReader reader = CodePointReader.create(input, UTF_8);
      assertThat(reader).isInstanceOf(BufferedCodePointReader.class);
    }
  }

  @Test
  void create_returns_unbuffered_for_non_mark_supported_stream() throws IOException {
    try (InputStream input = new MarkNotSupportedInputStream("test".getBytes(UTF_8))) {
      CodePointReader reader = CodePointReader.create(input, UTF_8);
      assertThat(reader).isInstanceOf(UnbufferedCodePointReader.class);
    }
  }
}
