package io.bytestreams.codec.core;

/**
 * A codec for {@link Double}s encoded as {@link String}s.
 *
 * <p>The double is converted to a string using {@link Double#toString()}. The underlying string
 * codec is responsible for handling any length or padding requirements.
 */
public class StringDoubleCodec extends StringNumberCodec<Double> {

  /**
   * Creates a new string double codec.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   */
  public StringDoubleCodec(Codec<String> stringCodec) {
    super(stringCodec);
  }

  @Override
  protected String fromNumber(Double value) {
    return value.toString();
  }

  @Override
  protected Double toNumber(String value) {
    return Double.valueOf(value);
  }
}
