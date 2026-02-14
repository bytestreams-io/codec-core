package io.bytestreams.codec.core;

import java.util.function.Supplier;

/**
 * Factory for creating object codec builders.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Ordered object codec
 * Codec<Message> ordered = ObjectCodecs.<Message>ofOrdered(Message::new)
 *     .field("name", nameCodec, Message::getName, Message::setName)
 *     .build();
 *
 * // Tagged object codec
 * Codec<MyObject> tagged = ObjectCodecs.<MyObject>ofTagged(MyObject::new)
 *     .tagCodec(StringCodecs.ofCodePoint(4).build())
 *     .field("code", NumberCodecs.ofUnsignedShort())
 *     .build();
 * }</pre>
 */
public class ObjectCodecs {
  private ObjectCodecs() {}

  /**
   * Creates a new builder for an ordered object codec.
   *
   * @param factory factory that creates new instances during decoding
   * @param <T> the type of object to encode/decode
   * @return a new ordered object codec builder
   */
  public static <T> OrderedObjectCodec.Builder<T> ofOrdered(Supplier<T> factory) {
    return OrderedObjectCodec.builder(factory);
  }

  /**
   * Creates a new builder for a tagged object codec.
   *
   * @param factory factory that creates new instances during decoding
   * @param <T> the type of object to encode/decode
   * @return a new tagged object codec builder
   */
  public static <T extends Tagged<T>> TaggedObjectCodec.Builder<T> ofTagged(Supplier<T> factory) {
    return TaggedObjectCodec.builder(factory);
  }
}
