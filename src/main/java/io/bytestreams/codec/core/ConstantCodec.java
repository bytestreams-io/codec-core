package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * A codec for constant byte sequences such as magic numbers, version bytes, and protocol
 * signatures.
 *
 * <p>On encode, always writes the expected bytes (ignoring the input value). On decode, reads and
 * validates that the bytes match the expected value, throwing {@link CodecException} on mismatch.
 */
public class ConstantCodec implements Codec<byte[]> {
  private static final HexFormat HEX = HexFormat.of().withUpperCase();
  private final byte[] expected;

  /**
   * Creates a new constant codec with the specified expected bytes.
   *
   * @param expected the expected byte sequence (must be non-null and non-empty)
   * @throws NullPointerException if expected is null
   * @throws IllegalArgumentException if expected is empty
   */
  ConstantCodec(byte[] expected) {
    Objects.requireNonNull(expected, "expected must not be null");
    Preconditions.check(expected.length > 0, "expected must not be empty");
    this.expected = expected.clone();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Ignores the input value and always writes the expected constant bytes.
   */
  @Override
  public EncodeResult encode(byte[] value, OutputStream output) throws IOException {
    output.write(expected);
    return EncodeResult.ofBytes(expected.length);
  }

  /**
   * {@inheritDoc}
   *
   * @throws CodecException if the decoded bytes do not match the expected constant
   * @throws java.io.EOFException if the stream ends before the required bytes are read
   */
  @Override
  public byte[] decode(InputStream input) throws IOException {
    byte[] actual = InputStreams.readFully(input, expected.length);
    if (!Arrays.equals(actual, expected)) {
      throw new CodecException(
          "expected constant [%s] but got [%s]"
              .formatted(HEX.formatHex(expected), HEX.formatHex(actual)),
          null);
    }
    return expected.clone();
  }
}
