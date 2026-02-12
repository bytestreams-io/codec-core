package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract codec for {@link Number}s that are encoded as {@link String}s.
 *
 * <p>This codec wraps a string codec and converts between numbers and their string representation.
 *
 * @param <V> the {@link Number} type this codec handles
 */
public abstract class StringNumberCodec<V extends Number> implements Codec<V> {
  private final Codec<String> stringCodec;

  /**
   * Creates a new string number codec with the specified string codec.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   */
  protected StringNumberCodec(Codec<String> stringCodec) {
    this.stringCodec = stringCodec;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the string representation is rejected by the underlying
   *     codec
   */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    return stringCodec.encode(fromNumber(value), output);
  }

  @Override
  public V decode(InputStream input) throws IOException {
    String string = stringCodec.decode(input);
    try {
      return toNumber(string);
    } catch (NumberFormatException e) {
      throw new CodecException("failed to parse number from string [%s]".formatted(string), e);
    }
  }

  /**
   * Converts a number to its string representation.
   *
   * @param value the number to convert
   * @return the string representation of the number
   */
  protected abstract String fromNumber(V value);

  /**
   * Converts a string to a number.
   *
   * @param value the string to convert
   * @return the number representation of the string
   */
  protected abstract V toNumber(String value);
}
