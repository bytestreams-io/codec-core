package io.bytestreams.codec.core.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * Factory methods for creating {@link Converter} instances.
 */
public final class Converters {
  private static final String LENGTH_MUST_BE_POSITIVE = "length must be positive: %d";

  private Converters() {}

  /**
   * Creates a converter from two functions.
   *
   * @param <V> the source type
   * @param <U> the target type
   * @param to the forward conversion function
   * @param from the reverse conversion function
   * @return a new converter
   */
  public static <V, U> Converter<V, U> of(Function<V, U> to, Function<U, V> from) {
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(from, "from");
    return new Converter<>() {
      @Override
      public U to(V value) {
        return to.apply(value);
      }

      @Override
      public V from(U value) {
        return from.apply(value);
      }
    };
  }

  /**
   * Returns a converter that right-pads strings to a fixed length on {@link Converter#from(Object)
   * from} and strips trailing pad characters on {@link Converter#to(Object) to}.
   *
   * @param padChar the character to pad with
   * @param length the target length
   * @return a right-padding converter
   * @throws IllegalArgumentException if length is not positive
   */
  public static Converter<String, String> rightPad(char padChar, int length) {
    Preconditions.check(length > 0, LENGTH_MUST_BE_POSITIVE, length);
    return new Converter<>() {
      @Override
      public String to(String value) {
        return Strings.stripEnd(value, padChar);
      }

      @Override
      public String from(String value) {
        Preconditions.check(
            value.length() <= length,
            "value length %d exceeds pad length %d",
            value.length(),
            length);
        return Strings.padEnd(value, padChar, length);
      }
    };
  }

  /**
   * Returns a converter that left-pads strings to a fixed length on {@link Converter#from(Object)
   * from} and strips leading pad characters on {@link Converter#to(Object) to}.
   *
   * @param padChar the character to pad with
   * @param length the target length
   * @return a left-padding converter
   * @throws IllegalArgumentException if length is not positive
   */
  public static Converter<String, String> leftPad(char padChar, int length) {
    Preconditions.check(length > 0, LENGTH_MUST_BE_POSITIVE, length);
    return new Converter<>() {
      @Override
      public String to(String value) {
        return Strings.stripStart(value, padChar);
      }

      @Override
      public String from(String value) {
        Preconditions.check(
            value.length() <= length,
            "value length %d exceeds pad length %d",
            value.length(),
            length);
        return Strings.padStart(value, padChar, length);
      }
    };
  }

  /**
   * Returns a converter that right-pads or right-truncates strings to a fixed length on {@link
   * Converter#from(Object) from} and strips trailing pad characters on {@link Converter#to(Object)
   * to}.
   *
   * @param padChar the character to pad with
   * @param length the target length
   * @return a right-fit-padding converter
   * @throws IllegalArgumentException if length is not positive
   */
  public static Converter<String, String> rightFitPad(char padChar, int length) {
    Preconditions.check(length > 0, LENGTH_MUST_BE_POSITIVE, length);
    return new Converter<>() {
      @Override
      public String to(String value) {
        return Strings.stripEnd(value, padChar);
      }

      @Override
      public String from(String value) {
        if (value.length() >= length) {
          return value.substring(0, length);
        }
        return Strings.padEnd(value, padChar, length);
      }
    };
  }

  /**
   * Returns a converter that left-pads or left-truncates strings to a fixed length on {@link
   * Converter#from(Object) from} and strips leading pad characters on {@link Converter#to(Object)
   * to}.
   *
   * @param padChar the character to pad with
   * @param length the target length
   * @return a left-fit-padding converter
   * @throws IllegalArgumentException if length is not positive
   */
  public static Converter<String, String> leftFitPad(char padChar, int length) {
    Preconditions.check(length > 0, LENGTH_MUST_BE_POSITIVE, length);
    return new Converter<>() {
      @Override
      public String to(String value) {
        return Strings.stripStart(value, padChar);
      }

      @Override
      public String from(String value) {
        if (value.length() >= length) {
          return value.substring(value.length() - length);
        }
        return Strings.padStart(value, padChar, length);
      }
    };
  }

  /**
   * Returns a converter that left-pads strings to an even length on {@link Converter#from(Object)
   * from} and strips leading pad characters on {@link Converter#to(Object) to}.
   *
   * @param padChar the character to pad with
   * @return a left-even-padding converter
   */
  public static Converter<String, String> leftEvenPad(char padChar) {
    return new Converter<>() {
      @Override
      public String to(String value) {
        return Strings.stripStart(value, padChar);
      }

      @Override
      public String from(String value) {
        if (value.length() % 2 == 0) {
          return value;
        }
        return padChar + value;
      }
    };
  }

  /**
   * Returns a converter that right-pads strings to an even length on {@link Converter#from(Object)
   * from} and strips trailing pad characters on {@link Converter#to(Object) to}.
   *
   * @param padChar the character to pad with
   * @return a right-even-padding converter
   */
  public static Converter<String, String> rightEvenPad(char padChar) {
    return new Converter<>() {
      @Override
      public String to(String value) {
        return Strings.stripEnd(value, padChar);
      }

      @Override
      public String from(String value) {
        if (value.length() % 2 == 0) {
          return value;
        }
        return value + padChar;
      }
    };
  }

  /**
   * Returns a converter that parses strings to integers on {@link Converter#to(Object) to} and
   * formats integers to zero-padded strings on {@link Converter#from(Object) from}.
   *
   * @param digits the number of digits for zero-padded formatting
   * @return a string-to-integer converter
   * @throws IllegalArgumentException if digits is not positive
   */
  public static Converter<String, Integer> toInt(int digits) {
    Preconditions.check(digits > 0, LENGTH_MUST_BE_POSITIVE, digits);
    return new Converter<>() {
      @Override
      public Integer to(String value) {
        try {
          return Integer.parseInt(value);
        } catch (NumberFormatException e) {
          throw new ConverterException("invalid integer: " + value, e);
        }
      }

      @Override
      public String from(Integer value) {
        return Strings.padStart(Integer.toString(value), '0', digits);
      }
    };
  }

  /**
   * Returns a converter that parses strings to longs on {@link Converter#to(Object) to} and
   * formats longs to zero-padded strings on {@link Converter#from(Object) from}.
   *
   * @param digits the number of digits for zero-padded formatting
   * @return a string-to-long converter
   * @throws IllegalArgumentException if digits is not positive
   */
  public static Converter<String, Long> toLong(int digits) {
    Preconditions.check(digits > 0, LENGTH_MUST_BE_POSITIVE, digits);
    return new Converter<>() {
      @Override
      public Long to(String value) {
        try {
          return Long.parseLong(value);
        } catch (NumberFormatException e) {
          throw new ConverterException("invalid long: " + value, e);
        }
      }

      @Override
      public String from(Long value) {
        return Strings.padStart(Long.toString(value), '0', digits);
      }
    };
  }
}
