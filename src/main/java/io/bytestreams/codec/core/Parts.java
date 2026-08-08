package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Runs one part of a composite value, tagging any {@link CodecException} with the part's name or
 * index so the failure reports where it came from.
 *
 * <p>Only {@code CodecException} is tagged. {@link IOException} keeps its type, and an
 * {@link IllegalArgumentException} raised before writing stays a constraint violation.
 */
final class Parts {

  private Parts() {}

  static <V> EncodeResult encode(String name, Codec<V> codec, V value, OutputStream output)
      throws IOException {
    try {
      return codec.encode(value, output);
    } catch (CodecException e) {
      throw e.withField(name);
    }
  }

  static <V> V decode(String name, Codec<V> codec, InputStream input) throws IOException {
    try {
      return codec.decode(input);
    } catch (CodecException e) {
      throw e.withField(name);
    }
  }

  static <V> EncodeResult encodeAt(int index, Codec<V> codec, V value, OutputStream output)
      throws IOException {
    try {
      return codec.encode(value, output);
    } catch (CodecException e) {
      throw e.withIndex(index);
    }
  }

  static <V> V decodeAt(int index, Codec<V> codec, InputStream input) throws IOException {
    try {
      return codec.decode(input);
    } catch (CodecException e) {
      throw e.withIndex(index);
    }
  }
}
