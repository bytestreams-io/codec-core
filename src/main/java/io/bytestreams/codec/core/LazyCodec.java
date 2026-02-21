package io.bytestreams.codec.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A codec that defers resolution to first use, enabling recursive codec definitions.
 *
 * <p>The supplier is invoked at most once and the result is cached. This codec is thread-safe —
 * multiple threads can safely share the same instance.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Codec<TreeNode>[] holder = new Codec[1];
 * holder[0] = Codecs.pair(
 *     Codecs.utf8(10),
 *     Codecs.prefixed(Codecs.uint8(), Codecs.listOf(Codecs.lazy(() -> holder[0])))
 * ).as(TreeNode::new, n -> n.name, n -> n.children);
 * }</pre>
 *
 * @param <V> the type of value this codec handles
 */
public class LazyCodec<V> implements Codec<V> {
  private final Supplier<Codec<V>> supplier;
  private Codec<V> resolved;

  LazyCodec(Supplier<Codec<V>> supplier) {
    this.supplier = Objects.requireNonNull(supplier, "supplier");
  }

  private synchronized Codec<V> codec() {
    if (resolved == null) {
      resolved = Objects.requireNonNull(supplier.get(), "supplier.get()");
    }
    return resolved;
  }

  @Override
  public EncodeResult encode(V value, OutputStream output) throws IOException {
    return codec().encode(value, output);
  }

  @Override
  public V decode(InputStream input) throws IOException {
    return codec().decode(input);
  }
}
