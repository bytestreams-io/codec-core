package io.bytestreams.codec.core;

import java.util.HexFormat;

/** Renders values for error messages, where a {@code byte[]} would otherwise print its identity. */
final class Values {
  private static final HexFormat HEX = HexFormat.of().withUpperCase();

  private Values() {}

  static String render(Object value) {
    return value instanceof byte[] bytes ? HEX.formatHex(bytes) : String.valueOf(value);
  }
}
