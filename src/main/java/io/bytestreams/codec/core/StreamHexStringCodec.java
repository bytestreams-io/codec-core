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
 * VariableLengthCodec}, where the stream is bounded by the length prefix.
 *
 * <p>Encode pads odd-length values to even (adds leading '0'). Decode always returns an even-length
 * hex string.
 */
public class StreamHexStringCodec implements Codec<String> {
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  @Override
  public EncodeResult encode(String value, OutputStream output) throws IOException {
    String padded = Strings.padStart(value, value.length() + (value.length() % 2), '0');
    byte[] bytes = HEX_FORMAT.parseHex(padded);
    output.write(bytes);
    return new EncodeResult(padded.length(), bytes.length);
  }

  @Override
  public String decode(InputStream input) throws IOException {
    return HEX_FORMAT.formatHex(input.readAllBytes());
  }
}
