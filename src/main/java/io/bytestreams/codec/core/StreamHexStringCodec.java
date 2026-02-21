package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HexFormat;

/**
 * A codec for variable-length hexadecimal strings that reads all remaining bytes from the stream.
 *
 * <p>Unlike {@link FixedHexStringCodec}, which reads a fixed number of hex digits, this codec reads
 * all bytes until EOF. This makes it suitable for use as a value codec inside {@link
 * VariableByteLengthCodec}, where the stream is bounded by the length prefix.
 *
 * <p>Encode pads odd-length values to even by left-padding with '0'. Decode always returns an
 * even-length hex string. Encode accepts both uppercase and lowercase hex digits. Decode always
 * returns uppercase.
 *
 * <pre>{@code
 * Codec<String> codec = Codecs.hex();
 * }</pre>
 */
public class StreamHexStringCodec implements Codec<String> {
  private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

  /** Creates a new stream hex string codec. */
  StreamHexStringCodec() {}

  /**
   * {@inheritDoc}
   *
   * <p>Odd-length values are left-padded with '0' to align to byte boundaries.
   */
  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    int paddedLength = Strings.hexByteCount(value) * 2;
    String padded = Strings.padStart(value, '0', paddedLength);
    byte[] bytes = HEX_FORMAT.parseHex(padded);
    output.write(bytes);
    return new EncodeResult(value.length(), bytes.length);
  }

  /** {@inheritDoc} */
  @Override
  public String decode(InputStream input) throws IOException {
    return HEX_FORMAT.formatHex(input.readAllBytes());
  }
}
