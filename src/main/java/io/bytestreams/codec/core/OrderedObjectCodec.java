package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import io.bytestreams.codec.core.util.Predicates;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A codec for objects with ordered fields.
 *
 * <p>Each field is encoded/decoded in the order it was added to the builder. Optional fields use a
 * predicate to determine presence - if the predicate returns false, the field is skipped during
 * both encoding and decoding.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * OrderedObjectCodec<Message> codec = OrderedObjectCodec.<Message>builder()
 *     .field("id", idCodec, Message::getId, Message::setId)
 *     .field("content", contentCodec, Message::getContent, Message::setContent)
 *     .field("tag", tagCodec, Message::getTag, Message::setTag,
 *            msg -> msg.getId() > 0)  // optional, based on earlier field
 *     .supplier(Message::new)
 *     .build();
 * }</pre>
 *
 * @param <T> the type of object to encode/decode
 */
public class OrderedObjectCodec<T> implements Codec<T> {

  private final List<FieldCodec<T, ?>> fields;
  private final Supplier<T> supplier;

  OrderedObjectCodec(List<FieldCodec<T, ?>> fields, Supplier<T> supplier) {
    this.fields = List.copyOf(fields);
    this.supplier = supplier;
  }

  /**
   * Creates a new builder for constructing an OrderedObjectCodec.
   *
   * @param <T> the type of object to encode/decode
   * @return a new builder
   */
  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  @Override
  public void encode(T value, OutputStream output) throws IOException {
    for (FieldCodec<T, ?> field : fields) {
      field.encode(value, output);
    }
  }

  @Override
  public T decode(InputStream input) throws IOException {
    T instance = Objects.requireNonNull(supplier.get(), "supplier.get() returned null");
    for (FieldCodec<T, ?> field : fields) {
      field.decode(instance, input);
    }
    return instance;
  }

  /** Builder for constructing an OrderedObjectCodec. */
  public static class Builder<T> {
    private final List<FieldCodec<T, ?>> fields = new ArrayList<>();
    private Supplier<T> supplier;

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
     * Sets the supplier for creating new instances during decoding.
     *
     * @param supplier supplier that creates new instances
     * @return this builder
     */
    public Builder<T> supplier(Supplier<T> supplier) {
      this.supplier = Objects.requireNonNull(supplier, "supplier");
      return this;
    }

    /**
     * Builds the OrderedObjectCodec.
     *
     * @return the constructed codec
     * @throws NullPointerException if supplier was not set
     * @throws IllegalArgumentException if no fields were added
     */
    public OrderedObjectCodec<T> build() {
      Objects.requireNonNull(supplier, "supplier must be set");
      Preconditions.check(!fields.isEmpty(), "at least one field is required");
      return new OrderedObjectCodec<>(fields, supplier);
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

    void encode(T object, OutputStream output) {
      if (presence.test(object)) {
        try {
          codec.encode(getter.apply(object), output);
        } catch (CodecException e) {
          throw e.withField(name);
        } catch (Exception e) {
          throw new CodecException(e.getMessage(), e).withField(name);
        }
      }
    }

    void decode(T object, InputStream input) {
      if (presence.test(object)) {
        try {
          setter.accept(object, codec.decode(input));
        } catch (CodecException e) {
          throw e.withField(name);
        } catch (Exception e) {
          throw new CodecException(e.getMessage(), e).withField(name);
        }
      }
    }
  }
}
