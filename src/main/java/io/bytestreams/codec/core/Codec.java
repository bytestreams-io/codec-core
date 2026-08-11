package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Converter;
import io.bytestreams.codec.core.util.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface for encoding and decoding values to and from byte streams.
 *
 * @param <V> the type of value this codec handles
 */
public interface Codec<V> {

  /**
   * Encodes the given value and writes it to the output stream.
   *
   * @param value the value to encode
   * @param output the output stream to write the encoded bytes to
   * @return the encode result containing logical count and bytes written
   * @throws IOException if an I/O error occurs during encoding
   * @throws IllegalArgumentException if the value violates codec constraints (e.g. wrong length)
   * @throws CodecException if an encoding error is detected (e.g. invalid value, field errors)
   */
  EncodeResult encode(V value, OutputStream output) throws IOException;

  /**
   * Decodes a value from the input stream.
   *
   * @param input the input stream to read the encoded bytes from
   * @return the decoded value
   * @throws IOException if an I/O error occurs during decoding
   * @throws CodecException if a decoding error is detected (e.g. invalid data, field errors)
   */
  V decode(InputStream input) throws IOException;

  /**
   * Returns a new codec that applies bidirectional mapping functions to transform between value
   * types.
   *
   * <p>The {@code decoder} function is applied after decoding (base type → mapped type), and the
   * {@code encoder} function is applied before encoding (mapped type → base type).
   *
   * @param <U> the mapped value type
   * @param decoder the function to apply after decoding
   * @param encoder the function to apply before encoding
   * @return a new codec that maps between {@code V} and {@code U}
   */
  default <U> Codec<U> xmap(Function<V, U> decoder, Function<U, V> encoder) {
    return new MappedCodec<>(this, decoder, encoder);
  }

  /**
   * Returns a new codec that maps between types using a {@link Converter}.
   *
   * <p>The converter provides the bidirectional mapping between the base type {@code V} and the
   * mapped type {@code U}. This is useful for reusable or composed conversions.
   *
   * <pre>{@code
   * BiMap<Integer, Color> colors = BiMap.of(
   *     Map.entry(1, Color.RED),
   *     Map.entry(2, Color.GREEN),
   *     Map.entry(3, Color.BLUE)
   * );
   * Codec<Color> colorCodec = Codecs.uint8().xmap(colors);
   * }</pre>
   *
   * @param <U> the mapped value type
   * @param converter the bidirectional conversion between {@code V} and {@code U}
   * @return a new codec that maps between {@code V} and {@code U}
   */
  default <U> Codec<U> xmap(Converter<V, U> converter) {
    Objects.requireNonNull(converter, "converter");
    return xmap(converter::to, converter::from);
  }

  /**
   * Returns a new codec that checks the value on both encode and decode.
   *
   * <p>The check runs before writing on encode and after reading on decode. A failed check throws
   * {@link IllegalArgumentException} on encode and {@link CodecException} on decode.
   *
   * <pre>{@code
   * Codec<Integer> amount = Codecs.uint16().validate(v -> v > 0, "amount must be positive");
   * }</pre>
   *
   * @param check the condition the value must satisfy
   * @param message the error message used when the check fails
   * @return a new codec that validates values passing through it
   * @throws NullPointerException if any argument is null
   */
  default Codec<V> validate(Predicate<V> check, String message) {
    Objects.requireNonNull(check, "check");
    Objects.requireNonNull(message, "message");
    return validate(check, value -> message);
  }

  /**
   * Returns a new codec that checks the value on both encode and decode, deriving the error message
   * from the rejected value.
   *
   * <p>The message function is applied only when the check fails.
   *
   * <pre>{@code
   * Codec<String> type = Codecs.ascii(2).validate("BT"::equals,
   *     actual -> "expected constant [BT] but got [%s]".formatted(actual));
   * }</pre>
   *
   * @param check the condition the value must satisfy
   * @param message produces the error message from the rejected value
   * @return a new codec that validates values passing through it
   * @throws NullPointerException if any argument is null
   */
  default Codec<V> validate(Predicate<V> check, Function<V, String> message) {
    return validate(Validator.of(check, message));
  }

  /**
   * Returns a new codec that applies a {@link Validator} on both encode and decode.
   *
   * <p>A validator pairs the condition with its message, so a check can be named once and shared
   * across the codecs that need it rather than restated at each call site:
   *
   * <pre>{@code
   * static final Validator<Integer> POSITIVE_AMOUNT =
   *     Validator.of(v -> v > 0, "amount must be positive");
   *
   * Codec<Integer> amount = Codecs.uint16().validate(POSITIVE_AMOUNT);
   * Codec<Integer> fee = Codecs.uint32().validate(POSITIVE_AMOUNT.and(UNDER_LIMIT));
   * }</pre>
   *
   * @param validator the check to apply
   * @return a new codec that validates values passing through it
   * @throws NullPointerException if validator is null
   */
  default Codec<V> validate(Validator<V> validator) {
    return new ValidatingCodec<>(this, validator);
  }
}
