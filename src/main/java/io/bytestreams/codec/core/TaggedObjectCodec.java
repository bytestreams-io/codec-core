package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
public class TaggedObjectCodec<T extends Tagged<T, K>, K> implements Codec<T>, Inspectable<T> {

  private static final Logger logger = LoggerFactory.getLogger(TaggedObjectCodec.class);
  private static final String MDC_KEY = "codec.field";
  private static final String LOG_KEY_FIELD = "field";

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
        totalBytes += encodeTag(tag, item, output);
        count++;
      }
    }
    logger
        .atDebug()
        .addKeyValue("type", value.getClass().getSimpleName())
        .addKeyValue("tags", count)
        .addKeyValue("bytes", totalBytes)
        .log("encoded");
    return new EncodeResult(count, totalBytes);
  }

  private int encodeTag(K tag, Object item, OutputStream output) throws IOException {
    String tagStr = String.valueOf(tag);
    boolean trace = logger.isTraceEnabled();
    String previousPath = trace ? pushFieldPath(tagStr) : null;
    try {
      int tagBytes = tagCodec.encode(tag, output).bytes();
      Codec<?> codec = codecs.getOrDefault(tag, defaultCodec);
      int valueBytes = encodeValue(codec, item, output).bytes();
      int fieldBytes = tagBytes + valueBytes;
      if (trace) {
        logger
            .atTrace()
            .addKeyValue(LOG_KEY_FIELD, MDC.get(MDC_KEY))
            .addKeyValue("bytes", fieldBytes)
            .log("encoded");
      }
      return fieldBytes;
    } catch (CodecException e) {
      throw e.withField(tagStr);
    } catch (Exception e) {
      throw new CodecException(e.getMessage(), e).withField(tagStr);
    } finally {
      if (trace) {
        popFieldPath(previousPath);
      }
    }
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
    int count = 0;
    while ((next = pushback.read()) != -1) {
      pushback.unread(next);
      decodeTag(instance, pushback);
      count++;
    }
    logger
        .atDebug()
        .addKeyValue("type", instance.getClass().getSimpleName())
        .addKeyValue("tags", count)
        .log("decoded");
    return instance;
  }

  private void decodeTag(T instance, PushbackInputStream pushback) throws IOException {
    K tag = null;
    String tagStr = null;
    try {
      tag = tagCodec.decode(pushback);
      tagStr = String.valueOf(tag);
      boolean trace = logger.isTraceEnabled();
      String previousPath = trace ? pushFieldPath(tagStr) : null;
      try {
        Codec<?> codec = codecs.getOrDefault(tag, defaultCodec);
        Object tagValue = codec.decode(pushback);
        instance.add(tag, tagValue);
        if (trace) {
          logger.atTrace().addKeyValue(LOG_KEY_FIELD, MDC.get(MDC_KEY)).log("decoded");
        }
      } finally {
        if (trace) {
          popFieldPath(previousPath);
        }
      }
    } catch (CodecException e) {
      throw tagStr != null ? e.withField(tagStr) : e;
    } catch (Exception e) {
      CodecException ce = new CodecException(e.getMessage(), e);
      throw tagStr != null ? ce.withField(tagStr) : ce;
    }
  }

  @Override
  public Object inspect(T object) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (K tag : object.tags()) {
      List<Object> values = object.getAll(tag);
      Codec<?> codec = codecs.getOrDefault(tag, defaultCodec);
      result.put(
          String.valueOf(tag), values.stream().map(v -> Inspector.inspect(codec, v)).toList());
    }
    return result;
  }

  private static String pushFieldPath(String name) {
    String previous = MDC.get(MDC_KEY);
    MDC.put(MDC_KEY, previous == null ? name : previous + "." + name);
    return previous;
  }

  private static void popFieldPath(String previous) {
    if (previous == null) {
      MDC.remove(MDC_KEY);
    } else {
      MDC.put(MDC_KEY, previous);
    }
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
