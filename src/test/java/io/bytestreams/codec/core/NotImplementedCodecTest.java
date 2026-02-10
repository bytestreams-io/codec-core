package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class NotImplementedCodecTest {

  private final NotImplementedCodec<Object> codec = new NotImplementedCodec<>();

  @Test
  void encode_throws() {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode("value", output))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("codec not implemented");
  }

  @Test
  void decode_throws() {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("codec not implemented");
  }
}
