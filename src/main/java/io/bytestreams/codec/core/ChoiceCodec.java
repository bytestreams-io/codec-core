package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Converter;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A codec for discriminated unions where a class tag determines which codec to use for the value.
 *
 * <p>The wire format is {@code [tag][value]}, where the tag selects among registered alternatives.
 * This codec only sees {@code Codec<Class<? extends V>>} — callers typically create it by using
 * {@link Codec#xmap(Converter) xmap(Converter)} to map a tag codec to class values:
 *
 * <pre>{@code
 * BiMap<Integer, Class<? extends Shape>> tags = BiMap.of(
 *     Map.entry(1, Circle.class),
 *     Map.entry(2, Rectangle.class));
 *
 * Codec<Shape> codec = Codecs.<Shape>choice(Codecs.uint8().xmap(tags))
 *     .on(Circle.class, circleCodec)
 *     .on(Rectangle.class, rectangleCodec)
 *     .build();
 * }</pre>
 *
 * @param <V> the base type of the discriminated union
 */
public class ChoiceCodec<V> implements Codec<V> {
  private final Codec<Class<? extends V>> classCodec;
  private final Map<Class<? extends V>, Codec<? extends V>> codecs;

  ChoiceCodec(
      Codec<Class<? extends V>> classCodec, Map<Class<? extends V>, Codec<? extends V>> codecs) {
    this.classCodec = Objects.requireNonNull(classCodec, "classCodec");
    this.codecs = Map.copyOf(codecs);
  }

  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    @SuppressWarnings("unchecked")
    Class<? extends V> type = (Class<? extends V>) value.getClass();
    @SuppressWarnings("unchecked")
    Codec<V> codec = (Codec<V>) codecs.get(type);
    if (codec == null) {
      throw new CodecException("no codec registered for " + type.getName(), null);
    }
    EncodeResult classResult = classCodec.encode(type, output);
    EncodeResult valueResult = codec.encode(value, output);
    return new EncodeResult(1, classResult.bytes() + valueResult.bytes());
  }

  @Override
  public V decode(InputStream input) throws IOException {
    Class<? extends V> type = classCodec.decode(input);
    @SuppressWarnings("unchecked")
    Codec<V> codec = (Codec<V>) codecs.get(type);
    if (codec == null) {
      throw new CodecException("no codec registered for " + type.getName(), null);
    }
    return codec.decode(input);
  }

  /**
   * Creates a new builder for a choice codec.
   *
   * @param classCodec the codec for the class tag
   * @param <V> the base type of the discriminated union
   * @return a new builder
   */
  static <V> Builder<V> builder(Codec<Class<? extends V>> classCodec) {
    return new Builder<>(classCodec);
  }

  /**
   * Builder for {@link ChoiceCodec}.
   *
   * @param <V> the base type of the discriminated union
   */
  public static class Builder<V> {
    private final Codec<Class<? extends V>> classCodec;
    private final Map<Class<? extends V>, Codec<? extends V>> codecs = new LinkedHashMap<>();

    Builder(Codec<Class<? extends V>> classCodec) {
      this.classCodec = Objects.requireNonNull(classCodec, "classCodec");
    }

    /**
     * Registers a class/codec pair.
     *
     * @param type the class to register
     * @param codec the codec for values of this class
     * @param <S> the subtype
     * @return this builder
     * @throws NullPointerException if type or codec is null
     * @throws IllegalArgumentException if the type is already registered
     */
    public <S extends V> Builder<V> on(Class<S> type, Codec<S> codec) {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(codec, "codec");
      Preconditions.check(!codecs.containsKey(type), "duplicate type: %s", type.getName());
      codecs.put(type, codec);
      return this;
    }

    /**
     * Builds the choice codec.
     *
     * @return a new choice codec
     * @throws IllegalArgumentException if no options have been registered
     */
    public ChoiceCodec<V> build() {
      Preconditions.check(!codecs.isEmpty(), "at least one option must be registered");
      return new ChoiceCodec<>(classCodec, codecs);
    }
  }
}
