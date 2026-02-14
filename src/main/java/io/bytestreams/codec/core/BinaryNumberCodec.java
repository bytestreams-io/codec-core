package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.InputStreams;
import io.bytestreams.codec.core.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Codec for {@link Number}s encoded as fixed-length big-endian binary.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FixedLengthCodec<Integer> codec = NumberCodecs.ofInt();
 * FixedLengthCodec<Long> codec = NumberCodecs.ofUnsignedInt();
 * }</pre>
 *
 * @param <V> the {@link Number} type this codec handles
 */
public class BinaryNumberCodec<V extends Number> implements FixedLengthCodec<V> {
  private final int byteLength;
  private final BiConsumer<ByteBuffer, V> writer;
  private final Function<ByteBuffer, V> reader;
  private final Consumer<V> validator;

  BinaryNumberCodec(
      int byteLength, BiConsumer<ByteBuffer, V> writer, Function<ByteBuffer, V> reader) {
    this(byteLength, writer, reader, v -> {});
  }

  BinaryNumberCodec(
      int byteLength,
      BiConsumer<ByteBuffer, V> writer,
      Function<ByteBuffer, V> reader,
      Consumer<V> validator) {
    this.byteLength = byteLength;
    this.writer = writer;
    this.reader = reader;
    this.validator = validator;
  }

  /**
   * Creates a codec for signed integer values (-2147483648 to 2147483647).
   *
   * @return a new codec for signed integers
   */
  public static BinaryNumberCodec<Integer> ofInt() {
    return new BinaryNumberCodec<>(Integer.BYTES, ByteBuffer::putInt, ByteBuffer::getInt);
  }

  /**
   * Creates a codec for signed long values (-9223372036854775808 to 9223372036854775807).
   *
   * @return a new codec for signed longs
   */
  public static BinaryNumberCodec<Long> ofLong() {
    return new BinaryNumberCodec<>(Long.BYTES, ByteBuffer::putLong, ByteBuffer::getLong);
  }

  /**
   * Creates a codec for signed short values (-32768 to 32767).
   *
   * @return a new codec for signed shorts
   */
  public static BinaryNumberCodec<Short> ofShort() {
    return new BinaryNumberCodec<>(Short.BYTES, ByteBuffer::putShort, ByteBuffer::getShort);
  }

  /**
   * Creates a codec for double values (IEEE 754 double-precision, 8 bytes).
   *
   * @return a new codec for doubles
   */
  public static BinaryNumberCodec<Double> ofDouble() {
    return new BinaryNumberCodec<>(Double.BYTES, ByteBuffer::putDouble, ByteBuffer::getDouble);
  }

  /**
   * Creates a codec for float values (IEEE 754 single-precision, 4 bytes).
   *
   * @return a new codec for floats
   */
  public static BinaryNumberCodec<Float> ofFloat() {
    return new BinaryNumberCodec<>(Float.BYTES, ByteBuffer::putFloat, ByteBuffer::getFloat);
  }

  /**
   * Creates a codec for unsigned byte values (0 to 255).
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

  @Override
  public int getLength() {
    return byteLength;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the value is outside the valid range for unsigned types
   */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    validator.accept(value);
    ByteBuffer buffer = ByteBuffer.allocate(byteLength);
    writer.accept(buffer, value);
    output.write(buffer.array());
    return EncodeResult.ofBytes(byteLength);
  }

  @Override
  public V decode(InputStream input) throws IOException {
    return reader.apply(ByteBuffer.wrap(InputStreams.readFully(input, byteLength)));
  }
}
