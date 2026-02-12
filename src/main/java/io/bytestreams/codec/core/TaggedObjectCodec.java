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
 * <p>Each field in the stream is a tag-value pair, where the tag (a string) determines which codec
 * to use for the value. Supports duplicate tags and unknown tags via a configurable default codec.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TaggedObjectCodec<MyObject> codec = TaggedObjectCodec.<MyObject>builder()
 *     .tagCodec(new CodePointStringCodec(4, UTF_8))
 *     .field("code", new UnsignedShortCodec())
 *     .field("name", new CodePointStringCodec(10, UTF_8))
 *     .defaultCodec(new BinaryCodec(8))
 *     .maxFields(100)
 *     .factory(MyObject::new)
 *     .build();
 * }</pre>
 *
 * @param <T> the type of object to encode/decode
 */
public class TaggedObjectCodec<T extends Tagged<T>> implements Codec<T> {

  private final Codec<String> tagCodec;
  private final Map<String, Codec<?>> fields;
  private final Codec<?> defaultCodec;
  private final Supplier<T> factory;
  private final int maxFields;

  TaggedObjectCodec(
      Codec<String> tagCodec,
      Map<String, Codec<?>> fields,
      Codec<?> defaultCodec,
      Supplier<T> factory,
      int maxFields) {
    this.tagCodec = tagCodec;
    this.fields = Map.copyOf(fields);
    this.defaultCodec = defaultCodec;
    this.factory = factory;
    this.maxFields = maxFields;
  }

  /**
   * Creates a new builder for constructing a TaggedObjectCodec.
   *
   * @param <T> the type of object to encode/decode
   * @return a new builder
   */
  public static <T extends Tagged<T>> Builder<T> builder() {
    return new Builder<>();
  }

  @Override
  public EncodeResult encode(T value, OutputStream output) throws IOException {
    int count = 0;
    int totalBytes = 0;
    for (String tag : value.tags()) {
      for (Object item : value.getAll(tag)) {
        try {
          totalBytes += tagCodec.encode(tag, output).bytes();
          Codec<?> codec = fields.getOrDefault(tag, defaultCodec);
          totalBytes += encodeValue(codec, item, output).bytes();
          count++;
        } catch (CodecException e) {
          throw e.withField(tag);
        } catch (Exception e) {
          throw new CodecException(e.getMessage(), e).withField(tag);
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
    int count = 0;
    int next;
    while (count < maxFields && (next = pushback.read()) != -1) {
      pushback.unread(next);
      String tag = null;
      try {
        tag = tagCodec.decode(pushback);
        Codec<?> codec = fields.getOrDefault(tag, defaultCodec);
        Object value = codec.decode(pushback);
        instance.add(tag, value);
        count++;
      } catch (CodecException e) {
        throw tag != null ? e.withField(tag) : e;
      } catch (Exception e) {
        CodecException ce = new CodecException(e.getMessage(), e);
        throw tag != null ? ce.withField(tag) : ce;
      }
    }
    return instance;
  }

  /** Builder for constructing a TaggedObjectCodec. */
  public static class Builder<T extends Tagged<T>> {
    private Codec<String> tagCodec;
    private final Map<String, Codec<?>> fields = new HashMap<>();
    private Codec<?> defaultCodec;
    private Supplier<T> factory;
    private int maxFields = Integer.MAX_VALUE;

    /**
     * Sets the codec used to read and write tag strings.
     *
     * @param tagCodec the tag codec
     * @return this builder
     */
    public Builder<T> tagCodec(Codec<String> tagCodec) {
      this.tagCodec = Objects.requireNonNull(tagCodec, "tagCodec");
      return this;
    }

    /**
     * Registers a codec for a specific tag.
     *
     * @param tag the tag name
     * @param codec the codec for this tag's values
     * @return this builder
     */
    public Builder<T> field(String tag, Codec<?> codec) {
      Objects.requireNonNull(tag, "tag");
      Objects.requireNonNull(codec, "codec");
      fields.put(tag, codec);
      return this;
    }

    /**
     * Sets the default codec for unregistered tags.
     *
     * @param defaultCodec the default codec
     * @return this builder
     */
    public Builder<T> defaultCodec(Codec<?> defaultCodec) {
      this.defaultCodec = Objects.requireNonNull(defaultCodec, "defaultCodec");
      return this;
    }

    /**
     * Sets the maximum number of fields to decode.
     *
     * @param maxFields the maximum number of fields
     * @return this builder
     * @throws IllegalArgumentException if maxFields is negative
     */
    public Builder<T> maxFields(int maxFields) {
      if (maxFields < 0) {
        throw new IllegalArgumentException(
            "maxFields must be non-negative, but was [%d]".formatted(maxFields));
      }
      this.maxFields = maxFields;
      return this;
    }

    /**
     * Sets the factory for creating new instances during decoding.
     *
     * @param factory factory that creates new instances
     * @return this builder
     */
    public Builder<T> factory(Supplier<T> factory) {
      this.factory = Objects.requireNonNull(factory, "factory");
      return this;
    }

    /**
     * Builds the TaggedObjectCodec.
     *
     * @return the constructed codec
     * @throws NullPointerException if tagCodec or factory was not set
     */
    public TaggedObjectCodec<T> build() {
      Objects.requireNonNull(tagCodec, "tagCodec must be set");
      Objects.requireNonNull(factory, "factory must be set");
      Codec<?> effectiveDefault = defaultCodec != null ? defaultCodec : new NotImplementedCodec<>();
      return new TaggedObjectCodec<>(tagCodec, fields, effectiveDefault, factory, maxFields);
    }
  }
}
