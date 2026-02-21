package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Pair;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A codec that encodes and decodes a pair of values sequentially.
 *
 * <p>The wire format is {@code [first][second]}. Use {@link #as(BiFunction, Function, Function)} to
 * map the pair to a domain type without exposing the internal representation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Codec<Rectangle> codec = Codecs.pair(Codecs.uint8(), Codecs.uint8())
 *     .as(Rectangle::new, r -> r.width, r -> r.height);
 * }</pre>
 *
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 */
public class PairCodec<A, B> implements Codec<Pair<A, B>> {
  private final Codec<A> first;
  private final Codec<B> second;

  PairCodec(Codec<A> first, Codec<B> second) {
    this.first = Objects.requireNonNull(first, "first");
    this.second = Objects.requireNonNull(second, "second");
  }

  @Override
  public EncodeResult encode(Pair<A, B> value, OutputStream output) throws IOException {
    EncodeResult r1 = first.encode(value.first(), output);
    EncodeResult r2 = second.encode(value.second(), output);
    return new EncodeResult(1, r1.bytes() + r2.bytes());
  }

  @Override
  public Pair<A, B> decode(InputStream input) throws IOException {
    return new Pair<>(first.decode(input), second.decode(input));
  }

  /**
   * Maps this pair codec to a domain type.
   *
   * @param constructor constructs the domain type from the two values
   * @param getFirst extracts the first value from the domain type
   * @param getSecond extracts the second value from the domain type
   * @param <V> the domain type
   * @return a codec for the domain type
   * @throws NullPointerException if any argument is null
   */
  public <V> Codec<V> as(
      BiFunction<A, B, V> constructor, Function<V, A> getFirst, Function<V, B> getSecond) {
    return xmap(
        pair -> constructor.apply(pair.first(), pair.second()),
        v -> new Pair<>(getFirst.apply(v), getSecond.apply(v)));
  }
}
