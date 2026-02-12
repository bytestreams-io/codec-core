package io.bytestreams.codec.core;

/**
 * Result of an {@link Codec#encode encode} operation, containing both the logical length and the
 * byte count written.
 *
 * @param length the logical length in codec-specific units (e.g. code points, digits, items)
 * @param bytes the number of bytes written to the output stream
 */
public record EncodeResult(int length, int bytes) {

  /** An empty result representing zero length and zero bytes written. */
  public static final EncodeResult EMPTY = new EncodeResult(0, 0);

  /**
   * Creates a result where the logical length equals the byte count.
   *
   * @param bytes the number of bytes written (used as both length and bytes)
   * @return an EncodeResult where length == bytes
   */
  public static EncodeResult ofBytes(int bytes) {
    return new EncodeResult(bytes, bytes);
  }
}
