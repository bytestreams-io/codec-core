package io.bytestreams.codec.core;

/**
 * Factory for creating object codec builders.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Ordered object codec
 * Codec<Message> ordered = ObjectCodec.<Message>ordered()
 *     .field("name", nameCodec, Message::getName, Message::setName)
 *     .factory(Message::new)
 *     .build();
 *
 * // Tagged object codec
 * Codec<MyObject> tagged = ObjectCodec.<MyObject>tagged()
 *     .tagCodec(FixedCodePointStringCodec.builder(4).build())
 *     .field("code", BinaryNumberCodec.ofUnsignedShort())
 *     .factory(MyObject::new)
 *     .build();
 * }</pre>
 */
public class ObjectCodec {
  private ObjectCodec() {}

  /**
   * Creates a new builder for an ordered object codec.
   *
   * @param <T> the type of object to encode/decode
   * @return a new ordered object codec builder
   */
  public static <T> OrderedObjectCodec.Builder<T> ordered() {
    return OrderedObjectCodec.builder();
  }

  /**
   * Creates a new builder for a tagged object codec.
   *
   * @param <T> the type of object to encode/decode
   * @return a new tagged object codec builder
   */
  public static <T extends Tagged<T>> TaggedObjectCodec.Builder<T> tagged() {
    return TaggedObjectCodec.builder();
  }
}
