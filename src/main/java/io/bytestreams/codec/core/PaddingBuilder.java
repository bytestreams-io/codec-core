package io.bytestreams.codec.core;

/**
 * Abstract builder for configuring padding direction and character for string codecs.
 *
 * <p>Uses the self-type pattern to enable fluent method chaining in subclasses.
 *
 * @param <T> the concrete builder type (self-type for fluent chaining)
 */
abstract class PaddingBuilder<T extends PaddingBuilder<T>> {
  protected char padChar;
  protected boolean padLeft;

  /**
   * Configures left padding with the specified character.
   *
   * @param padChar the character to use for padding
   * @return this builder
   * @throws IllegalArgumentException if padChar is not valid
   */
  @SuppressWarnings("unchecked")
  public T padLeft(char padChar) {
    validatePadChar(padChar);
    this.padChar = padChar;
    this.padLeft = true;
    return (T) this;
  }

  /**
   * Configures right padding with the specified character.
   *
   * @param padChar the character to use for padding
   * @return this builder
   * @throws IllegalArgumentException if padChar is not valid
   */
  @SuppressWarnings("unchecked")
  public T padRight(char padChar) {
    validatePadChar(padChar);
    this.padChar = padChar;
    this.padLeft = false;
    return (T) this;
  }

  /**
   * Validates that the specified character is a valid pad character.
   *
   * @param c the character to validate
   * @throws IllegalArgumentException if the character is not valid
   */
  abstract void validatePadChar(char c);
}
