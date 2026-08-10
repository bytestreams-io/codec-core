package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * A codec for values followed by a terminator byte sequence.
 *
 * <p>Bounds a scope the same way {@link VariableByteLengthCodec} does, but the extent is marked by
 * a trailing sentinel rather than a leading count. On decode, bytes are read up to and including
 * the terminator, and the bytes before it are handed to the value codec as a bounded stream. This
 * is what makes read-until-EOF codecs such as {@link StreamListCodec} and {@code Codecs.ascii()}
 * usable inside it.
 *
 * <p>On encode, the value is buffered so the encoded bytes can be checked for the terminator before
 * anything is written; a value containing its own terminator would produce a stream that cannot be
 * decoded back, so it is rejected with {@link IllegalArgumentException}. There is no escaping.
 *
 * <p>The stream is read one byte at a time because outer codecs continue reading it afterwards, so
 * reading ahead and retaining the excess is not possible. Wrap unbuffered sources such as {@link
 * java.io.FileInputStream} in a {@link java.io.BufferedInputStream}, or every byte costs a system
 * call.
 *
 * <p>Inspection delegates to the value codec.
 *
 * <p>Created via {@link Codecs#terminated(Codec, byte[])}.
 *
 * @param <V> the value type
 */
class TerminatedCodec<V> implements Codec<V>, Inspectable<V> {
  private static final HexFormat HEX = HexFormat.of().withUpperCase();

  private final byte[] terminator;
  private final Codec<V> valueCodec;
  private final Codecs.Termination termination;

  TerminatedCodec(Codec<V> valueCodec, byte[] terminator, Codecs.Termination termination) {
    Objects.requireNonNull(terminator, "terminator");
    Preconditions.check(terminator.length > 0, "terminator must not be empty");
    this.terminator = terminator.clone();
    this.valueCodec = Objects.requireNonNull(valueCodec, "valueCodec");
    this.termination = Objects.requireNonNull(termination, "termination");
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    EncodeResult result = valueCodec.encode(value, buffer);
    byte[] bytes = buffer.toByteArray();
    if (contains(bytes, terminator)) {
      throw new IllegalArgumentException(
          "encoded value contains the terminator [%s]".formatted(HEX.formatHex(terminator)));
    }
    output.write(bytes);
    output.write(terminator);
    return new EncodeResult(result.count(), result.bytes() + terminator.length);
  }

  private static boolean contains(byte[] haystack, byte[] needle) {
    for (int i = 0; i <= haystack.length - needle.length; i++) {
      if (Arrays.equals(haystack, i, i + needle.length, needle, 0, needle.length)) {
        return true;
      }
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public V decode(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    // A trailing window rather than a match counter, so self-overlapping terminators
    // (delimiter AAB against input AAAB) still match.
    byte[] tail = new byte[terminator.length];
    int filled = 0;
    int b;
    while ((b = input.read()) != -1) {
      buffer.write(b);
      if (filled < tail.length) {
        tail[filled++] = (byte) b;
      } else {
        System.arraycopy(tail, 1, tail, 0, tail.length - 1);
        tail[tail.length - 1] = (byte) b;
      }
      if (filled == tail.length && Arrays.equals(tail, terminator)) {
        byte[] all = buffer.toByteArray();
        return decodeValue(all, all.length - terminator.length);
      }
    }
    if (termination == Codecs.Termination.REQUIRED) {
      throw new CodecException(
          "terminator [%s] not found before end of stream".formatted(HEX.formatHex(terminator)),
          null);
    }
    byte[] all = buffer.toByteArray();
    return decodeValue(all, all.length);
  }

  private V decodeValue(byte[] bytes, int length) throws IOException {
    return valueCodec.decode(new ByteArrayInputStream(bytes, 0, length));
  }

  @Override
  public Object inspect(V value) {
    return Inspector.inspect(valueCodec, value);
  }
}
