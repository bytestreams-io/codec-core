package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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
 * class. The choice codec reads and writes the tag in every case, registered or not, so it never
 * rewinds and places no requirement on the stream. An unrecognised tag or class throws a
 * {@link CodecException} unless a fallback is registered with {@link Builder#otherwise}.
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
  private final Fallback<T, ? extends V> fallback;

  /**
   * The three things an unrecognised alternative needs to round trip: how to recognise it, how to
   * read its tag back when encoding, and how to rebuild it from that tag when decoding. Held as
   * one value so the class, the tag function and the body codec are bound to the same type.
   */
  private record Fallback<T, U>(Class<U> type, Function<U, T> tagOf, Function<T, Codec<U>> body) {}

  ChoiceCodec(
      Codec<T> tagCodec,
      Map<T, Codec<? extends V>> byTag,
      Map<Class<? extends V>, T> tagOf,
      Fallback<T, ? extends V> fallback) {
    this.tagCodec = Objects.requireNonNull(tagCodec, "tagCodec");
    this.byTag = Map.copyOf(byTag);
    this.tagOf = Map.copyOf(tagOf);
    this.fallback = fallback;
  }

  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    T tag = tagOf.get(value.getClass());
    if (tag != null) {
      EncodeResult tagResult = tagCodec.encode(tag, output);
      EncodeResult valueResult = cast(byTag.get(tag)).encode(value, output);
      return new EncodeResult(valueResult.count(), tagResult.bytes() + valueResult.bytes());
    }
    if (fallback != null && fallback.type().isInstance(value)) {
      return encodeUnknown(fallback, value, output);
    }
    throw new CodecException("no codec registered for " + value.getClass().getName(), null);
  }

  @Override
  public V decode(InputStream input) throws IOException {
    T tag = tagCodec.decode(input);
    Codec<? extends V> registered = byTag.get(tag);
    if (registered != null) {
      // Only an unrecognised tag reaches the fallback: a registered alternative whose body is
      // corrupt raises its own error rather than being swallowed as an unknown.
      return cast(registered).decode(input);
    }
    if (fallback == null) {
      throw new CodecException("no codec registered for tag " + Values.render(tag), null);
    }
    return decodeUnknown(fallback, tag, input);
  }

  /**
   * Encodes an unrecognised alternative. The tag comes from the value rather than from the wire,
   * so nothing has to be rewound and the choice codec writes the tag in both paths.
   */
  private <U extends V> EncodeResult encodeUnknown(
      Fallback<T, U> unknown, V value, OutputStream output) throws IOException {
    U typed = unknown.type().cast(value);
    T tag = Objects.requireNonNull(unknown.tagOf().apply(typed), "tagOf returned null");
    // A tag that is registered would write bytes that read back as that other alternative, so an
    // unknown would round trip into something else entirely rather than failing.
    Preconditions.check(
        !byTag.containsKey(tag),
        "fallback returned registered tag %s for %s",
        Values.render(tag),
        typed.getClass().getName());
    EncodeResult tagResult = tagCodec.encode(tag, output);
    EncodeResult bodyResult = bodyFor(unknown, tag).encode(typed, output);
    return new EncodeResult(bodyResult.count(), tagResult.bytes() + bodyResult.bytes());
  }

  /** Decodes the body of an unrecognised alternative, handing the tag to the body codec. */
  private <U extends V> U decodeUnknown(Fallback<T, U> unknown, T tag, InputStream input)
      throws IOException {
    return bodyFor(unknown, tag).decode(input);
  }

  private static <T, U> Codec<U> bodyFor(Fallback<T, U> unknown, T tag) {
    return Objects.requireNonNull(unknown.body().apply(tag), "body returned null");
  }

  @SuppressWarnings("unchecked")
  private Codec<V> cast(Codec<? extends V> codec) {
    return (Codec<V>) codec;
  }

  @Override
  public Object inspect(V value) {
    T tag = tagOf.get(value.getClass());
    if (tag != null) {
      return Inspector.inspect(byTag.get(tag), value);
    }
    if (fallback != null && fallback.type().isInstance(value)) {
      return inspectUnknown(fallback, value);
    }
    return value;
  }

  private <U extends V> Object inspectUnknown(Fallback<T, U> unknown, V value) {
    U typed = unknown.type().cast(value);
    return Inspector.inspect(bodyFor(unknown, unknown.tagOf().apply(typed)), typed);
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
    private Fallback<T, ? extends V> fallback;

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
      Preconditions.check(
          !tagOf.containsKey(type) && (fallback == null || !type.equals(fallback.type())),
          "duplicate type: %s",
          type.getName());
      byTag.put(tag, codec);
      tagOf.put(type, tag);
      return this;
    }

    /**
     * Registers a fallback for alternatives that are not registered, so unknown types round trip
     * instead of failing.
     *
     * <p>Three things are needed, and each is used in both directions: the class recognises an
     * unknown value when encoding, {@code tagOf} supplies the tag to write for it, and
     * {@code body} builds the codec for the bytes after the tag. Because the tag comes from the
     * value rather than from the wire, nothing has to be rewound, so a choice codec never
     * constrains the stream it is given.
     *
     * <pre>{@code
     * .otherwise(
     *     Unknown.class,
     *     Unknown::tag,
     *     tag -> Codecs.binary().xmap(body -> new Unknown(tag, body), Unknown::body))
     * }</pre>
     *
     * <p>Declaring the class keeps encoding checked: a value that is neither registered nor an
     * instance of this type raises a {@link CodecException} naming it, rather than reaching a
     * codec that cannot handle it. A {@code tagOf} that returns a tag already registered is
     * rejected too, since those bytes would read back as that other alternative.
     *
     * <p>The tag is re-encoded from the decoded value rather than replayed, so byte-for-byte
     * passthrough of an unknown alternative holds exactly when the tag codec round trips — as
     * every tag codec in this library does.
     *
     * <p>A body codec that reads to end-of-stream needs a bounded scope — inside
     * {@link Codecs#prefixed} or {@link Codecs#terminated}, which hand down a bounded stream —
     * since an unknown alternative has unknown extent.
     *
     * @param type the class of values this fallback produces and encodes
     * @param tagOf returns the tag to write for a value of that class
     * @param body returns the codec for the body following a given tag
     * @param <U> the fallback subtype
     * @return this builder
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if a fallback is already registered, or if the type is
     *     already registered as an alternative
     */
    public <U extends V> Builder<T, V> otherwise(
        Class<U> type, Function<U, T> tagOf, Function<T, Codec<U>> body) {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(tagOf, "tagOf");
      Objects.requireNonNull(body, "body");
      Preconditions.check(this.fallback == null, "fallback is already registered");
      Preconditions.check(!this.tagOf.containsKey(type), "duplicate type: %s", type.getName());
      this.fallback = new Fallback<>(type, tagOf, body);
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
