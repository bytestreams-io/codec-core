package io.bytestreams.codec.core;

import java.util.List;
import java.util.stream.Stream;

/**
 * Exception thrown when encoding or decoding fails.
 *
 * <p>This exception accumulates field path information as it propagates through nested codecs,
 * providing clear error messages like: {@code field [order.customer.name]: End of stream reached}
 */
public class CodecException extends RuntimeException {

  private final List<String> fieldPath;

  /**
   * Creates a new CodecException with the given message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public CodecException(String message, Throwable cause) {
    super(message, cause);
    this.fieldPath = List.of();
  }

  private CodecException(String message, Throwable cause, List<String> fieldPath) {
    super(message, cause);
    this.fieldPath = fieldPath;
  }

  /**
   * Creates a new CodecException with the field name prepended to the path.
   *
   * @param fieldName the field name to prepend
   * @return a new CodecException with the updated field path
   */
  public CodecException withField(String fieldName) {
    List<String> newPath = Stream.concat(Stream.of(fieldName), this.fieldPath.stream()).toList();
    return new CodecException(super.getMessage(), getCause(), newPath);
  }

  /**
   * Returns the field path as a dot-separated string.
   *
   * @return the field path, or empty string if no path
   */
  public String getFieldPath() {
    return String.join(".", fieldPath);
  }

  @Override
  public String getMessage() {
    if (fieldPath.isEmpty()) {
      return super.getMessage();
    }
    return "field [%s]: %s".formatted(getFieldPath(), super.getMessage());
  }
}
