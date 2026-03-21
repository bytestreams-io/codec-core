package io.bytestreams.codec.core;

/**
 * A codec or codec-like object that can produce a structured representation of a decoded value.
 *
 * <p>The return type depends on the codec:
 *
 * <ul>
 *   <li>Object codecs (sequential, tagged, pair, triple) return {@code Map<String, Object>}
 *   <li>List codecs return {@code List<Object>}
 *   <li>Wrapper codecs (prefixed, xmap, lazy) delegate to the inner codec
 *   <li>Choice codecs delegate to the matched branch
 *   <li>Constant codecs return the constant value
 * </ul>
 *
 * @param <T> the type of the value to inspect
 */
public interface Inspector<T> {

  /**
   * Returns a structured representation of the given value suitable for logging, serialization, or
   * diagnostics.
   *
   * @param value the decoded value to inspect
   * @return a structured representation (Map, List, or scalar)
   */
  Object inspect(T value);

  /**
   * Inspects a value using an inspector whose type parameter is unknown at compile time.
   *
   * <p>This helper centralizes the unchecked cast required when an {@code Inspector<?>} is obtained
   * via {@code instanceof} pattern matching. The caller is responsible for ensuring the value's
   * runtime type matches the inspector's type parameter.
   *
   * @param inspector the inspector (typically obtained from a codec's {@code instanceof} check)
   * @param value the value to inspect
   * @return the structured representation
   */
  @SuppressWarnings("unchecked")
  static Object inspect(Inspector<?> inspector, Object value) {
    return ((Inspector<Object>) inspector).inspect(value);
  }
}
