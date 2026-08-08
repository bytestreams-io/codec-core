package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Codec for {@link Number}s encoded as fixed-length binary, big-endian unless
 * {@link #withOrder(ByteOrder)} says otherwise.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Codec<Integer> int32 = Codecs.int32();
 * Codec<Long> uint32 = Codecs.uint32();
 * Codec<Long> uint32le = Codecs.uint32(ByteOrder.LITTLE_ENDIAN);
 * }</pre>
 *
 * @param <V> the {@link Number} type this codec handles
 */
public class BinaryNumberCodec<V extends Number> implements Codec<V> {
  private final int byteLength;
  private final BiConsumer<ByteBuffer, V> writer;
  private final Function<ByteBuffer, V> reader;
  private final Consumer<V> validator;
  private final ByteOrder byteOrder;

  BinaryNumberCodec(
      int byteLength, BiConsumer<ByteBuffer, V> writer, Function<ByteBuffer, V> reader) {
    this(byteLength, writer, reader, v -> {});
  }

  BinaryNumberCodec(
      int byteLength,
      BiConsumer<ByteBuffer, V> writer,
      Function<ByteBuffer, V> reader,
      Consumer<V> validator) {
    this(byteLength, writer, reader, validator, ByteOrder.BIG_ENDIAN);
  }

  private BinaryNumberCodec(
      int byteLength,
      BiConsumer<ByteBuffer, V> writer,
      Function<ByteBuffer, V> reader,
      Consumer<V> validator,
      ByteOrder byteOrder) {
    this.byteLength = byteLength;
    this.writer = writer;
    this.reader = reader;
    this.validator = validator;
    this.byteOrder = byteOrder;
  }

  /**
   * Returns a codec that reads and writes the same value in the given byte order.
   *
   * <p>Single-byte codecs are unaffected, since there is no order to choose.
   *
   * @param order the byte order to use
   * @return a codec equivalent to this one but using {@code order}
   * @throws NullPointerException if order is null
   */
  public BinaryNumberCodec<V> withOrder(ByteOrder order) {
    Objects.requireNonNull(order, "order");
    return new BinaryNumberCodec<>(byteLength, writer, reader, validator, order);
  }

  /**
   * Creates a codec for signed integer values (-2147483648 to 2147483647).
   *
   * <p>Big-endian; use {@link #withOrder(ByteOrder)} for little-endian.
   *
   * @return a new codec for signed integers
   */
  public static BinaryNumberCodec<Integer> ofInt() {
    return new BinaryNumberCodec<>(Integer.BYTES, ByteBuffer::putInt, ByteBuffer::getInt);
  }

  /**
   * Creates a codec for signed long values (-9223372036854775808 to 9223372036854775807).
   *
   * <p>Big-endian; use {@link #withOrder(ByteOrder)} for little-endian.
   *
   * @return a new codec for signed longs
   */
  public static BinaryNumberCodec<Long> ofLong() {
    return new BinaryNumberCodec<>(Long.BYTES, ByteBuffer::putLong, ByteBuffer::getLong);
  }

  /**
   * Creates a codec for signed short values (-32768 to 32767).
   *
   * <p>Big-endian; use {@link #withOrder(ByteOrder)} for little-endian.
   *
   * @return a new codec for signed shorts
   */
  public static BinaryNumberCodec<Short> ofShort() {
    return new BinaryNumberCodec<>(Short.BYTES, ByteBuffer::putShort, ByteBuffer::getShort);
  }

  /**
   * Creates a codec for double values (IEEE 754 double-precision, 8 bytes).
   *
   * <p>Big-endian; use {@link #withOrder(ByteOrder)} for little-endian.
   *
   * @return a new codec for doubles
   */
  public static BinaryNumberCodec<Double> ofDouble() {
    return new BinaryNumberCodec<>(Double.BYTES, ByteBuffer::putDouble, ByteBuffer::getDouble);
  }

  /**
   * Creates a codec for float values (IEEE 754 single-precision, 4 bytes).
   *
   * <p>Big-endian; use {@link #withOrder(ByteOrder)} for little-endian.
   *
   * @return a new codec for floats
   */
  public static BinaryNumberCodec<Float> ofFloat() {
    return new BinaryNumberCodec<>(Float.BYTES, ByteBuffer::putFloat, ByteBuffer::getFloat);
  }

  /**
   * Creates a codec for unsigned byte values (0 to 255).
   *
   * <p>Byte order does not apply to a single byte.
   *
   * @return a new codec for unsigned bytes
   */
  public static BinaryNumberCodec<Integer> ofUnsignedByte() {
    return new BinaryNumberCodec<>(
        Byte.BYTES,
        (buf, v) -> buf.put(v.byteValue()),
        buf -> Byte.toUnsignedInt(buf.get()),
        v -> validateRange(v, 0xFF));
  }

  /**
   * Creates a codec for unsigned short values (0 to 65535).
   *
   * <p>Big-endian; use {@link #withOrder(ByteOrder)} for little-endian.
   *
   * @return a new codec for unsigned shorts
   */
  public static BinaryNumberCodec<Integer> ofUnsignedShort() {
    return new BinaryNumberCodec<>(
        Short.BYTES,
        (buf, v) -> buf.putShort(v.shortValue()),
        buf -> Short.toUnsignedInt(buf.getShort()),
        v -> validateRange(v, 0xFFFF));
  }

  /**
   * Creates a codec for unsigned integer values (0 to 4294967295).
   *
   * <p>Big-endian; use {@link #withOrder(ByteOrder)} for little-endian.
   *
   * @return a new codec for unsigned integers
   */
  public static BinaryNumberCodec<Long> ofUnsignedInt() {
    return new BinaryNumberCodec<>(
        Integer.BYTES,
        (buf, v) -> buf.putInt(v.intValue()),
        buf -> Integer.toUnsignedLong(buf.getInt()),
        v -> validateRange(v, 0xFFFFFFFFL));
  }

  private static <V extends Number> void validateRange(V value, long max) {
    String message =
        "value must be between 0 and %d, but was [%d]".formatted(max, value.longValue());
    Preconditions.check(value.longValue() >= 0, message);
    Preconditions.check(value.longValue() <= max, message);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the value is outside the valid range for unsigned types
   */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    validator.accept(value);
    ByteBuffer buffer = ByteBuffer.allocate(byteLength).order(byteOrder);
    writer.accept(buffer, value);
    output.write(buffer.array());
    return EncodeResult.ofBytes(byteLength);
  }

  /**
   * {@inheritDoc}
   *
   * @throws java.io.EOFException if the stream ends before the required bytes are read
   */
  @Override
  public V decode(InputStream input) throws IOException {
    return reader.apply(
        ByteBuffer.wrap(InputStreams.readFully(input, byteLength)).order(byteOrder));
  }
}
