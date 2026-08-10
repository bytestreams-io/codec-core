package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import io.bytestreams.codec.core.util.Predicates;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * A codec for objects with sequential fields.
 *
 * <p>Each field is encoded/decoded in the order it was added to the builder. Optional fields use a
 * predicate to determine presence - if the predicate returns false, the field is skipped during
 * both encoding and decoding.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SequentialObjectCodec<Message> codec = Codecs.<Message>sequential(Message::new)
 *     .field("id", idCodec, Message::getId, Message::setId)
 *     .field("content", contentCodec, Message::getContent, Message::setContent)
 *     .field("tag", tagCodec, Message::getTag, Message::setTag,
 *            msg -> msg.getId() > 0)  // optional, based on earlier field
 *     .build();
 * }</pre>
 *
 * @param <T> the type of object to encode/decode
 */
public class SequentialObjectCodec<T> implements Codec<T>, Inspectable<T> {

  private static final Logger logger = LoggerFactory.getLogger(SequentialObjectCodec.class);
  private static final String MDC_KEY = "codec.field";
  private static final String LOG_KEY_FIELD = "field";

  private final List<FieldCodec<T, ?>> fields;
  private final Supplier<T> factory;

  SequentialObjectCodec(List<FieldCodec<T, ?>> fields, Supplier<T> factory) {
    this.fields = List.copyOf(fields);
    this.factory = factory;
  }

  /**
   * Creates a new builder for constructing a SequentialObjectCodec.
   *
   * @param factory factory that creates new instances during decoding
   * @param <T> the type of object to encode/decode
   * @return a new builder
   */
  public static <T> Builder<T> builder(Supplier<T> factory) {
    return new Builder<>(factory);
  }

  @Override
  public EncodeResult encode(T value, OutputStream output) throws IOException {
    int fieldCount = 0;
    int totalBytes = 0;
    for (FieldCodec<T, ?> field : fields) {
      EncodeResult result = field.encode(value, output);
      totalBytes += result.bytes();
      if (result.bytes() > 0) {
        fieldCount++;
      }
    }
    logger
        .atDebug()
        .addKeyValue("type", value.getClass().getSimpleName())
        .addKeyValue("fields", fieldCount)
        .addKeyValue("bytes", totalBytes)
        .log("encoded");
    return new EncodeResult(fieldCount, totalBytes);
  }

  @Override
  public T decode(InputStream input) throws IOException {
    T instance = Objects.requireNonNull(factory.get(), "factory.get() returned null");
    for (FieldCodec<T, ?> field : fields) {
      field.decode(instance, input);
    }
    logger.atDebug().addKeyValue("type", instance.getClass().getSimpleName()).log("decoded");
    return instance;
  }

