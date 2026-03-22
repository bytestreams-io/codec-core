package io.bytestreams.codec.core;

import io.bytestreams.codec.core.util.Pair;
import io.bytestreams.codec.core.util.Triple;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces structured representations of decoded values by walking the codec tree.
 *
 * <p>For built-in codecs, inspection uses package-private access to codec internals. Custom codecs
 * participate by implementing {@link Inspectable}.
 */
public final class Inspector {

  private Inspector() {}

  /**
   * Returns a structured representation of the given value by walking the codec tree.
   *
   * @param codec the codec that produced the value
   * @param value the decoded value to inspect
   * @return a structured representation (Map, List, or scalar)
   */
  public static Object inspect(Codec<?> codec, Object value) {
    // Extension point: custom codecs
    if (codec instanceof Inspectable<?> custom) {
      @SuppressWarnings("unchecked")
      var result = ((Inspectable<Object>) custom).inspect(value);
      return result;
    }
    // Built-in codecs
    if (codec instanceof SequentialObjectCodec<?> seq) {
      return inspectSequential(seq, value);
    }
    if (codec instanceof TaggedObjectCodec<?, ?> tagged) {
      return inspectTagged(tagged, value);
    }
    if (codec instanceof FixedListCodec<?> list) {
      return inspectList(list.itemCodec, (List<?>) value);
    }
    if (codec instanceof StreamListCodec<?> list) {
      return inspectList(list.itemCodec, (List<?>) value);
    }
    if (codec instanceof PairCodec<?, ?> pair) {
      return inspectPair(pair, value);
    }
    if (codec instanceof TripleCodec<?, ?, ?> triple) {
      return inspectTriple(triple, value);
    }
    if (codec instanceof ChoiceCodec<?> choice) {
      return inspectChoice(choice, value);
    }
    if (codec instanceof VariableByteLengthCodec<?> vbl) {
      return inspect(vbl.valueCodec, value);
    }
    if (codec instanceof VariableItemLengthCodec<?> vil) {
      return inspectVariableItemLength(vil, value);
    }
    if (codec instanceof MappedCodec<?, ?> mapped) {
      return inspectMapped(mapped, value);
    }
    if (codec instanceof LazyCodec<?> lazy) {
      return inspect(lazy.codec(), value);
    }
    if (codec instanceof ConstantCodec constant) {
      return constant.expected.clone();
    }
    // Primitive codecs — return as-is
    return value;
  }

  private static <T> Object inspectSequential(SequentialObjectCodec<T> seq, Object value) {
    @SuppressWarnings("unchecked")
    T object = (T) value;
    Map<String, Object> result = new LinkedHashMap<>();
    for (SequentialObjectCodec.FieldCodec<T, ?> field : seq.fields) {
      if (field.presence().test(object)) {
        Object fieldValue = field.get(object);
        result.put(field.name(), inspect(field.codec(), fieldValue));
      }
    }
    return result;
  }

  private static <T extends Tagged<T, K>, K> Object inspectTagged(
      TaggedObjectCodec<T, K> codec, Object value) {
    @SuppressWarnings("unchecked")
    T object = (T) value;
    Map<String, Object> result = new LinkedHashMap<>();
    for (K tag : object.tags()) {
      List<Object> values = object.getAll(tag);
      Codec<?> tagCodec = codec.codecs.getOrDefault(tag, codec.defaultCodec);
      result.put(String.valueOf(tag), values.stream().map(v -> inspect(tagCodec, v)).toList());
    }
    return result;
  }

  private static Object inspectList(Codec<?> itemCodec, List<?> values) {
    return values.stream().map(v -> inspect(itemCodec, v)).toList();
  }

  private static <A, B> Object inspectPair(PairCodec<A, B> pair, Object value) {
    @SuppressWarnings("unchecked")
    Pair<A, B> p = (Pair<A, B>) value;
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("first", inspect(pair.first, p.first()));
    result.put("second", inspect(pair.second, p.second()));
    return result;
  }

  private static <A, B, C> Object inspectTriple(TripleCodec<A, B, C> triple, Object value) {
    @SuppressWarnings("unchecked")
    Triple<A, B, C> t = (Triple<A, B, C>) value;
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("first", inspect(triple.first, t.first()));
    result.put("second", inspect(triple.second, t.second()));
    result.put("third", inspect(triple.third, t.third()));
    return result;
  }

  private static <V> Object inspectChoice(ChoiceCodec<V> choice, Object value) {
    @SuppressWarnings("unchecked")
    Class<? extends V> type = (Class<? extends V>) value.getClass();
    Codec<? extends V> codec = choice.codecs.get(type);
    if (codec == null) {
      return value;
    }
    return inspect(codec, value);
  }

  private static <V> Object inspectVariableItemLength(
      VariableItemLengthCodec<V> vil, Object value) {
    @SuppressWarnings("unchecked")
    V typedValue = (V) value;
    int length = vil.lengthOf.applyAsInt(typedValue);
    Codec<V> inner = vil.codecFactory.apply(length);
    return inspect(inner, value);
  }

  private static <V, U> Object inspectMapped(MappedCodec<V, U> mapped, Object value) {
    @SuppressWarnings("unchecked")
    U typedValue = (U) value;
    V reversed = mapped.encoder.apply(typedValue);
    return inspect(mapped.base, reversed);
  }
}
