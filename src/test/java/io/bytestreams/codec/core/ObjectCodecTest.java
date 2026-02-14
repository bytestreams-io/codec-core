package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.bytestreams.codec.core.TaggedObjectCodecTest.TestTagged;
import org.junit.jupiter.api.Test;

class ObjectCodecTest {

  @Test
  void ordered_returns_builder() {
    assertThat(ObjectCodec.<Object>ordered(Object::new)).isNotNull();
  }

  @Test
  void tagged_returns_builder() {
    assertThat(ObjectCodec.<TestTagged>tagged(TestTagged::new)).isNotNull();
  }
}
