package io.bytestreams.codec.core;

/**
 * A codec for {@link Float}s encoded as {@link String}s.
 *
 * <p>The float is converted to a string using {@link Float#toString()}. The underlying string codec
 * is responsible for handling any length or padding requirements.
 */
public class StringFloatCodec extends StringNumberCodec<Float> {

  /**
   * Creates a new string float codec.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   */
  public StringFloatCodec(Codec<String> stringCodec) {
    super(stringCodec);
  }

  @Override
  protected String fromNumber(Float value) {
    return value.toString();
  }

  @Override
  protected Float toNumber(String value) {
    return Float.valueOf(value);
  }
}