  @Override
  public Object inspect(T object) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (FieldCodec<T, ?> field : fields) {
      if (field.presence().test(object)) {
        result.put(field.name(), Inspector.inspect(field.codec(), field.get(object)));
      }
    }
    return result;
  }

  /** Builder for constructing a SequentialObjectCodec. */
  public static class Builder<T> {
    private final List<FieldCodec<T, ?>> fields = new ArrayList<>();
    private final Supplier<T> factory;

    Builder(Supplier<T> factory) {
      this.factory = Objects.requireNonNull(factory, "factory");
    }

    /**
     * Adds a required field to the codec.
     *
     * @param name the field name (used in error messages)
     * @param codec the codec for this field's value
     * @param getter function to extract the field value for encoding
     * @param setter consumer to set the field value when decoding
     * @param <V> the field value type
     * @return this builder
     */
    public <V> Builder<T> field(
        String name, Codec<V> codec, Function<T, V> getter, BiConsumer<T, V> setter) {
      return field(name, codec, getter, setter, Predicates.alwaysTrue());
    }

    /**
     * Adds a fixed-value field that is present on the wire but not stored on the object.
     *
     * <p>Writes {@code value} on encode. On decode, reads a value, verifies it matches, and
     * discards it. Intended for record type indicators, magic numbers and protocol version markers
     * — values the format requires but the domain object has no reason to carry.
     *
     * <pre>{@code
     * Codecs.<BatchTrailer>sequential(BatchTrailer::new)
     *     .constant("type", Codecs.ascii(2), "BT")
     *     .field("count", Codecs.uint16(), BatchTrailer::getCount, BatchTrailer::setCount)
     *     .build();
     * }</pre>
     *
     * <p>Comparison is by content, so {@code byte[]} constants behave as expected and are reported
     * as hex on mismatch. A failure carries the field path like any other decode error:
     * {@code field [batch.trailer.type]: expected constant [BT] but got [XX]}.
     *
     * <p>The constant is held by reference and never copied, since an arbitrary {@code V} cannot
     * be. Mutating it after the codec is built changes what the codec writes and accepts, so pass a
     * value nothing else will modify.
     *
     * @param name the field name (used in error messages)
     * @param codec the codec for the constant's wire format
     * @param value the expected constant value
     * @param <V> the constant's value type
     * @return this builder
     * @throws NullPointerException if any argument is null
     */
    public <V> Builder<T> constant(String name, Codec<V> codec, V value) {
      return constant(name, codec, value, v -> Objects.deepEquals(value, v));
    }

    /**
     * Adds a fixed-value field that is written on encode but matched loosely on decode.
     *
     * <p>Use when the wire format tolerates variants of the same constant, such as a fixed-width
     * type indicator that may or may not be padded. {@code value} is always what gets written.
     *
     * <p>{@code value} must itself satisfy {@code accepts}, since it is checked on encode like any
     * other value. This is verified here rather than left to fail at encode time.
     *
     * @param name the field name (used in error messages)
     * @param codec the codec for the constant's wire format
     * @param value the value written on encode
     * @param accepts the condition a decoded value must satisfy
     * @param <V> the constant's value type
     * @return this builder
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code value} does not satisfy {@code accepts}
     */
    public <V> Builder<T> constant(String name, Codec<V> codec, V value, Predicate<V> accepts) {
      Objects.requireNonNull(codec, "codec");
      Objects.requireNonNull(value, "value");
      Objects.requireNonNull(accepts, "accepts");
      Preconditions.check(
          accepts.test(value),
          "constant [%s] must satisfy its own accepts predicate",
          Values.render(value));
      Codec<V> verifying =
          codec.validate(
              accepts,
              actual ->
                  "expected constant [%s] but got [%s]"
                      .formatted(Values.render(value), Values.render(actual)));
      return field(
          name,
          verifying,
          object -> value,
          (object, ignored) -> {
            /* not stored */
          });
    }

    /**
     * Adds a field to the codec with a presence predicate.
     *
     * <p>The presence predicate determines whether the field should be encoded/decoded. If the
     * predicate returns false, the field is skipped. Note that during decoding, the predicate can
     * only reference fields that have already been decoded (earlier in the field order).
     *
     * @param name the field name (used in error messages)
     * @param codec the codec for this field's value
     * @param getter function to extract the field value for encoding
     * @param setter consumer to set the field value when decoding
     * @param presence predicate to determine if field is present
     * @param <V> the field value type
     * @return this builder
     */
    public <V> Builder<T> field(
        String name,
        Codec<V> codec,
        Function<T, V> getter,
        BiConsumer<T, V> setter,
        Predicate<T> presence) {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(codec, "codec");
      Objects.requireNonNull(getter, "getter");
      Objects.requireNonNull(setter, "setter");
      Objects.requireNonNull(presence, "presence");
      fields.add(new FieldCodec<>(name, codec, getter, setter, presence));
      return this;
    }

    /**
     * Adds a field to the codec using a FieldSpec.
     *
     * <p>The field's presence is determined by {@link FieldSpec#presence()}, which defaults to
     * always true (required field).
     *
     * @param spec the field specification
     * @param <V> the field value type
     * @return this builder
     */
    public <V> Builder<T> field(FieldSpec<T, V> spec) {
      Objects.requireNonNull(spec, "spec");
      return field(spec.name(), spec.codec(), spec::get, spec::set, spec.presence());
    }

    /**
     * Builds the SequentialObjectCodec.
     *
     * @return the constructed codec
     * @throws IllegalArgumentException if no fields were added
     */
    public SequentialObjectCodec<T> build() {
      Preconditions.check(!fields.isEmpty(), "at least one field is required");
      return new SequentialObjectCodec<>(fields, factory);
    }
  }

  /** Package-private field codec that handles encoding/decoding a single field. */
  static class FieldCodec<T, V> {
    private final String name;
    private final Codec<V> codec;
    private final Function<T, V> getter;
    private final BiConsumer<T, V> setter;
    private final Predicate<T> presence;

    FieldCodec(
        String name,
        Codec<V> codec,
        Function<T, V> getter,
        BiConsumer<T, V> setter,
        Predicate<T> presence) {
      this.name = name;
      this.codec = codec;
      this.getter = getter;
      this.setter = setter;
      this.presence = presence;
    }

    String name() {
      return name;
    }

    Codec<V> codec() {
      return codec;
    }

    Predicate<T> presence() {
      return presence;
    }

    V get(T object) {
      return getter.apply(object);
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

    EncodeResult encode(T object, OutputStream output) {
      if (!presence.test(object)) {
        logger.atTrace().addKeyValue(LOG_KEY_FIELD, name).log("skipped");
        return EncodeResult.EMPTY;
      }
      boolean trace = logger.isTraceEnabled();
      String previousPath = trace ? pushFieldPath(name) : null;
      try {
        EncodeResult result = codec.encode(getter.apply(object), output);
        if (trace) {
          logger
              .atTrace()
              .addKeyValue(LOG_KEY_FIELD, MDC.get(MDC_KEY))
              .addKeyValue("bytes", result.bytes())
              .log("encoded");
        }
        return result;
      } catch (CodecException e) {
        throw e.withField(name);
      } catch (Exception e) {
        throw new CodecException(e.getMessage(), e).withField(name);
      } finally {
        if (trace) {
          popFieldPath(previousPath);
        }
      }
    }

    void decode(T object, InputStream input) {
      if (!presence.test(object)) {
        logger.atTrace().addKeyValue(LOG_KEY_FIELD, name).log("skipped");
        return;
      }
      boolean trace = logger.isTraceEnabled();
      String previousPath = trace ? pushFieldPath(name) : null;
      try {
        setter.accept(object, codec.decode(input));
        if (trace) {
          logger.atTrace().addKeyValue(LOG_KEY_FIELD, MDC.get(MDC_KEY)).log("decoded");
        }
      } catch (CodecException e) {
        throw e.withField(name);
      } catch (Exception e) {
        throw new CodecException(e.getMessage(), e).withField(name);
      } finally {
        if (trace) {
          popFieldPath(previousPath);
        }
      }
    }
  }
}
