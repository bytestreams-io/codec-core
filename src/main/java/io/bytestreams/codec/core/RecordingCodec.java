package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * A codec that captures raw bytes during decoding, returning a {@link Recorded} value containing
 * both the decoded value and the original wire bytes.
 *
 * <p>Designed for use as the outermost codec — wrapping the top-level message codec. On decode, the
 * input stream is tee'd so all bytes read by the delegate codec are captured. On encode, the
 * delegate codec encodes {@link Recorded#value()} directly.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Codec<Message> messageCodec = Codecs.<Message>sequential(Message::new)
 *     .field("id", Codecs.uint16(), Message::getId, Message::setId)
 *     .build();
 *
 * RecordingCodec<Message> recording = new RecordingCodec<>(messageCodec);
 * Recorded<Message> result = recording.decode(inputStream);
 * Message msg = result.value();
 * byte[] rawBytes = result.rawBytes();
 * }</pre>
 *
 * @param <T> the type of the decoded value
 */
public class RecordingCodec<T> implements Codec<Recorded<T>>, Inspectable<Recorded<T>> {
  private final Codec<T> delegate;

  public RecordingCodec(Codec<T> delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  /**
   * Creates a new recording codec wrapping the given delegate.
   *
   * @param delegate the codec to wrap
   * @param <T> the decoded value type
   * @return a new recording codec
   */
  public static <T> RecordingCodec<T> of(Codec<T> delegate) {
    return new RecordingCodec<>(delegate);
  }

  @Override
  public EncodeResult encode(Recorded<T> value, OutputStream output) throws IOException {
    return delegate.encode(value.value(), output);
  }

  @Override
  public Recorded<T> decode(InputStream input) throws IOException {
    RecordingInputStream recording = new RecordingInputStream(input);
    T value = delegate.decode(recording);
    return new Recorded<>(value, recording.recordedBytes());
  }

  @Override
  public Object inspect(Recorded<T> value) {
    return Inspector.inspect(delegate, value.value());
  }
}
