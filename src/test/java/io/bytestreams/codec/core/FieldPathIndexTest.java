package io.bytestreams.codec.core;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.bytestreams.codec.core.util.Pair;
import io.bytestreams.codec.core.util.Triple;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A failure inside a list should say which item failed, not just which field. */
class FieldPathIndexTest {

  private static final byte[] LF = {0x0A};

  /** Two ASCII characters per item; an item of "99" is rejected. */
  private static Codec<String> item() {
    return Codecs.terminated(Codecs.ascii(2).validate(s -> !s.equals("99"), "item rejected"), LF);
  }

  private static Codec<Holder> holderCodec(Codec<List<String>> items) {
    return Codecs.<Holder>sequential(Holder::new)
        .field("items", items, Holder::getItems, Holder::setItems)
        .build();
  }

  @Test
  void fixed_list_reports_the_failing_index() {
    Codec<Holder> codec = holderCodec(Codecs.listOf(3, item()));
    ByteArrayInputStream input = new ByteArrayInputStream("ab\ncd\n99\n".getBytes(US_ASCII));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageStartingWith("field [items[2]]:");
  }

  @Test
  void stream_list_reports_the_failing_index() {
    Codec<Holder> codec = holderCodec(Codecs.listOf(item()));
    ByteArrayInputStream input = new ByteArrayInputStream("ab\n99\n".getBytes(US_ASCII));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageStartingWith("field [items[1]]:");
  }

  @Test
  void repeat_while_reports_the_failing_index() {
    Codec<Holder> codec =
        holderCodec(Codecs.repeatWhile(Codecs.ascii(1), s -> !"T".equals(s), item()));
    ByteArrayInputStream input = new ByteArrayInputStream("ab\n99\n".getBytes(US_ASCII));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageStartingWith("field [items[1]]:");
  }

  @Test
  void nested_lists_report_every_index() {
    Codec<List<String>> inner = Codecs.listOf(2, item());
    Codec<Outer> codec =
        Codecs.<Outer>sequential(Outer::new)
            .field("groups", Codecs.listOf(2, inner), Outer::getGroups, Outer::setGroups)
            .build();
    ByteArrayInputStream input = new ByteArrayInputStream("ab\ncd\nef\n99\n".getBytes(US_ASCII));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessageStartingWith("field [groups[1][1]]:");
  }

  @Test
  void field_path_is_available_without_the_message() {
    Codec<Holder> codec = holderCodec(Codecs.listOf(2, item()));
    ByteArrayInputStream input = new ByteArrayInputStream("ab\n99\n".getBytes(US_ASCII));

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOfSatisfying(
            CodecException.class, e -> assertThat(e.getFieldPath()).isEqualTo("items[1]"));
  }

  /** An item whose own field rejects "99", so encoding it fails with a field path of its own. */
  private static Codec<Item> itemRecord() {
    return Codecs.<Item>sequential(Item::new)
        .field(
            "code",
            Codecs.ascii(2).validate(c -> !c.equals("99"), "item rejected"),
            Item::getCode,
            Item::setCode)
        .build();
  }

  private static Codec<Records> recordsCodec(Codec<List<Item>> items) {
    return Codecs.<Records>sequential(Records::new)
        .field("items", items, Records::getItems, Records::setItems)
        .build();
  }

  private static List<Item> twoItemsSecondBad() {
    Item ok = new Item();
    ok.setCode("ab");
    Item bad = new Item();
    bad.setCode("99");
    return List.of(ok, bad);
  }

  @Test
  void fixed_list_reports_the_failing_index_on_encode() {
    Codec<Records> codec = recordsCodec(Codecs.listOf(2, itemRecord()));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Records records = new Records();
    records.setItems(twoItemsSecondBad());

    assertThatThrownBy(() -> codec.encode(records, output))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [items[1].code]: item rejected");
  }

  @Test
  void stream_list_reports_the_failing_index_on_encode() {
    Codec<Records> codec = recordsCodec(Codecs.listOf(itemRecord()));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Records records = new Records();
    records.setItems(twoItemsSecondBad());

    assertThatThrownBy(() -> codec.encode(records, output))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [items[1].code]: item rejected");
  }

