package io.bytestreams.codec.core.util;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;

class MarkNotSupportedInputStream extends FilterInputStream {
  MarkNotSupportedInputStream(byte[] data) {
    super(new ByteArrayInputStream(data));
  }

  @Override
  public boolean markSupported() {
    return false;
  }
}
