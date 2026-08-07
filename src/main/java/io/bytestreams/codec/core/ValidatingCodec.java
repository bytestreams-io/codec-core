package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A codec that checks values against a condition as they pass through.
 *
 * <p>Wraps a base {@link Codec Codec&lt;V&gt;} and applies the check in both directions: before
 * writing on encode, and after reading on decode. A failed check throws {@link
 * IllegalArgumentException} on encode and {@link CodecException} on decode.
 *
 * <p>Inspection delegates to the base codec without running the check, so inspecting a value that
 * would fail validation is safe.
 *
 * <p>Created via {@link Codec#validate(Predicate, Function)}.
 *
 * @param <V> the value type
 */
class ValidatingCodec<V> implements Codec<V>, Inspectable<V> {
  private final Codec<V> base;
  private final Predicate<V> check;
  private final Function<V, String> message;

  ValidatingCodec(Codec<V> base, Predicate<V> check, Function<V, String> message) {
    this.base = Objects.requireNonNull(base, "base");
    this.check = Objects.requireNonNull(check, "check");
    this.message = Objects.requireNonNull(message, "message");
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    if (!check.test(value)) {
      throw new IllegalArgumentException(message.apply(value));
    }
    return base.encode(value, output);
  }

  /** {@inheritDoc} */
  @Override
  public V decode(InputStream input) throws IOException {
    V value = base.decode(input);
    if (!check.test(value)) {
      throw new CodecException(message.apply(value), null);
    }
    return value;
  }

  @Override
  public Object inspect(V value) {
    return Inspector.inspect(base, value);
  }
}
