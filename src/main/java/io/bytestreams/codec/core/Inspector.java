package io.bytestreams.codec.core;

/**
 * Produces structured representations of decoded values by walking the codec tree.
 *
 * <p>Codecs that implement {@link Inspectable} provide their own inspection logic. For all other
 * codecs (typically primitives), the value is returned as-is.
 */
public final class Inspector {

  private Inspector() {}

  /**
   * Returns a structured representation of the given value by walking the codec tree.
   *
   * @param codec the codec that produced the value
   * @param value the decoded value to inspect
   * @return a structured representation (Map, List, or scalar)
   */
  public static Object inspect(Codec<?> codec, Object value) {
    if (codec instanceof Inspectable<?> inspectable) {
      @SuppressWarnings("unchecked")
      var result = ((Inspectable<Object>) inspectable).inspect(value);
      return result;
    }
    return value;
  }
}
