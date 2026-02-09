package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.EOFException;
import org.junit.jupiter.api.Test;

class CodecExceptionTest {

  @Test
  void message_without_field_path() {
    CodecException e = new CodecException("something went wrong", null);

    assertThat(e.getMessage()).isEqualTo("something went wrong");
    assertThat(e.getFieldPath()).isEmpty();
  }

  @Test
  void message_with_single_field() {
    CodecException e = new CodecException("something went wrong", null).withField("name");

    assertThat(e.getMessage()).isEqualTo("field [name]: something went wrong");
    assertThat(e.getFieldPath()).isEqualTo("name");
  }

  @Test
  void message_with_nested_fields() {
    CodecException e =
        new CodecException("something went wrong", null)
            .withField("name")
            .withField("customer")
            .withField("order");

    assertThat(e.getMessage()).isEqualTo("field [order.customer.name]: something went wrong");
    assertThat(e.getFieldPath()).isEqualTo("order.customer.name");
  }

  @Test
  void preserves_cause() {
    EOFException cause = new EOFException("end of stream");
    CodecException e = new CodecException("read failed", cause).withField("data");

    assertThat(e.getCause()).isSameAs(cause);
  }

  @Test
  void withField_returns_new_instance() {
    CodecException original = new CodecException("error", null);
    CodecException withField = original.withField("name");

    assertThat(withField).isNotSameAs(original);
    assertThat(original.getFieldPath()).isEmpty();
    assertThat(withField.getFieldPath()).isEqualTo("name");
  }
}
