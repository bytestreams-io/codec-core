package io.bytestreams.codec.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;

/** Shared test helper classes across codec test files. */
class TestFixtures {

  static AutoCloseable disableLogging(Class<?> clazz) {
    Logger logger = (Logger) LoggerFactory.getLogger(clazz);
    Level original = logger.getLevel();
    logger.setLevel(Level.OFF);
    return () -> logger.setLevel(original);
  }

  static class TestTagged implements Tagged<TestTagged, String> {
    private final Map<String, List<Object>> fields = new LinkedHashMap<>();

    @Override
    public Set<String> tags() {
      return fields.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> List<V> getAll(String tag) {
      return (List<V>) fields.getOrDefault(tag, List.of());
    }

    @Override
    public <V> TestTagged add(String tag, V value) {
      fields.computeIfAbsent(tag, k -> new ArrayList<>()).add(value);
      return this;
    }
  }

  static class Inner {
    private int value;

    int getValue() {
      return value;
    }

    void setValue(int value) {
      this.value = value;
    }
  }

  static class Outer {
    private int id;
    private Inner inner;

    int getId() {
      return id;
    }

    void setId(int id) {
      this.id = id;
    }

    Inner getInner() {
      return inner;
    }

    void setInner(Inner inner) {
      this.inner = inner;
    }

    TestTagged getTags() {
      return tags;
    }

    void setTags(TestTagged tags) {
      this.tags = tags;
    }

    private TestTagged tags;
  }
}
