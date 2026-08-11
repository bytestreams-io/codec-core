package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.bytestreams.codec.core.util.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ValidatingCodecTest {

  private static final String MUST_BE_POSITIVE = "value must be positive";
  private static final Validator<Integer> POSITIVE = Validator.of(v -> v > 0, MUST_BE_POSITIVE);

  @Test
  void validate_decode_rejects_failing_value() {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("value must be positive");
  }

  @Test
  void validate_decode_passes_accepted_value() throws IOException {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {7});

    assertThat(codec.decode(input)).isEqualTo(7);
  }

  @Test
  void validate_encode_rejects_failing_value() {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must be positive");
  }

  @Test
  void validate_encode_writes_nothing_when_rejected() {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0, output)).isInstanceOf(IllegalArgumentException.class);

    assertThat(output.toByteArray()).isEmpty();
  }

  @Test
  void validate_encode_passes_accepted_value() throws IOException {
    Codec<Integer> codec = Codecs.uint8().validate(v -> v > 0, "value must be positive");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(7, output);

    assertThat(output.toByteArray()).containsExactly(7);
    assertThat(result.bytes()).isEqualTo(1);
  }

  @Test
  void validate_encode_preserves_encode_result() throws IOException {
    Codec<Integer> codec = Codecs.bcdInt(4).validate(v -> v > 0, "value must be positive");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(1234, output);

    assertThat(result.count()).isEqualTo(4);
    assertThat(result.bytes()).isEqualTo(2);
  }

  @Test
  void validate_message_function_decode_describes_rejected_value() {
    Codec<Integer> codec =
        Codecs.uint8().validate(v -> v > 0, "value [%d] must be positive"::formatted);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {0});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("value [0] must be positive");
  }

  @Test
  void validate_message_function_encode_describes_rejected_value() {
    Codec<Integer> codec =
        Codecs.uint8().validate(v -> v > 0, "value [%d] must be positive"::formatted);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value [0] must be positive");
  }

  @Test
  void validate_message_function_not_applied_when_check_passes() throws IOException {
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
  void validate_inspect_delegates_to_base_codec() {
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
  void validate_decode_failure_carries_field_path() {
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
  void validate_shared_validator_decode() {
    Codec<Integer> codec = Codecs.uint8().validate(POSITIVE);
    var input = new ByteArrayInputStream(new byte[] {0});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageContaining(MUST_BE_POSITIVE);
  }

  @Test
  void validate_shared_validator_encode() {
    Codec<Integer> codec = Codecs.uint8().validate(POSITIVE);
    var output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(0, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(MUST_BE_POSITIVE);
  }

  @Test
  void validate_shared_validator_passes() throws IOException {
    Codec<Integer> codec = Codecs.uint8().validate(POSITIVE);
    var output = new ByteArrayOutputStream();

    codec.encode(7, output);

    assertThat(output.toByteArray()).containsExactly(7);
  }

  @Test
  void validate_composed_validator_reports_the_failing_check() {
    // The same POSITIVE instance is reused here and above: pairing check with message once is
    // the point of the type
    Codec<Integer> codec =
        Codecs.uint8().validate(POSITIVE.and(Validator.of(v -> v < 100, "value must be small")));
    var output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(200, output))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("value must be small");
  }

  @Test
  void validate_null_validator() {
    Codec<Integer> codec = Codecs.uint8();
    assertThatThrownBy(() -> codec.validate((Validator<Integer>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("validator");
  }

  @Test
  void validate_null_check() {
    Codec<Integer> codec = Codecs.uint8();

    assertThatThrownBy(() -> codec.validate(null, "message"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("check");
  }

  @Test
  void validate_null_message() {
    Codec<Integer> codec = Codecs.uint8();

    assertThatThrownBy(() -> codec.validate(v -> true, (String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("message");
  }

  @Test
  void validate_null_messageFunction() {
    Codec<Integer> codec = Codecs.uint8();

    assertThatThrownBy(() -> codec.validate(v -> true, (Function<Integer, String>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("message");
  }
}
