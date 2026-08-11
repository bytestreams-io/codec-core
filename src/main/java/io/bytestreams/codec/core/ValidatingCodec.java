package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;

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
 * <p>Created via {@link Codec#validate(Validator)}.
 *
 * @param <V> the value type
 */
class ValidatingCodec<V> implements Codec<V>, Inspectable<V> {
  private final Codec<V> base;
  private final Validator<V> validator;

  ValidatingCodec(Codec<V> base, Validator<V> validator) {
    this.base = Objects.requireNonNull(base, "base");
    this.validator = Objects.requireNonNull(validator, "validator");
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    Optional<String> failure = validator.check(value);
    if (failure.isPresent()) {
      throw new IllegalArgumentException(failure.get());
    }
    return base.encode(value, output);
  }

  /** {@inheritDoc} */
  @Override
  public V decode(InputStream input) throws IOException {
    V value = base.decode(input);
    Optional<String> failure = validator.check(value);
    if (failure.isPresent()) {
      throw new CodecException(failure.get(), null);
    }
    return value;
  }

  @Override
  public Object inspect(V value) {
    return Inspector.inspect(base, value);
  }
}
