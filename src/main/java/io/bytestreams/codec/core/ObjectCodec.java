package io.bytestreams.codec.core;

import java.util.function.Supplier;

/**
 * Factory for creating object codec builders.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Ordered object codec
 * Codec<Message> ordered = ObjectCodec.<Message>ordered(Message::new)
 *     .field("name", nameCodec, Message::getName, Message::setName)
 *     .build();
 *
 * // Tagged object codec
 * Codec<MyObject> tagged = ObjectCodec.<MyObject>tagged(MyObject::new)
 *     .tagCodec(FixedCodePointStringCodec.builder(4).build())
 *     .field("code", BinaryNumberCodec.ofUnsignedShort())
 *     .build();
 * }</pre>
 */
public class ObjectCodec {
  private ObjectCodec() {}

  /**
   * Creates a new builder for an ordered object codec.
   *
   * @param factory factory that creates new instances during decoding
   * @param <T> the type of object to encode/decode
   * @return a new ordered object codec builder
   */
  public static <T> OrderedObjectCodec.Builder<T> ordered(Supplier<T> factory) {
    return OrderedObjectCodec.builder(factory);
  }

  /**
   * Creates a new builder for a tagged object codec.
   *
   * @param factory factory that creates new instances during decoding
   * @param <T> the type of object to encode/decode
   * @return a new tagged object codec builder
   */
  public static <T extends Tagged<T>> TaggedObjectCodec.Builder<T> tagged(Supplier<T> factory) {
    return TaggedObjectCodec.builder(factory);
  }
}
