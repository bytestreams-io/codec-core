package io.bytestreams.codec.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * An input stream that records all bytes read from the underlying input.
 *
 * <p>Use {@link #toByteArray()} after reading to retrieve the captured bytes.
 */
class RecordingInputStream extends InputStream {
  private final InputStream input;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  RecordingInputStream(InputStream input) {
    this.input = Objects.requireNonNull(input, "input");
  }

  byte[] recordedBytes() {
    return buffer.toByteArray();
  }

  @Override
  public int read() throws IOException {
    int b = input.read();
    if (b != -1) {
      buffer.write(b);
    }
    return b;
  }

  @Override
  public int read(byte[] buf, int off, int len) throws IOException {
    int n = input.read(buf, off, len);
    if (n > 0) {
      buffer.write(buf, off, n);
    }
    return n;
  }

  @Override
  public long skip(long n) throws IOException {
    byte[] buf = new byte[(int) Math.min(n, 8192)];
    long remaining = n;
    while (remaining > 0) {
      int read = read(buf, 0, (int) Math.min(remaining, buf.length));
      if (read == -1) {
        break;
      }
      remaining -= read;
    }
    return n - remaining;
  }

  @Override
  public int available() throws IOException {
    return input.available();
  }

  @Override
  public void close() throws IOException {
    input.close();
  }
}
