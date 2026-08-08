package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Preconditions;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A codec for a run of values that continues while a lookahead matches.
 *
 * <p>Before each item, the peek codec decodes a value from the stream and the predicate decides
 * whether another item follows. The stream is rewound afterwards, so the item codec — or the next
 * field, once the run ends — sees those bytes again.
 *
 * <p>The run ends at end of stream or at the first value the predicate rejects. Any other failure
 * while reading the lookahead propagates.
 *
 * @param <T> the lookahead value type
 * @param <V> the item type
 */
class RepeatWhileCodec<T, V> implements Codec<List<V>>, Inspectable<List<V>> {
  private static final int READ_LIMIT = 8192;

  private final Codec<T> peekCodec;
  private final Predicate<T> accepts;
  private final Codec<V> itemCodec;

  RepeatWhileCodec(Codec<T> peekCodec, Predicate<T> accepts, Codec<V> itemCodec) {
    this.peekCodec = Objects.requireNonNull(peekCodec, "peekCodec");
    this.accepts = Objects.requireNonNull(accepts, "accepts");
    this.itemCodec = Objects.requireNonNull(itemCodec, "itemCodec");
  }

  /** {@inheritDoc} */
  @Override
  public EncodeResult encode(List<V> values, OutputStream output) throws IOException {
    int totalBytes = 0;
    int index = 0;
    // Iterate rather than index into the list, so encoding stays linear for any List.
    for (V value : values) {
      totalBytes += Parts.encodeAt(index++, itemCodec, value, output).bytes();
    }
    return new EncodeResult(values.size(), totalBytes);
  }

  /** {@inheritDoc} */
  @Override
  public List<V> decode(InputStream input) throws IOException {
    Preconditions.check(
        input.markSupported(), "input stream must support mark; wrap it in a BufferedInputStream");
    List<V> values = new ArrayList<>();
    while (matchesNext(input)) {
      values.add(Parts.decodeAt(values.size(), itemCodec, input));
    }
    return values;
  }

  /** Decodes the lookahead, rewinds, and reports whether another item follows. */
  private boolean matchesNext(InputStream input) throws IOException {
    input.mark(READ_LIMIT);
    T peeked;
    try {
      peeked = peekCodec.decode(input);
    } catch (EOFException e) {
      // End of stream is how a run ends. Every other failure propagates: a discriminator that
      // cannot be decoded is an error, and swallowing IOException here would turn a disk or
      // socket failure into a silently truncated result.
      rewind(input);
      return false;
    }
    rewind(input);
    return accepts.test(peeked);
  }

  private void rewind(InputStream input) throws IOException {
    try {
      input.reset();
    } catch (IOException e) {
      throw new CodecException(
          "lookahead read more than the %d byte limit".formatted(READ_LIMIT), e);
    }
  }

  @Override
  public Object inspect(List<V> values) {
    return values.stream().map(v -> Inspector.inspect(itemCodec, v)).toList();
  }
}
