package io.bytestreams.codec.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A codec for variable-length binary data that reads all remaining bytes from the stream.
 *
 * <p>On encode, writes all bytes from the value. On decode, reads bytes until EOF.
 *
 * <p>Created via {@link Codecs#binary()}.
 */
class StreamBinaryCodec implements Codec<byte[]> {

  @Override
  public EncodeResult encode(byte[] value, OutputStream output) throws IOException {
    output.write(value);
    return EncodeResult.ofBytes(value.length);
  }

  @Override
  public byte[] decode(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int b;
    while ((b = input.read()) != -1) {
      buffer.write(b);
    }
    return buffer.toByteArray();
  }
}
