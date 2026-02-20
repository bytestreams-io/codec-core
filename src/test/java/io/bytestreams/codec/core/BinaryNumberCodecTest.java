package io.bytestreams.codec.core;

import static io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RandomParametersExtension.class)
class BinaryNumberCodecTest {

  @Nested
  class IntegerTests {
    private final BinaryNumberCodec<Integer> codec = BinaryNumberCodec.ofInt();

    @Test
    void encode(@Randomize int value) throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toByteArray())
          .isEqualTo(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    @Test
    void decode(@Randomize byte[] value) throws IOException {
      ByteArrayInputStream input = new ByteArrayInputStream(value);
      assertThat(codec.decode(input)).isEqualTo(ByteBuffer.wrap(value, 0, Integer.BYTES).getInt());
      assertThat(input.available()).isEqualTo(value.length - Integer.BYTES);
    }

    @Test
    void decode_empty_stream() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }

    @Test
    void decode_insufficient_data() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03});
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }
  }

  @Nested
  class LongTests {
    private final BinaryNumberCodec<Long> codec = BinaryNumberCodec.ofLong();

    @Test
    void encode(@Randomize long value) throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toByteArray())
          .isEqualTo(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }

    @Test
    void decode(@Randomize(length = 10) byte[] value) throws IOException {
      ByteArrayInputStream input = new ByteArrayInputStream(value);
      assertThat(codec.decode(input)).isEqualTo(ByteBuffer.wrap(value, 0, Long.BYTES).getLong());
      assertThat(input.available()).isEqualTo(value.length - Long.BYTES);
    }

    @Test
    void decode_empty_stream() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }

    @Test
    void decode_insufficient_data() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03});
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }
  }

  @Nested
  class ShortTests {
    private final BinaryNumberCodec<Short> codec = BinaryNumberCodec.ofShort();

    @Test
    void encode(@Randomize short value) throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toByteArray())
          .isEqualTo(ByteBuffer.allocate(Short.BYTES).putShort(value).array());
    }

    @Test
    void decode(@Randomize byte[] value) throws IOException {
      ByteArrayInputStream input = new ByteArrayInputStream(value);
      assertThat(codec.decode(input)).isEqualTo(ByteBuffer.wrap(value, 0, Short.BYTES).getShort());
      assertThat(input.available()).isEqualTo(value.length - Short.BYTES);
    }

    @Test
    void decode_empty_stream() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }

    @Test
    void decode_insufficient_data() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01});
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }
  }

  @Nested
  class DoubleTests {
    private final BinaryNumberCodec<Double> codec = BinaryNumberCodec.ofDouble();

    @Test
    void encode(@Randomize double value) throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toByteArray())
          .isEqualTo(ByteBuffer.allocate(Double.BYTES).putDouble(value).array());
    }

    @Test
    void decode(@Randomize(length = 10) byte[] value) throws IOException {
      ByteArrayInputStream input = new ByteArrayInputStream(value);
      assertThat(codec.decode(input))
          .usingComparator(Double::compare)
          .isEqualTo(ByteBuffer.wrap(value, 0, Double.BYTES).getDouble());
      assertThat(input.available()).isEqualTo(value.length - Double.BYTES);
    }

    @Test
    void decode_empty_stream() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }

    @Test
    void decode_insufficient_data() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03});
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }
  }

  @Nested
  class FloatTests {
    private final BinaryNumberCodec<Float> codec = BinaryNumberCodec.ofFloat();

    @Test
    void encode(@Randomize float value) throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toByteArray())
          .isEqualTo(ByteBuffer.allocate(Float.BYTES).putFloat(value).array());
    }

    @Test
    void decode(@Randomize byte[] value) throws IOException {
      ByteArrayInputStream input = new ByteArrayInputStream(value);
      assertThat(codec.decode(input))
          .usingComparator(Float::compare)
          .isEqualTo(ByteBuffer.wrap(value, 0, Float.BYTES).getFloat());
      assertThat(input.available()).isEqualTo(value.length - Float.BYTES);
    }

    @Test
    void decode_empty_stream() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }

    @Test
    void decode_insufficient_data() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03});
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }
  }

  @Nested
  class UnsignedByteTests {
    private static final int UNSIGNED_BYTE_MAX = 0xFF;
    private final BinaryNumberCodec<Integer> codec = BinaryNumberCodec.ofUnsignedByte();

    @Test
    void encode(@Randomize(intMin = 0, intMax = UNSIGNED_BYTE_MAX + 1) int value)
        throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toByteArray()).isEqualTo(new byte[] {(byte) value});
    }

    @Test
    void encode_overflow(@Randomize(intMin = 1) int value) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      assertThatThrownBy(() -> codec.encode(UNSIGNED_BYTE_MAX + value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "value must be between 0 and %d, but was [%d]",
              UNSIGNED_BYTE_MAX, UNSIGNED_BYTE_MAX + value);
    }

    @Test
    void encode_negative_value(@Randomize(intMin = 1) int value) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      assertThatThrownBy(() -> codec.encode(-value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("value must be between 0 and %d, but was [%d]", UNSIGNED_BYTE_MAX, -value);
    }

    @Test
    void decode(@Randomize byte[] value) throws IOException {
      ByteArrayInputStream input = new ByteArrayInputStream(value);
      assertThat(codec.decode(input)).isEqualTo(value[0] & UNSIGNED_BYTE_MAX);
      assertThat(input.available()).isPositive();
    }

    @Test
    void decode_empty_stream() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }
  }

  @Nested
  class UnsignedShortTests {
    private static final int UNSIGNED_SHORT_MAX = 0xFFFF;
    private final BinaryNumberCodec<Integer> codec = BinaryNumberCodec.ofUnsignedShort();

    @Test
    void encode(@Randomize(intMin = 0, intMax = UNSIGNED_SHORT_MAX + 1) int value)
        throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toByteArray()).isEqualTo(new byte[] {(byte) (value >> 8), (byte) value});
    }

    @Test
    void encode_overflow(@Randomize(intMin = 1) int value) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      assertThatThrownBy(() -> codec.encode(UNSIGNED_SHORT_MAX + value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "value must be between 0 and %d, but was [%d]",
              UNSIGNED_SHORT_MAX, UNSIGNED_SHORT_MAX + value);
    }

    @Test
    void encode_negative_value(@Randomize(intMin = 1) int value) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      assertThatThrownBy(() -> codec.encode(-value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("value must be between 0 and %d, but was [%d]", UNSIGNED_SHORT_MAX, -value);
    }

    @Test
    void decode(@Randomize byte[] value) throws IOException {
      ByteArrayInputStream input = new ByteArrayInputStream(value);
      assertThat(codec.decode(input)).isEqualTo((value[0] & 0xFF) << 8 | value[1] & 0xFF);
      assertThat(input.available()).isEqualTo(value.length - 2);
    }

    @Test
    void decode_insufficient_data() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01});
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }
  }

  @Nested
  class UnsignedIntegerTests {
    private static final long UNSIGNED_INT_MAX = 0xFFFFFFFFL;
    private final BinaryNumberCodec<Long> codec = BinaryNumberCodec.ofUnsignedInt();

    @Test
    void encode(@Randomize(longMin = 0, longMax = UNSIGNED_INT_MAX + 1) long value)
        throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      codec.encode(value, output);
      assertThat(output.toByteArray())
          .isEqualTo(
              new byte[] {
                (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value
              });
    }

    @Test
    void encode_overflow(@Randomize(longMin = 1) long value) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      assertThatThrownBy(() -> codec.encode(UNSIGNED_INT_MAX + value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "value must be between 0 and %d, but was [%d]",
              UNSIGNED_INT_MAX, UNSIGNED_INT_MAX + value);
    }

    @Test
    void encode_negative_value(@Randomize(longMin = 1) long value) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      assertThatThrownBy(() -> codec.encode(-value, output))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("value must be between 0 and %d, but was [%d]", UNSIGNED_INT_MAX, -value);
    }

    @Test
    void decode(@Randomize byte[] value) throws IOException {
      ByteArrayInputStream input = new ByteArrayInputStream(value);
      assertThat(codec.decode(input))
          .isEqualTo(
              (long) (value[0] & 0xFF) << 24
                  | (long) (value[1] & 0xFF) << 16
                  | (long) (value[2] & 0xFF) << 8
                  | (long) (value[3] & 0xFF));
      assertThat(input.available()).isEqualTo(value.length - 4);
    }

    @Test
    void decode_empty_stream() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }

    @Test
    void decode_insufficient_data() {
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03});
      assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
    }
  }
}
