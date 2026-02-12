package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A codec for boolean values encoded as a single byte.
 *
 * <p>Encodes {@code false} as {@code 0x00} and {@code true} as {@code 0x01}. Decoding is strict:
 * any value other than {@code 0x00} or {@code 0x01} throws a {@link CodecException}.
 */
public class BooleanCodec implements FixedLengthCodec<Boolean> {

  @Override
  public int getLength() {
    return 1;
  }

  @Override
  public EncodeResult encode(Boolean value, OutputStream output) throws IOException {
    output.write(Boolean.TRUE.equals(value) ? 0x01 : 0x00);
    return EncodeResult.ofBytes(1);
  }

  @Override
  public Boolean decode(InputStream input) throws IOException {
    byte[] bytes = InputStreams.readFully(input, 1);
    return switch (bytes[0]) {
      case 0x00 -> false;
      case 0x01 -> true;
      default ->
          throw new CodecException(
              "invalid boolean value: 0x%02X, expected 0x00 or 0x01".formatted(bytes[0] & 0xFF),
              null);
    };
  }
}
