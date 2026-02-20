package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A codec for objects with tag-identified fields.
 *
 * <p>Each field in the stream is a tag-value pair, where the tag determines which codec to use for
 * the value. Supports duplicate tags and unknown tags via a configurable default codec.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TaggedObjectCodec<MyObject, String> codec = Codecs.<MyObject, String>tagged(MyObject::new, Codecs.ascii(4))
 *     .tag("code", Codecs.uint16())
 *     .tag("name", Codecs.ascii(10))
 *     .defaultCodec(Codecs.binary(8))
 *     .build();
 * }</pre>
 *
 * @param <T> the type of object to encode/decode
 * @param <K> the tag key type
 */
public class TaggedObjectCodec<T extends Tagged<T, K>, K> implements Codec<T> {

  private final Codec<K> tagCodec;
  private final Map<K, Codec<?>> codecs;
  private final Codec<?> defaultCodec;
  private final Supplier<T> factory;

  TaggedObjectCodec(
      Codec<K> tagCodec, Map<K, Codec<?>> codecs, Codec<?> defaultCodec, Supplier<T> factory) {
    this.tagCodec = tagCodec;
    this.codecs = Map.copyOf(codecs);
    this.defaultCodec = defaultCodec;
    this.factory = factory;
  }

  /**
   * Creates a new builder for constructing a TaggedObjectCodec.
   *
   * @param factory factory that creates new instances during decoding
   * @param tagCodec the codec used to read and write tags
   * @param <T> the type of object to encode/decode
   * @param <K> the tag key type
   * @return a new builder
   */
  public static <T extends Tagged<T, K>, K> Builder<T, K> builder(
      Supplier<T> factory, Codec<K> tagCodec) {
    return new Builder<>(factory, tagCodec);
  }

  @Override
  public EncodeResult encode(T value, OutputStream output) throws IOException {
    int count = 0;
    int totalBytes = 0;
    for (K tag : value.tags()) {
      for (Object item : value.getAll(tag)) {
        try {
          totalBytes += tagCodec.encode(tag, output).bytes();
          Codec<?> codec = codecs.getOrDefault(tag, defaultCodec);
          totalBytes += encodeValue(codec, item, output).bytes();
          count++;
        } catch (CodecException e) {
          throw e.withField(String.valueOf(tag));
        } catch (Exception e) {
          throw new CodecException(e.getMessage(), e).withField(String.valueOf(tag));
        }
      }
    }
    return new EncodeResult(count, totalBytes);
  }

  @SuppressWarnings("unchecked")
  private static EncodeResult encodeValue(Codec<?> codec, Object value, OutputStream output)
      throws IOException {
    return ((Codec<Object>) codec).encode(value, output);
  }

  @Override
  public T decode(InputStream input) throws IOException {
    T instance = Objects.requireNonNull(factory.get(), "factory.get() returned null");
    PushbackInputStream pushback = new PushbackInputStream(input);
    int next;
    while ((next = pushback.read()) != -1) {
      pushback.unread(next);
      K tag = null;
      try {
        tag = tagCodec.decode(pushback);
        Codec<?> codec = codecs.getOrDefault(tag, defaultCodec);
        Object value = codec.decode(pushback);
        instance.add(tag, value);
      } catch (CodecException e) {
        throw tag != null ? e.withField(String.valueOf(tag)) : e;
      } catch (Exception e) {
        CodecException ce = new CodecException(e.getMessage(), e);
        throw tag != null ? ce.withField(String.valueOf(tag)) : ce;
      }
    }
    return instance;
  }

  /**
   * Builder for constructing a TaggedObjectCodec.
   *
   * @param <T> the type of object to encode/decode
   * @param <K> the tag key type
   */
  public static class Builder<T extends Tagged<T, K>, K> {
    private final Codec<K> tagCodec;
    private final Map<K, Codec<?>> codecs = new HashMap<>();
    private Codec<?> defaultCodec = new NotImplementedCodec<>();
    private final Supplier<T> factory;

    Builder(Supplier<T> factory, Codec<K> tagCodec) {
      this.factory = Objects.requireNonNull(factory, "factory");
      this.tagCodec = Objects.requireNonNull(tagCodec, "tagCodec");
    }

    /**
     * Registers a codec for a specific tag.
     *
     * @param tag the tag
     * @param codec the codec for this tag's values
     * @return this builder
     */
    public Builder<T, K> tag(K tag, Codec<?> codec) {
      Objects.requireNonNull(tag, "tag");
      Objects.requireNonNull(codec, "codec");
      codecs.put(tag, codec);
      return this;
    }

    /**
     * Sets the default codec for unregistered tags.
     *
     * @param defaultCodec the default codec
     * @return this builder
     */
    public Builder<T, K> defaultCodec(Codec<?> defaultCodec) {
      this.defaultCodec = Objects.requireNonNull(defaultCodec, "defaultCodec");
      return this;
    }

    /**
     * Builds the TaggedObjectCodec.
     *
     * @return the constructed codec
     */
    public TaggedObjectCodec<T, K> build() {
      return new TaggedObjectCodec<>(tagCodec, codecs, defaultCodec, factory);
    }
  }
}
