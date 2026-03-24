package io.bytestreams.codec.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * A decoded value paired with the raw bytes it was decoded from.
 *
 * <p>Produced by {@link RecordingCodec} to preserve the original wire bytes alongside the decoded
 * value. Useful for auditing, replay, and debugging.
 *
 * @param value the decoded value
 * @param rawBytes the raw bytes that were read from the input stream during decoding
 * @param <T> the type of the decoded value
 */
public record Recorded<T>(T value, byte[] rawBytes) {

  public Recorded {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(rawBytes, "rawBytes");
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Recorded<?> other
        && Objects.equals(value, other.value)
        && Arrays.equals(rawBytes, other.rawBytes);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hashCode(value) + Arrays.hashCode(rawBytes);
  }

  @Override
  public String toString() {
    return "Recorded[value=" + value + ", rawBytes=(" + rawBytes.length + " bytes)]";
  }
}
