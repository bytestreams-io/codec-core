package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepeatWhileCodecTest {

  private static final byte[] LF = {0x0A};

  private static Codec<List<String>> detailLines() {
    return Codecs.repeatWhile(Codecs.ascii(1), "D"::equals, Codecs.terminated(Codecs.ascii(), LF));
  }

  @Test
  void decode_reads_items_while_the_peek_matches() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream("D1\nD2\nT9\n".getBytes(US_ASCII));

    assertThat(detailLines().decode(input)).containsExactly("D1", "D2");
  }

  @Test
  void decode_leaves_the_unmatched_bytes_for_the_next_reader() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream("D1\nT9\n".getBytes(US_ASCII));

    detailLines().decode(input);

    assertThat(new String(input.readAllBytes(), US_ASCII)).isEqualTo("T9\n");
  }

  @Test
  void decode_returns_empty_when_the_first_peek_does_not_match() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream("T9\n".getBytes(US_ASCII));

    assertThat(detailLines().decode(input)).isEmpty();
  }

  @Test
  void decode_stops_at_end_of_stream() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream("D1\nD2\n".getBytes(US_ASCII));

    assertThat(detailLines().decode(input)).containsExactly("D1", "D2");
  }

  @Test
  void decode_stops_when_the_lookahead_cannot_decode() throws IOException {
    Codec<List<String>> codec =
        Codecs.repeatWhile(Codecs.ascii(4), s -> s.startsWith("D"), Codecs.ascii(4));
    ByteArrayInputStream input = new ByteArrayInputStream("DATAxy".getBytes(US_ASCII));

    assertThat(codec.decode(input)).containsExactly("DATA");
    assertThat(new String(input.readAllBytes(), US_ASCII)).isEqualTo("xy");
  }

  @Test
  void decode_requires_a_markable_stream() {
    Codec<List<String>> codec = detailLines();
    InputStream input = new UnmarkableInput("D1\n".getBytes(US_ASCII));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("BufferedInputStream");
  }

  @Test
  void decode_propagates_an_io_failure_during_the_lookahead() {
    Codec<List<String>> codec = detailLines();
    InputStream input = new FailingInput("D1\nD2\n".getBytes(US_ASCII), 4);

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(IOException.class)
        .hasMessage("disk went away");
  }

  @Test
  void decode_propagates_a_lookahead_that_cannot_be_decoded() {
    Codec<List<String>> codec = Codecs.repeatWhile(Codecs.bcdInt(2), i -> i > 0, Codecs.ascii(2));
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {(byte) 0xAB, (byte) 0xCD});

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(CodecException.class);
  }

  @Test
  void decode_reports_a_lookahead_that_outruns_the_limit() {
    Codec<List<byte[]>> codec =
        Codecs.repeatWhile(Codecs.binary(9000), b -> true, Codecs.binary(1));
    InputStream input = new BufferedInputStream(new ByteArrayInputStream(new byte[20_000]));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining("lookahead read more than");
  }

  @Test
  void encode_writes_the_items_and_counts_them() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = detailLines().encode(List.of("D1", "D2"), output);

    assertThat(output.toString(US_ASCII)).isEqualTo("D1\nD2\n");
    assertThat(result.count()).isEqualTo(2);
    assertThat(result.bytes()).isEqualTo(6);
  }

  @Test
  void encode_writes_nothing_for_an_empty_run() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = detailLines().encode(List.of(), output);

    assertThat(output.toByteArray()).isEmpty();
    assertThat(result.count()).isZero();
  }

  @Test
  void inspect_delegates_to_the_item_codec() {
    Codec<List<String>> codec = detailLines();

    assertThat(Inspector.inspect(codec, List.of("D1", "D2"))).isEqualTo(List.of("D1", "D2"));
  }

  @Test
  void rejects_null_peek_codec() {
    Codec<String> item = Codecs.ascii(1);
    assertThatThrownBy(() -> Codecs.repeatWhile(null, s -> true, item))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("peekCodec");
  }

  @Test
  void rejects_null_predicate() {
    Codec<String> item = Codecs.ascii(1);
    assertThatThrownBy(() -> Codecs.repeatWhile(item, null, item))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("accepts");
  }

  @Test
  void rejects_null_item_codec() {
    Codec<String> peek = Codecs.ascii(1);
    assertThatThrownBy(() -> Codecs.repeatWhile(peek, s -> true, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("itemCodec");
  }

  @Test
  void decodes_a_nested_record_grammar() throws IOException {
    Codec<String> recordType = Codecs.ascii(2);

    Codec<Detail> detail =
        Codecs.<Detail>sequential(Detail::new)
            .constant("type", Codecs.ascii(2), "D ")
            .field("amount", Codecs.ascii(), Detail::getAmount, Detail::setAmount)
            .build();

    Codec<Batch> batch =
        Codecs.<Batch>sequential(Batch::new)
            .constant("type", Codecs.terminated(Codecs.ascii(2), LF), "BH")
            .field(
                "details",
                Codecs.repeatWhile(recordType, "D "::equals, Codecs.terminated(detail, LF)),
                Batch::getDetails,
                Batch::setDetails)
            .constant("trailer", Codecs.terminated(Codecs.ascii(2), LF), "BT")
            .build();

    ByteArrayInputStream input =
        new ByteArrayInputStream("BH\nD 100\nD 250\nBT\n".getBytes(US_ASCII));

    Batch decoded = batch.decode(input);

    assertThat(decoded.getDetails()).hasSize(2);
    assertThat(decoded.getDetails().get(0).getAmount()).isEqualTo("100");
    assertThat(decoded.getDetails().get(1).getAmount()).isEqualTo("250");
  }

  @Test
  void a_stray_record_ends_the_run_and_the_trailer_reports_it() {
    Codec<String> recordType = Codecs.ascii(2);
    Codec<Batch> batch =
        Codecs.<Batch>sequential(Batch::new)
            .field(
                "details",
                Codecs.repeatWhile(recordType, "D "::equals, Codecs.terminated(Codecs.ascii(), LF)),
                Batch::getRawDetails,
                Batch::setRawDetails)
            .constant("trailer", Codecs.terminated(Codecs.ascii(2), LF), "BT")
            .build();
    ByteArrayInputStream input = new ByteArrayInputStream("D 1\nXX\nBT\n".getBytes(US_ASCII));

    assertThatThrownBy(() -> batch.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [trailer]: expected constant [BT] but got [XX]");
  }

  static class Detail {
    private String amount;

    String getAmount() {
      return amount;
    }

    void setAmount(String amount) {
      this.amount = amount;
    }
  }

  static class Batch {
    private List<Detail> details;
    private List<String> rawDetails;

    List<Detail> getDetails() {
      return details;
    }

    void setDetails(List<Detail> details) {
      this.details = details;
    }

    List<String> getRawDetails() {
      return rawDetails;
    }

    void setRawDetails(List<String> rawDetails) {
      this.rawDetails = rawDetails;
    }
  }

  /** Fails with a real I/O error after a given number of bytes, as a dropped socket would. */
  static class FailingInput extends FilterInputStream {
    private final int failAfter;
    private int consumed;

    FailingInput(byte[] bytes, int failAfter) {
      super(new BufferedInputStream(new ByteArrayInputStream(bytes)));
      this.failAfter = failAfter;
    }

    @Override
    public int read() throws IOException {
      failIfExhausted();
      consumed++;
      return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      failIfExhausted();
      int n = super.read(b, off, len);
      if (n > 0) {
        consumed += n;
      }
      return n;
    }

    private void failIfExhausted() throws IOException {
      if (consumed >= failAfter) {
        throw new IOException("disk went away");
      }
    }

    @Override
    public boolean markSupported() {
      return true;
    }
  }

  /** Stands in for FileInputStream and socket streams, which refuse mark. */
  static class UnmarkableInput extends FilterInputStream {
    UnmarkableInput(byte[] bytes) {
      super(new ByteArrayInputStream(bytes));
    }

    @Override
    public boolean markSupported() {
      return false;
    }
  }
}
