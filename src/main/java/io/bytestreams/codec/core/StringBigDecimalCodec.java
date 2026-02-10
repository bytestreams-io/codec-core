package io.bytestreams.codec.core;

import java.math.BigDecimal;

/**
 * A codec for {@link BigDecimal}s encoded as {@link String}s.
 *
 * <p>The big decimal is converted to a string using {@link BigDecimal#toPlainString()} to avoid
 * scientific notation. The underlying string codec is responsible for handling any length or padding
 * requirements.
 */
public class StringBigDecimalCodec extends StringNumberCodec<BigDecimal> {

  /**
   * Creates a new string big decimal codec.
   *
   * @param stringCodec the string codec to use for encoding/decoding
   */
  public StringBigDecimalCodec(Codec<String> stringCodec) {
    super(stringCodec);
  }

  @Override
  protected String fromNumber(BigDecimal value) {
    return value.toPlainString();
  }

  @Override
  protected BigDecimal toNumber(String value) {
    return new BigDecimal(value);
  }
}
