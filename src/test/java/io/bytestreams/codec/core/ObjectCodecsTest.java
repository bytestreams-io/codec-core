package io.bytestreams.codec.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.bytestreams.codec.core.TaggedObjectCodecTest.TestTagged;
import org.junit.jupiter.api.Test;

class ObjectCodecsTest {

  @Test
  void ofOrdered_returns_builder() {
    assertThat(ObjectCodecs.<Object>ofOrdered(Object::new)).isNotNull();
  }

  @Test
  void ofTagged_returns_builder() {
    assertThat(ObjectCodecs.<TestTagged>ofTagged(TestTagged::new)).isNotNull();
  }
}
