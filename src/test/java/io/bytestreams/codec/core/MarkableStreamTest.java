package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Composite codecs that peek must leave the stream they pass down able to support {@code mark},
 * otherwise combinators that rewind cannot be nested inside them.
 */
class MarkableStreamTest {

  /** Records whether the stream it was handed supports mark, then consumes one byte. */
  static class MarkProbeCodec implements Codec<Integer> {
    private final List<Boolean> observed = new ArrayList<>();

    @Override
    public EncodeResult encode(Integer value, OutputStream output) throws IOException {
      output.write(value);
      return EncodeResult.ofBytes(1);
    }

    @Override
    public Integer decode(InputStream input) throws IOException {
      observed.add(input.markSupported());
      return input.read();
    }

    List<Boolean> observed() {
      return observed;
    }
  }

  @Test
  void stream_list_passes_a_markable_stream_to_items() throws IOException {
    MarkProbeCodec probe = new MarkProbeCodec();
    Codec<List<Integer>> codec = Codecs.listOf(probe);

    codec.decode(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    assertThat(probe.observed()).containsExactly(true, true, true);
  }

  @Test
  void tagged_passes_a_markable_stream_to_values() throws IOException {
    MarkProbeCodec probe = new MarkProbeCodec();
    Codec<TaggedData<Integer>> codec =
        Codecs.tagged(Codecs.uint8()).tag(1, probe).tag(2, probe).build();

    codec.decode(new ByteArrayInputStream(new byte[] {1, 0x10, 2, 0x20}));

    assertThat(probe.observed()).containsExactly(true, true);
  }

  @Test
  void stream_list_wraps_an_unmarkable_source() throws IOException {
    MarkProbeCodec probe = new MarkProbeCodec();
    Codec<List<Integer>> codec = Codecs.listOf(probe);

    codec.decode(new UnmarkableInput(new byte[] {1, 2}));

    assertThat(probe.observed()).containsExactly(true, true);
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
