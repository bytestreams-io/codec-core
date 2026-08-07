package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ValidatingCodecTest {

  @Test
  void validate_decode_rejectsFailingValue() {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("value must be positive");
  }

  @Test
  void validate_decode_passesAcceptedValue() throws IOException {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {7});

    assertThat(codec.decode(input)).isEqualTo(7);
  }

  @Test
  void validate_encode_rejectsFailingValue() {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must be positive");
  }

  @Test
  void validate_encode_writesNothingWhenRejected() {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0, output)).isInstanceOf(IllegalArgumentException.class);

    assertThat(output.toByteArray()).isEmpty();
  }

  @Test
  void validate_encode_passesAcceptedValue() throws IOException {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(7, output);

    assertThat(output.toByteArray()).containsExactly(7);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void validate_messageFunction_decode_describesRejectedValue() {
    Codec<Integer> codec =
        Codecs.uint8().validate(v -> v > 0, v -> "value [%d] must be positive".formatted(v));
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("value [0] must be positive");
  }

  @Test
  void validate_messageFunction_encode_describesRejectedValue() {
    Codec<Integer> codec =
        Codecs.uint8().validate(v -> v > 0, v -> "value [%d] must be positive".formatted(v));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value [0] must be positive");
  }

  @Test
  void validate_messageFunction_notAppliedWhenCheckPasses() throws IOException {
    AtomicInteger calls = new AtomicInteger();
    Codec<Integer> codec =
        Codecs.uint8()
            .validate(
                v -> v > 0,
                v -> {
                  calls.incrementAndGet();
                  return "value must be positive";
                });

    codec.encode(7, new ByteArrayOutputStream());
    codec.decode(new ByteArrayInputStream(new byte[] {7}));

    assertThat(calls).hasValue(0);
  }

  @Test
  void validate_inspect_delegatesToBaseCodec() {
    SequentialObjectCodec<TestFixtures.Inner> inner =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value",
                Codecs.uint16(),
                TestFixtures.Inner::getValue,
                TestFixtures.Inner::setValue)
            .build();
    Codec<TestFixtures.Inner> codec = inner.validate(v -> true, "never fails");
    TestFixtures.Inner value = new TestFixtures.Inner();
    value.setValue(7);

    assertThat(Inspector.inspect(codec, value)).isEqualTo(Map.of("value", 7));
  }

  @Test
  void validate_decode_failureCarriesFieldPath() {
    SequentialObjectCodec<TestFixtures.Inner> codec =
        Codecs.<TestFixtures.Inner>sequential(TestFixtures.Inner::new)
            .field(
                "value",
                Codecs.uint16().validate(v -> v > 0, "value must be positive"),
                TestFixtures.Inner::getValue,
                TestFixtures.Inner::setValue)
            .build();
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0, 0});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [value]: value must be positive");
  }

  @Test
  void validate_rejectsNullCheck() {
    Codec<Integer> codec = Codecs.uint8();

    assertThatThrownBy(() -> codec.validate(null, "message"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("check");
  }

  @Test
  void validate_rejectsNullMessage() {
    Codec<Integer> codec = Codecs.uint8();

    assertThatThrownBy(() -> codec.validate(v -> true, (String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("message");
  }

  @Test
  void validate_rejectsNullMessageFunction() {
    Codec<Integer> codec = Codecs.uint8();

    assertThatThrownBy(() -> codec.validate(v -> true, (Function<Integer, String>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("message");
  }
}
