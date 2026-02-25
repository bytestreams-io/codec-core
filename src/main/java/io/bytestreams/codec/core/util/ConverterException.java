package io.bytestreams.codec.core.util;

/**
 * Exception thrown when a {@link Converter} conversion fails.
 */
public class ConverterException extends RuntimeException {

  /**
   * Creates a new ConverterException with the given message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public ConverterException(String message, Throwable cause) {
    super(message, cause);
  }
}
