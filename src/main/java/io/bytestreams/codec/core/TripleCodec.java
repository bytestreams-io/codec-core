package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.TriFunction;
import io.bytestreams.codec.core.util.Triple;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * A codec that encodes and decodes a triple of values sequentially.
 *
 * <p>The wire format is {@code [first][second][third]}. Use {@link #as(TriFunction, Function,
 * Function, Function)} to map the triple to a domain type without exposing the internal
 * representation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Codec<Color> codec = Codecs.triple(Codecs.uint8(), Codecs.uint8(), Codecs.uint8())
 *     .as(Color::new, Color::r, Color::g, Color::b);
 * }</pre>
 *
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 * @param <C> the type of the third value
 */
public class TripleCodec<A, B, C> implements Codec<Triple<A, B, C>> {
  private final Codec<A> first;
  private final Codec<B> second;
  private final Codec<C> third;

  TripleCodec(Codec<A> first, Codec<B> second, Codec<C> third) {
    this.first = Objects.requireNonNull(first, "first");
    this.second = Objects.requireNonNull(second, "second");
    this.third = Objects.requireNonNull(third, "third");
  }

  @Override
  public EncodeResult encode(Triple<A, B, C> value, OutputStream output) throws IOException {
    EncodeResult r1 = first.encode(value.first(), output);
    EncodeResult r2 = second.encode(value.second(), output);
    EncodeResult r3 = third.encode(value.third(), output);
    return new EncodeResult(1, r1.bytes() + r2.bytes() + r3.bytes());
  }

  @Override
  public Triple<A, B, C> decode(InputStream input) throws IOException {
    return new Triple<>(first.decode(input), second.decode(input), third.decode(input));
  }

  /**
   * Maps this triple codec to a domain type.
   *
   * @param constructor constructs the domain type from the three values
   * @param getFirst extracts the first value from the domain type
   * @param getSecond extracts the second value from the domain type
   * @param getThird extracts the third value from the domain type
   * @param <V> the domain type
   * @return a codec for the domain type
   * @throws NullPointerException if any argument is null
   */
  public <V> Codec<V> as(
      TriFunction<A, B, C, V> constructor,
      Function<V, A> getFirst,
      Function<V, B> getSecond,
      Function<V, C> getThird) {
    return xmap(
        triple -> constructor.apply(triple.first(), triple.second(), triple.third()),
        v -> new Triple<>(getFirst.apply(v), getSecond.apply(v), getThird.apply(v)));
  }
}
