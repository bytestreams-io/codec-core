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
class FixedListCodecTest {

  private static FixedListCodec<String> listCodec(Charset charset, int codePoints, int length) {
    return new FixedListCodec<>(Codecs.ofCharset(charset, codePoints), length);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_single_item(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedListCodec<String> codec = listCodec(charset, 5, 1);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(List.of(value), output);

    assertThat(output.toByteArray()).isEqualTo(value.getBytes(charset));
    assertThat(result.count()).isEqualTo(1);
    assertThat(result.bytes()).isEqualTo(value.getBytes(charset).length);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_multiple_items(String charsetName, @Randomize String value1, @Randomize String value2)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedListCodec<String> codec = listCodec(charset, 5, 2);

    byte[] bytes1 = value1.getBytes(charset);
    byte[] bytes2 = value2.getBytes(charset);
    ByteBuffer expected = ByteBuffer.allocate(bytes1.length + bytes2.length);
    expected.put(bytes1).put(bytes2);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    EncodeResult result = codec.encode(List.of(value1, value2), output);

    assertThat(output.toByteArray()).isEqualTo(expected.array());
    assertThat(result.count()).isEqualTo(2);
    assertThat(result.bytes()).isEqualTo(bytes1.length + bytes2.length);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void encode_empty_list(String charsetName) throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedListCodec<String> codec = listCodec(charset, 5, 0);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    EncodeResult result = codec.encode(List.of(), output);

    assertThat(output.toByteArray()).isEmpty();
    assertThat(result.count()).isZero();
    assertThat(result.bytes()).isZero();
  }

  @Test
  void encode_wrong_item_count() {
    FixedListCodec<String> codec = listCodec(UTF_8, 5, 3);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    List<String> values = List.of("12345", "abcde");

    assertThatThrownBy(() -> codec.encode(values, output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_single_item(String charsetName, @Randomize String value) throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedListCodec<String> codec = listCodec(charset, 5, 1);
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
    FixedListCodec<String> codec = listCodec(charset, 5, 2);
    ByteArrayInputStream input = new ByteArrayInputStream((value1 + value2).getBytes(charset));

    List<String> decoded = codec.decode(input);

    assertThat(decoded).containsExactly(value1, value2);
    assertThat(input.available()).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void decode_empty_list(String charsetName) throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedListCodec<String> codec = listCodec(charset, 5, 0);
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    List<String> decoded = codec.decode(input);

    assertThat(decoded).isEmpty();
  }

  @Test
  void decode_leaves_remaining_bytes_unconsumed() throws IOException {
    FixedListCodec<String> codec = listCodec(UTF_8, 5, 1);
    byte[] bytes = "helloworld".getBytes(UTF_8);
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);

    List<String> decoded = codec.decode(input);

    assertThat(decoded).containsExactly("hello");
    assertThat(input.available()).isEqualTo(5);
  }

  @Test
  void decode_insufficient_data() {
    FixedListCodec<String> codec = listCodec(UTF_8, 5, 2);
    ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(UTF_8));

    assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(EOFException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE"})
  void roundtrip(String charsetName, @Randomize String value1, @Randomize String value2)
      throws IOException {
    Charset charset = Charset.forName(charsetName);
    FixedListCodec<String> codec = listCodec(charset, 5, 2);
    List<String> original = List.of(value1, value2);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    codec.encode(original, output);
    List<String> decoded = codec.decode(new ByteArrayInputStream(output.toByteArray()));

    assertThat(decoded).isEqualTo(original);
  }

  @Test
  void decode_returns_array_list_by_default(@Randomize String value) throws IOException {
    FixedListCodec<String> codec = listCodec(UTF_8, 5, 1);
    ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(UTF_8));

    List<String> decoded = codec.decode(input);

    assertThat(decoded).isInstanceOf(ArrayList.class);
  }

  @Test
  void decode_with_custom_list_factory(@Randomize String value1, @Randomize String value2)
      throws IOException {
    FixedListCodec<String> codec =
        new FixedListCodec<>(Codecs.ofCharset(Charset.defaultCharset(), 5), 2, LinkedList::new);
    ByteArrayInputStream input = new ByteArrayInputStream((value1 + value2).getBytes(UTF_8));

    List<String> decoded = codec.decode(input);

    assertThat(decoded).isInstanceOf(LinkedList.class).containsExactly(value1, value2);
  }

  @Test
  void constructor_null_item_codec() {
    assertThatThrownBy(() -> new FixedListCodec<>(null, 1))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("itemCodec");
  }

  @Test
  void constructor_null_list_factory() {
    Codec<String> itemCodec = Codecs.ofCharset(Charset.defaultCharset(), 5);

    assertThatThrownBy(() -> new FixedListCodec<>(itemCodec, 1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("listFactory");
  }

  @Test
  void constructor_negative_length() {
    Codec<String> itemCodec = Codecs.ofCharset(Charset.defaultCharset(), 5);

    assertThatThrownBy(() -> new FixedListCodec<>(itemCodec, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decode_list_factory_returns_null() {
    FixedListCodec<String> codec =
        new FixedListCodec<>(Codecs.ofCharset(Charset.defaultCharset(), 5), 1, () -> null);
    ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(UTF_8));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("listFactory.get()");
  }
}
