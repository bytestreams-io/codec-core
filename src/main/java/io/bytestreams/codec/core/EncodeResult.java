package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;

/**
 * Result of an {@link Codec#encode encode} operation, containing both the logical count and the
 * byte count written.
 *
 * <p>The {@code count} field represents the number of logical units processed, whose meaning is
 * codec-specific. For fixed-length codecs, this is the codec's configured length. For
 * variable-length (stream) codecs, this is the actual number of units in the encoded value.
 * Examples: code points for string codecs, hex digits for hex codecs, items for list codecs, bytes
 * for binary, boolean, and number codecs.
 *
 * @param count the logical count in codec-specific units (e.g. code points, digits, items)
 * @param bytes the number of bytes written to the output stream
 */
public record EncodeResult(int count, int bytes) {

  /** Creates a new EncodeResult. */
  public EncodeResult {
    Preconditions.check(count >= 0, "count must be non-negative, but was [%d]", count);
    Preconditions.check(bytes >= 0, "bytes must be non-negative, but was [%d]", bytes);
  }

  /** An empty result representing zero count and zero bytes written. */
  public static final EncodeResult EMPTY = new EncodeResult(0, 0);

  /**
   * Creates a result where the logical count equals the byte count.
   *
   * @param bytes the number of bytes written (used as both count and bytes)
   * @return an EncodeResult where count == bytes
   */
  public static EncodeResult ofBytes(int bytes) {
    return new EncodeResult(bytes, bytes);
  }
}
