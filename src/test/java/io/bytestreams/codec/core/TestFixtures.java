package io.bytestreams.codec.core;

/** Shared test helper classes for inspect() tests across codec test files. */
class TestFixtures {

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
  }
}
