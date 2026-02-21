package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * A codec that applies bidirectional mapping functions to transform between value types.
 *
 * <p>Wraps a base {@link Codec Codec&lt;V&gt;} and applies functions to convert between {@code V}
 * and {@code U}. On encode, the encoder function converts {@code U} to {@code V} before delegating
 * to the base codec. On decode, the decoder function converts the base codec's result from {@code
 * V} to {@code U}.
 *
 * <p>Created via {@link Codec#xmap(Function, Function)}.
 *
 * @param <V> the base codec's value type
 * @param <U> the mapped value type
 */
class MappedCodec<V, U> implements Codec<U> {
  private final Codec<V> base;
  private final Function<V, U> decoder;
  private final Function<U, V> encoder;

  MappedCodec(Codec<V> base, Function<V, U> decoder, Function<U, V> encoder) {
    this.base = Objects.requireNonNull(base, "base");
    this.decoder = Objects.requireNonNull(decoder, "decoder");
    this.encoder = Objects.requireNonNull(encoder, "encoder");
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(U value, OutputStream output) throws IOException {
    return base.encode(encoder.apply(value), output);
  }

  /** {@inheritDoc} */
  @Override
  public U decode(InputStream input) throws IOException {
    return decoder.apply(base.decode(input));
  }
}
