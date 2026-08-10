package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * An input stream that records all bytes read from the underlying input.
 *
 * <p>Use {@link #recordedBytes()} after reading to retrieve the recorded bytes.
 *
 * <p>{@code mark} and {@code reset} pass through to the underlying stream, and a reset also drops
 * the bytes recorded since the mark. Without that, a codec that rewinds — {@code repeatWhile} peeks
 * and resets before every item — would record the peeked bytes twice and corrupt the recording.
 */
class RecordingInputStream extends InputStream {
  private static final int INITIAL_CAPACITY = 64;

  private final InputStream input;
  private byte[] recorded = new byte[INITIAL_CAPACITY];
  private int size;
  // A stream with no explicit mark resets to where it started, so the recording does too.
  private int markedSize;

  RecordingInputStream(InputStream input) {
    this.input = Objects.requireNonNull(input, "input");
  }

  byte[] recordedBytes() {
    return Arrays.copyOf(recorded, size);
  }

  private void append(int b) {
    ensureCapacity(size + 1);
    recorded[size++] = (byte) b;
  }

  private void append(byte[] source, int offset, int length) {
    ensureCapacity(size + length);
    System.arraycopy(source, offset, recorded, size, length);
    size += length;
  }

  private void ensureCapacity(int required) {
    if (required > recorded.length) {
      recorded = Arrays.copyOf(recorded, Math.max(required, recorded.length * 2));
    }
  }

  @Override
  public int read() throws IOException {
    int b = input.read();
    if (b != -1) {
      append(b);
    }
    return b;
  }

  @Override
  public int read(byte[] buf, int off, int len) throws IOException {
    int n = input.read(buf, off, len);
    if (n > 0) {
      append(buf, off, n);
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
  public boolean markSupported() {
    return input.markSupported();
  }

  @Override
  public synchronized void mark(int readLimit) {
    input.mark(readLimit);
    markedSize = size;
  }

  @Override
  public synchronized void reset() throws IOException {
    input.reset();
    size = markedSize;
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
