package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
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
 * class, and the choice codec writes the tag. An unrecognised tag or class throws a
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
  // Set together by otherwise(), so these are null together and the codec handles the type.
  private final Class<? extends V> unknownType;
  private final Codec<? extends V> unknownCodec;

  ChoiceCodec(
      Codec<T> tagCodec,
      Map<T, Codec<? extends V>> byTag,
      Map<Class<? extends V>, T> tagOf,
      Class<? extends V> unknownType,
      Codec<? extends V> unknownCodec) {
    this.tagCodec = Objects.requireNonNull(tagCodec, "tagCodec");
    this.byTag = Map.copyOf(byTag);
    this.tagOf = Map.copyOf(tagOf);
    this.unknownType = unknownType;
    this.unknownCodec = unknownCodec;
  }

  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    T tag = tagOf.get(value.getClass());
    if (tag != null) {
      EncodeResult tagResult = tagCodec.encode(tag, output);
      EncodeResult valueResult = cast(byTag.get(tag)).encode(value, output);
      return new EncodeResult(valueResult.count(), tagResult.bytes() + valueResult.bytes());
    }
    if (unknownType != null && unknownType.isInstance(value)) {
      // The fallback owns the whole span, tag included: an unknown alternative has no tag this
      // codec could write, since nothing here knows how to derive one from the value.
      return cast(unknownCodec).encode(value, output);
    }
    throw new CodecException("no codec registered for " + value.getClass().getName(), null);
  }

  @Override
  public V decode(InputStream input) throws IOException {
    if (unknownCodec == null) {
      T tag = tagCodec.decode(input);
      return body(tag).decode(input);
    }
    // Record the tag rather than peeking it: the fallback needs those bytes back, and marking
    // would impose markSupported() on every choice codec to pay for an opt-in feature.
    RecordingInputStream tap = new RecordingInputStream(input);
    T tag = tagCodec.decode(tap);
    Codec<? extends V> registered = byTag.get(tag);
    if (registered != null) {
      // Only an unrecognised tag reaches the fallback: a registered alternative whose body is
      // corrupt raises its own error rather than being swallowed as an unknown.
      return cast(registered).decode(input);
    }
    return cast(unknownCodec).decode(replaying(tap.recordedBytes(), input));
  }

  /**
   * Returns the recorded tag bytes followed by the live input, so the fallback sees the whole
   * span. The stream reads through on demand and never buffers ahead, leaving anything the
   * fallback does not consume readable by whatever decodes next.
   */
  private static InputStream replaying(byte[] tagBytes, InputStream input) {
    return new SequenceInputStream(
        new ByteArrayInputStream(tagBytes),
        new FilterInputStream(input) {
          @Override
          public void close() {
            // SequenceInputStream closes each stream as it is exhausted, but this one belongs to
            // the caller: no codec in this library closes a stream it was handed.
          }
        });
  }

  private Codec<V> body(T tag) {
    Codec<? extends V> codec = byTag.get(tag);
    if (codec == null) {
      throw new CodecException("no codec registered for tag " + Values.render(tag), null);
    }
    return cast(codec);
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
    if (unknownType != null && unknownType.isInstance(value)) {
      return Inspector.inspect(unknownCodec, value);
    }
    return value;
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
    private Class<? extends V> unknownType;
    private Codec<? extends V> unknownCodec;

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
          !tagOf.containsKey(type) && !type.equals(unknownType),
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
     * <p>The fallback covers the whole {@code [tag][value]} span rather than only the body: an
     * unknown alternative has no registered tag this codec could write back, so its bytes are
     * indivisible. That also means the fallback is an ordinary codec, built from the same
     * combinators as any other, and never has to be told which tag it saw:
     *
     * <pre>{@code
     * .otherwise(Unknown.class, Codecs.binary().xmap(Unknown::new, Unknown::raw))
     *
     * .otherwise(Unknown.class, Codecs.pair(Codecs.uint8(), Codecs.binary())
     *     .as(Unknown::new, Unknown::tag, Unknown::body))
     * }</pre>
     *
     * <p>Declaring the class keeps encoding checked: a value that is neither registered nor an
     * instance of this type raises a {@link CodecException} naming it, rather than reaching a
     * codec that cannot handle it.
     *
     * <p>A fallback that reads to end-of-stream needs a bounded scope — inside
     * {@link Codecs#prefixed} or {@link Codecs#terminated}, which hand down a bounded stream —
     * since an unknown alternative has unknown extent.
     *
     * @param type the class of values this fallback produces and encodes
     * @param codec the codec for the whole span of an unrecognised alternative
     * @param <U> the fallback subtype
     * @return this builder
     * @throws NullPointerException if type or codec is null
     * @throws IllegalArgumentException if a fallback is already registered, or if the type is
     *     already registered as an alternative
     */
    public <U extends V> Builder<T, V> otherwise(Class<U> type, Codec<U> codec) {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(codec, "codec");
      Preconditions.check(unknownType == null, "fallback is already registered");
      Preconditions.check(!tagOf.containsKey(type), "duplicate type: %s", type.getName());
      this.unknownType = type;
      this.unknownCodec = codec;
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
      return new ChoiceCodec<>(tagCodec, byTag, tagOf, unknownType, unknownCodec);
    }
  }
}