  @Test
  void repeat_while_reports_the_failing_index_on_encode() {
    Codec<Records> codec =
        recordsCodec(Codecs.repeatWhile(Codecs.ascii(1), s -> true, itemRecord()));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Records records = new Records();
    records.setItems(twoItemsSecondBad());

    assertThatThrownBy(() -> codec.encode(records, output))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [items[1].code]: item rejected");
  }

  @Test
  void pair_reports_which_half_failed() {
    Codec<Integer> ok = Codecs.uint8();
    Codec<Integer> bad = Codecs.uint8().validate(v -> v < 100, "too large");
    Codec<Pair<Integer, Integer>> pair = Codecs.pair(ok, bad);
    Codec<PairHolder> codec =
        Codecs.<PairHolder>sequential(PairHolder::new)
            .field("size", pair, PairHolder::getSize, PairHolder::setSize)
            .build();
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, (byte) 200});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [size.second]: too large");
  }

  @Test
  void triple_reports_which_part_failed() {
    Codec<Integer> ok = Codecs.uint8();
    Codec<Integer> bad = Codecs.uint8().validate(v -> v < 100, "too large");
    Codec<TripleHolder> codec =
        Codecs.<TripleHolder>sequential(TripleHolder::new)
            .field(
                "point", Codecs.triple(ok, ok, bad), TripleHolder::getPoint, TripleHolder::setPoint)
            .build();
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2, (byte) 200});

    assertThatThrownBy(() -> codec.decode(input))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [point.third]: too large");
  }

  @Test
  void pair_reports_which_half_failed_on_encode() {
    Codec<PairRecord> codec =
        Codecs.<PairRecord>sequential(PairRecord::new)
            .field(
                "size",
                Codecs.pair(Codecs.uint8(), itemRecord()),
                PairRecord::getSize,
                PairRecord::setSize)
            .build();
    Item bad = new Item();
    bad.setCode("99");
    PairRecord holder = new PairRecord();
    holder.setSize(new Pair<>(1, bad));

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(holder, output))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [size.second.code]: item rejected");
  }

  @Test
  void triple_reports_which_part_failed_on_encode() {
    Codec<TripleRecord> codec =
        Codecs.<TripleRecord>sequential(TripleRecord::new)
            .field(
                "parts",
                Codecs.triple(Codecs.uint8(), Codecs.uint8(), itemRecord()),
                TripleRecord::getParts,
                TripleRecord::setParts)
            .build();
    Item bad = new Item();
    bad.setCode("99");
    TripleRecord holder = new TripleRecord();
    holder.setParts(new Triple<>(1, 2, bad));

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertThatThrownBy(() -> codec.encode(holder, output))
        .isInstanceOf(CodecException.class)
        .hasMessage("field [parts.third.code]: item rejected");
  }

  static class PairRecord {
    private Pair<Integer, Item> size;

    Pair<Integer, Item> getSize() {
      return size;
    }

    void setSize(Pair<Integer, Item> size) {
      this.size = size;
    }
  }

  static class TripleRecord {
    private Triple<Integer, Integer, Item> parts;

    Triple<Integer, Integer, Item> getParts() {
      return parts;
    }

    void setParts(Triple<Integer, Integer, Item> parts) {
      this.parts = parts;
    }
  }

  static class PairHolder {
    private Pair<Integer, Integer> size;

    Pair<Integer, Integer> getSize() {
      return size;
    }

    void setSize(Pair<Integer, Integer> size) {
      this.size = size;
    }
  }

  static class TripleHolder {
    private Triple<Integer, Integer, Integer> point;

    Triple<Integer, Integer, Integer> getPoint() {
      return point;
    }

    void setPoint(Triple<Integer, Integer, Integer> point) {
      this.point = point;
    }
  }

  static class Item {
    private String code;

    String getCode() {
      return code;
    }

    void setCode(String code) {
      this.code = code;
    }
  }

  static class Records {
    private List<Item> items;

    List<Item> getItems() {
      return items;
    }

    void setItems(List<Item> items) {
      this.items = items;
    }
  }

  static class Holder {
    private List<String> items;

    List<String> getItems() {
      return items;
    }

    void setItems(List<String> items) {
      this.items = items;
    }
  }

  static class Outer {
    private List<List<String>> groups;

    List<List<String>> getGroups() {
      return groups;
    }

    void setGroups(List<List<String>> groups) {
      this.groups = groups;
    }
  }
}
