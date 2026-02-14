package io.bytestreams.codec.core;

/**
 * Factory for creating variable-length codec builders.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Variable-length by byte count
 * VariableByteLengthCodec.Builder llvar = VariableLengthCodecs.ofByteLength(NumberCodecs.ofUnsignedShort());
 * Codec<String> varString = llvar.of(stringCodec);
 *
 * // Variable-length by item count
 * Codec<String> codec = VariableLengthCodecs.ofItemLength(NumberCodecs.ofUnsignedByte())
 *     .of(Strings::codePointCount,
 *         length -> StringCodecs.ofCodePoint(length).build());
 * }</pre>
 */
public class VariableLengthCodecs {
  private VariableLengthCodecs() {}

  /**
   * Creates a new builder for a variable-length codec where the byte count is encoded as a prefix.
   *
   * @param lengthCodec the codec for the byte count prefix
   * @return a new builder
   */
  public static VariableByteLengthCodec.Builder ofByteLength(Codec<Integer> lengthCodec) {
    return VariableByteLengthCodec.builder(lengthCodec);
  }

  /**
   * Creates a new builder for a variable-length codec where the item count is encoded as a prefix.
   *
   * @param lengthCodec the codec for the item count prefix
   * @return a new builder
   */
  public static VariableItemLengthCodec.Builder ofItemLength(Codec<Integer> lengthCodec) {
    return VariableItemLengthCodec.builder(lengthCodec);
  }
}
