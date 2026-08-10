package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A codec for discriminated unions where a tag determines which codec to use for the value.
 *
 * <p>The wire format is {@code [tag][value]}. Each alternative is registered once, as a tag, a
 * class and a codec, so the two directions cannot disagree:
 *
 * <pre>{@code
 * Codec<Shape> codec = Codecs.<Integer, Shape>choice(Codecs.uint8())
 *     .on(1, Circle.class, circleCodec)
 *     .on(2, Rectangle.class, rectangleCodec)
 *     .build();
 * }</pre>
 *
 * <p>Decoding dispatches on the decoded tag; encoding dispatches on the value's exact runtime
 * class. An unrecognised tag or class throws a {@link CodecException} unless a {@link Fallback} is
 * registered with {@link Builder#otherwise(Fallback)}.
 *
 * <p>Tags must not be arrays: array equality is by identity, so a decoded tag would never match a
 * registered one. Use a {@code String} tag — {@link Codecs#ascii(int)} — for character codes.
 *
 * @param <T> the tag type
 * @param <V> the base type of the discriminated union
 */
public class ChoiceCodec<T, V> implements Codec<V>, Inspectable<V> {
  private final Codec<T> tagCodec;
  private final Map<T, Codec<? extends V>> byTag;
  private final Map<Class<? extends V>, T> tagOf;
  private final Fallback<T, V> fallback;

  /**
   * Handles alternatives that are not registered, so unknown types survive a decode/encode round
   * trip instead of failing.
   *
   * <p>The tag itself is read and written by the choice codec, exactly as for a registered
   * alternative; an implementation handles only the body.
   *
   * <p>Decoding a body of unknown length requires a bounded scope — a fallback that reads to
   * end-of-stream is correct inside {@link Codecs#prefixed} or {@link Codecs#terminated}, which
   * hand down a bounded stream, and will otherwise swallow the rest of the input.
   *
   * @param <T> the tag type
   * @param <V> the base type of the discriminated union
   */
  public interface Fallback<T, V> {

    /**
     * Decodes the body of an unrecognised alternative.
     *
     * @param tag the tag that was decoded
     * @param input the input stream, positioned after the tag
     * @return the decoded value
     * @throws IOException if reading fails
     */
    V decode(T tag, InputStream input) throws IOException;

    /**
     * Returns the tag to write for a value this fallback produced.
     *
     * @param value the value being encoded
     * @return the tag to write before the body
     */
    T tagOf(V value);

    /**
     * Encodes the body of an unrecognised alternative. The tag has already been written.
     *
     * @param value the value being encoded
     * @param output the output stream
     * @return the number of body units and bytes written
     * @throws IOException if writing fails
     */
    EncodeResult encodeBody(V value, OutputStream output) throws IOException;
  }

  ChoiceCodec(
      Codec<T> tagCodec,
      Map<T, Codec<? extends V>> byTag,
      Map<Class<? extends V>, T> tagOf,
      Fallback<T, V> fallback) {
    this.tagCodec = Objects.requireNonNull(tagCodec, "tagCodec");
    this.byTag = Map.copyOf(byTag);
    this.tagOf = Map.copyOf(tagOf);
    this.fallback = fallback;
  }

  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    Class<?> type = value.getClass();
    T tag = tagOf.get(type);
    if (tag == null) {
      if (fallback == null) {
        throw new CodecException("no codec registered for " + type.getName(), null);
      }
      EncodeResult tagResult = tagCodec.encode(fallback.tagOf(value), output);
      EncodeResult bodyResult = fallback.encodeBody(value, output);
      return new EncodeResult(bodyResult.count(), tagResult.bytes() + bodyResult.bytes());
    }
    @SuppressWarnings("unchecked")
    Codec<V> codec = (Codec<V>) byTag.get(tag);
    EncodeResult tagResult = tagCodec.encode(tag, output);
    EncodeResult valueResult = codec.encode(value, output);
    return new EncodeResult(valueResult.count(), tagResult.bytes() + valueResult.bytes());
  }

  @Override
  public V decode(InputStream input) throws IOException {
    T tag = tagCodec.decode(input);
    @SuppressWarnings("unchecked")
    Codec<V> codec = (Codec<V>) byTag.get(tag);
    if (codec == null) {
      if (fallback == null) {
        throw new CodecException("no codec registered for tag " + Values.render(tag), null);
      }
      return fallback.decode(tag, input);
    }
    // Only an unrecognised tag reaches the fallback: a registered alternative whose body is
    // corrupt must stay an error rather than silently decoding as an unknown.
    return codec.decode(input);
  }

  @Override
  public Object inspect(V value) {
    T tag = tagOf.get(value.getClass());
    if (tag == null) {
      return value;
    }
    return Inspector.inspect(byTag.get(tag), value);
  }

  /**
   * Creates a new builder for a choice codec.
   *
   * @param tagCodec the codec for the tag
   * @param <T> the tag type
   * @param <V> the base type of the discriminated union
   * @return a new builder
   */
  static <T, V> Builder<T, V> builder(Codec<T> tagCodec) {
    return new Builder<>(tagCodec);
  }

  /**
   * Builder for {@link ChoiceCodec}.
   *
   * @param <T> the tag type
   * @param <V> the base type of the discriminated union
   */
  public static class Builder<T, V> {
    private final Codec<T> tagCodec;
    private final Map<T, Codec<? extends V>> byTag = new LinkedHashMap<>();
    private final Map<Class<? extends V>, T> tagOf = new LinkedHashMap<>();
    private Fallback<T, V> fallback;

    Builder(Codec<T> tagCodec) {
      this.tagCodec = Objects.requireNonNull(tagCodec, "tagCodec");
    }

    /**
     * Registers an alternative.
     *
     * @param tag the tag identifying this alternative on the wire
     * @param type the class of values for this alternative
     * @param codec the codec for values of this class
     * @param <S> the subtype
     * @return this builder
     * @throws NullPointerException if tag, type or codec is null
     * @throws IllegalArgumentException if the tag or the type is already registered, or if the tag
     *     is an array
     */
    public <S extends V> Builder<T, V> on(T tag, Class<S> type, Codec<S> codec) {
      Objects.requireNonNull(tag, "tag");
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(codec, "codec");
      Preconditions.check(
          !tag.getClass().isArray(), "array tags are not supported: %s", tag.getClass().getName());
      Preconditions.check(!byTag.containsKey(tag), "duplicate tag: %s", tag);
      Preconditions.check(!tagOf.containsKey(type), "duplicate type: %s", type.getName());
      byTag.put(tag, codec);
      tagOf.put(type, tag);
      return this;
    }

    /**
     * Registers a fallback for alternatives that are not registered, so unknown types round trip
     * instead of failing.
     *
     * @param fallback the fallback to use
     * @return this builder
     * @throws NullPointerException if fallback is null
     */
    public Builder<T, V> otherwise(Fallback<T, V> fallback) {
      this.fallback = Objects.requireNonNull(fallback, "fallback");
      return this;
    }

    /**
     * Builds the choice codec.
     *
     * @return a new choice codec
     * @throws IllegalArgumentException if no alternatives have been registered
     */
    public ChoiceCodec<T, V> build() {
      Preconditions.check(!byTag.isEmpty(), "at least one option must be registered");
      return new ChoiceCodec<>(tagCodec, byTag, tagOf, fallback);
    }
  }
}
