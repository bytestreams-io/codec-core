package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lyang.randomparamsresolver.RandomParametersExtension;
import io.github.lyang.randomparamsresolver.RandomParametersExtension.Randomize;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(RandomParametersExtension.class)
class StreamListCodecTest {

  private static StreamListCodec<String> listCodec(Charset charset, int length) {
    return new StreamListCodec<>(new FixedCodePointStringCodec(length, charset));
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_single_item(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamListCodec<String> codec = listCodec(charset, 5);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(List.of(value), output);

    assertThat(output.toByteArray()).isEqualTo(value.getBytes(charset));
    assertThat(result.length()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(value.getBytes(charset).length);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_multiple_items(String charsetName, @Randomize String value1, @Randomize String value2)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamListCodec<String> codec = listCodec(charset, 5);

    byte[] bytes1 = value1.getBytes(charset);
    byte[] bytes2 = value2.getBytes(charset);
    ByteBuffer expected = ByteBuffer.allocate(bytes1.length + bytes2.length);
    expected.put(bytes1).put(bytes2);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    EncodeResult result = codec.encode(List.of(value1, value2), output);

    assertThat(output.toByteArray()).isEqualTo(expected.array());
    assertThat(result.length()).isEqualTo(2);
    assertThat(result.bytes()).isEqualTo(bytes1.length + bytes2.length);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_empty_list(String charsetName) throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamListCodec<String> codec = listCodec(charset, 5);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(List.of(), output);

    assertThat(output.toByteArray()).isEmpty();
    assertThat(result.length()).isZero();
    assertThat(result.bytes()).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_incompatible_item_length(String charsetName) {
    Charset charset = Charset.forName(charsetName);
    StreamListCodec<String> codec = listCodec(charset, 5);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    List<String> values = List.of("12345", "abc");

    assertThatThrownBy(() -> codec.encode(values, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_single_item(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamListCodec<String> codec = listCodec(charset, 5);
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(charset));

    List<String> decoded = codec.decode(input);

    assertThat(decoded).containsExactly(value);
    assertThat(input.available()).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_multiple_items(String charsetName, @Randomize String value1, @Randomize String value2)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamListCodec<String> codec = listCodec(charset, 5);
    ByteArrayInputStream input = new ByteArrayInputStream((value1 + value2).getBytes(charset));

    List<String> decoded = codec.decode(input);

    assertThat(decoded).containsExactly(value1, value2);
    assertThat(input.available()).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_empty_stream(String charsetName) throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamListCodec<String> codec = listCodec(charset, 5);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    List<String> decoded = codec.decode(input);

    assertThat(decoded).isEmpty();
  }

  @Test
  void decode_insufficient_data() {
    StreamListCodec<String> codec = listCodec(UTF_8, 5);
    ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(UTF_8));

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void roundtrip(String charsetName, @Randomize String value1, @Randomize String value2)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    StreamListCodec<String> codec = listCodec(charset, 5);
    List<String> original = List.of(value1, value2);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);
    List<String> decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded).isEqualTo(original);
  }

  @Test
  void decode_returns_array_list_by_default(@Randomize String value) throws IOException {
    StreamListCodec<String> codec = listCodec(UTF_8, 5);
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(UTF_8));

    List<String> decoded = codec.decode(input);

    assertThat(decoded).isInstanceOf(ArrayList.class);
  }

  @Test
  void decode_with_custom_list_factory(@Randomize String value1, @Randomize String value2)
      throws IOException {
    StreamListCodec<String> codec =
        new StreamListCodec<>(new FixedCodePointStringCodec(5, UTF_8), LinkedList::new);
    ByteArrayInputStream input = new ByteArrayInputStream((value1 + value2).getBytes(UTF_8));

    List<String> decoded = codec.decode(input);

    assertThat(decoded).isInstanceOf(LinkedList.class).containsExactly(value1, value2);
  }

  @Test
  void constructor_null_item_codec() {
    assertThatThrownBy(() -> new StreamListCodec<>(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("itemCodec");
  }

  @Test
  void constructor_null_list_factory() {
    Codec<String> itemCodec = new FixedCodePointStringCodec(5, UTF_8);

    assertThatThrownBy(() -> new StreamListCodec<>(itemCodec, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("listFactory");
  }

  @Test
  void decode_list_factory_returns_null() {
    StreamListCodec<String> codec =
        new StreamListCodec<>(new FixedCodePointStringCodec(5, UTF_8), () -> null);
    ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(UTF_8));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("listFactory.get()");
  }
}
