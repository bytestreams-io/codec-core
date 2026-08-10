package io.bytestreams.codec.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * A codec for a value followed by a value computed over its bytes — an LRC, a CRC, a checksum or a
 * MAC.
 *
 * <p>The wire format is {@code [value][check]}. On encode the value is written to a buffer so the
 * check can be computed over exactly those bytes, then both are written. On decode the value is
 * read through a recording stream, the check is computed over what was recorded, and the check
 * field that follows must match.
 *
 * <p>The check function is supplied by the caller. Which polynomial, seed and bit order a protocol
 * uses is specification knowledge, so no algorithm is built in.
 *
 * <p>The value codec must consume exactly its own bytes, since the check field begins immediately
 * afterwards. A read-until-EOF codec such as {@link Codecs#ascii()} would swallow the check unless
 * placed inside a bounded scope.
 *
 * <p>Created via {@link Codecs#checked(Codec, Codec, Function)}.
 *
 * @param <T> the check value type
 * @param <V> the value type
 */
class CheckedCodec<T, V> implements Codec<V>, Inspectable<V> {
  private final Codec<T> checkCodec;
  private final Function<byte[], T> compute;
  private final Codec<V> valueCodec;

  CheckedCodec(Codec<V> valueCodec, Codec<T> checkCodec, Function<byte[], T> compute) {
    this.checkCodec = Objects.requireNonNull(checkCodec, "checkCodec");
    this.compute = Objects.requireNonNull(compute, "compute");
    this.valueCodec = Objects.requireNonNull(valueCodec, "valueCodec");
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    EncodeResult valueResult = valueCodec.encode(value, buffer);
    byte[] covered = buffer.toByteArray();
    // Compute and encode the check before writing anything, so a failure in either leaves the
    // output untouched rather than half a frame.
    ByteArrayOutputStream checkBuffer = new ByteArrayOutputStream();
    EncodeResult checkResult = checkCodec.encode(compute.apply(covered), checkBuffer);
    output.write(covered);
    checkBuffer.writeTo(output);
    return new EncodeResult(valueResult.count(), valueResult.bytes() + checkResult.bytes());
  }

  /** {@inheritDoc} */
  @Override
  public V decode(InputStream input) throws IOException {
    RecordingInputStream recording = new RecordingInputStream(input);
    V value = valueCodec.decode(recording);
    T expected = compute.apply(recording.recordedBytes());
    T actual = checkCodec.decode(input);
    if (!Objects.deepEquals(expected, actual)) {
      throw new CodecException(
          "check value mismatch: computed [%s] but read [%s]"
              .formatted(Values.render(expected), Values.render(actual)),
          null);
    }
    return value;
  }

  @Override
  public Object inspect(V value) {
    return Inspector.inspect(valueCodec, value);
  }
}
