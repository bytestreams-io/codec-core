# codec-core

[![Build](https://github.com/bytestreams-io/codec-core/actions/workflows/build.yaml/badge.svg)](https://github.com/bytestreams-io/codec-core/actions/workflows/build.yaml)
[![CodeQL](https://github.com/bytestreams-io/codec-core/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/bytestreams-io/codec-core/actions/workflows/github-code-scanning/codeql)
[![Dependabot Updates](https://github.com/bytestreams-io/codec-core/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/bytestreams-io/codec-core/actions/workflows/dependabot/dependabot-updates)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=io.bytestreams.codec%3Acore&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=io.bytestreams.codec%3Acore)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=io.bytestreams.codec%3Acore&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=io.bytestreams.codec%3Acore)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=io.bytestreams.codec%3Acore&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=io.bytestreams.codec%3Acore)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=io.bytestreams.codec%3Acore&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=io.bytestreams.codec%3Acore)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=io.bytestreams.codec%3Acore&metric=bugs)](https://sonarcloud.io/summary/new_code?id=io.bytestreams.codec%3Acore)
[![codecov](https://codecov.io/gh/bytestreams-io/codec-core/graph/badge.svg)](https://codecov.io/gh/bytestreams-io/codec-core)
[![GitHub License](https://img.shields.io/github/license/bytestreams-io/codec-core)](LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-latest-blue)](https://bytestreams-io.github.io/codec-core/)

Core codec library for encoding and decoding values to and from byte streams.

## Installation

**Maven**

```xml
<dependency>
  <groupId>io.bytestreams.codec</groupId>
  <artifactId>core</artifactId>
  <version>${version}</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.bytestreams.codec:core:${version}'
```

## What is codec-core?

Many binary protocols — financial messages, file formats, network packets — define data as a sequence of fields packed into a byte stream. Each field has a type, a size, and a position. Parsing these protocols by hand means writing repetitive, error-prone code full of offset arithmetic and byte manipulation.

codec-core provides composable **codecs** that handle encoding and decoding for you. A `Codec<V>` knows how to write a value of type `V` to an output stream and read it back from an input stream. Simple codecs handle primitives like integers and strings. Combinators compose simple codecs into complex ones — objects with multiple fields, variable-length lists, discriminated unions, and recursive structures — without any manual byte manipulation.

## Encoding and Decoding

At its simplest, a codec encodes a value to bytes and decodes bytes back to a value.

```java
import io.bytestreams.codec.core.Codecs;

Codec<Integer> codec = Codecs.uint8();

// Encode
EncodeResult result = codec.encode(255, outputStream);
result.count();  // logical count in codec-specific units
result.bytes();  // number of bytes written to the stream

// Decode
int value = codec.decode(inputStream);
```

Every `encode` returns an `EncodeResult` with two values: `count` is the logical size in codec-specific units, and `bytes` is the number of bytes actually written to the stream. For number codecs like `uint8`, the two are the same. They diverge for codecs where the logical unit isn't a byte — BCD counts digits, string codecs count code points, hex codecs count hex digits. This distinction matters when you need to report field lengths in protocol-specific units rather than raw byte counts.

Every codec in the library follows this same interface. The examples below build from simple codecs to complex compositions.

## Error Handling

Codecs signal errors through three exception types:

- **`IOException`** — I/O failures and unexpected end-of-stream (`EOFException`). Thrown when the underlying stream cannot be read or written.
- **`CodecException`** — encoding or decoding errors detected by the codec itself, such as invalid BCD nibbles or malformed data. For nested object codecs, `CodecException` accumulates a field path as it propagates, producing messages like `field [order.customer.name]: End of stream reached`. The path is available via `getFieldPath()`.
- **`IllegalArgumentException`** — constraint violations caught before writing, such as a string with the wrong number of code points or a value out of range.

`CodecException` and `IllegalArgumentException` are unchecked, so you only need to handle them explicitly when you want to recover from bad data. `ConverterException` (a `RuntimeException` in the `util` package) is thrown when a `Converter` conversion fails, for example when `Converters.toInt()` receives a non-numeric string.

## Number Codecs

Numbers are the most fundamental building block. codec-core provides codecs for binary integers, floating-point numbers, BCD, and text-encoded numerics.

### Binary integers

```java
Codec<Integer> unsignedByte = Codecs.uint8();    // 1 byte
Codec<Integer> unsignedShort = Codecs.uint16();   // 2 bytes big-endian
Codec<Long> unsignedInt = Codecs.uint32();        // 4 bytes big-endian
Codec<Short> signedShort = Codecs.int16();        // 2 bytes big-endian
Codec<Integer> signedInt = Codecs.int32();        // 4 bytes big-endian
Codec<Long> signedLong = Codecs.int64();          // 8 bytes big-endian
```

### Floating-point

```java
Codec<Float> floatCodec = Codecs.float32();    // IEEE 754, 4 bytes
Codec<Double> doubleCodec = Codecs.float64();  // IEEE 754, 8 bytes
```

### BCD (Binary Coded Decimal)

Some protocols encode numbers in BCD — each digit occupies a nibble (4 bits), so two digits fit in one byte. This is common in financial and telecom protocols.

```java
Codec<Integer> bcdCodec = Codecs.bcdInt(4);      // 4 digits, 2 bytes
Codec<Long> bcdLongCodec = Codecs.bcdLong(10);   // 10 digits, 5 bytes
```

Because two digits pack into one byte, `count` and `bytes` differ: encoding `42` with `bcdInt(4)` writes 2 bytes (`0x00 0x42`) but returns `count=4` (digits) and `bytes=2`. Odd digit counts are left-padded with a zero nibble.

### ASCII and EBCDIC numerics

Other protocols encode numbers as text — zero-padded decimal strings in ASCII or EBCDIC. These codecs handle the string encoding and numeric parsing in one step.

```java
// ASCII numeric
Codec<Integer> asciiInt = Codecs.asciiInt(4);     // "0042" ↔ 42
Codec<Long> asciiLong = Codecs.asciiLong(10);     // "1234567890" ↔ 1234567890L

// EBCDIC numeric
Codec<Integer> ebcdicInt = Codecs.ebcdicInt(4);   // "0042" in EBCDIC ↔ 42
Codec<Long> ebcdicLong = Codecs.ebcdicLong(10);   // "1234567890" in EBCDIC ↔ 1234567890L
```

## String Codecs

Strings appear in almost every protocol. codec-core supports fixed-length, variable-length, and multiple character encodings.

### Fixed-length strings

When the protocol defines a field as a fixed number of characters, pass the length to the codec factory. The length is always in **code points**, not bytes.

```java
Codec<String> ascii = Codecs.ascii(5);              // 5 ASCII code points
Codec<String> utf8 = Codecs.utf8(5);                // 5 UTF-8 code points
Codec<String> latin1 = Codecs.latin1(10);            // 10 ISO-8859-1 code points
Codec<String> ebcdic = Codecs.ebcdic(10);            // 10 EBCDIC code points
Codec<String> custom = Codecs.ofCharset(charset, 5); // explicit charset
```

For single-byte charsets like ASCII and ISO-8859-1, code points and bytes are the same. For multi-byte charsets like UTF-8, they can differ: encoding `"caf\u00e9"` with `Codecs.utf8(4)` returns `count=4` (code points) but `bytes=5` (because `\u00e9` is 2 bytes in UTF-8).

### Variable-length strings

When the length isn't fixed, omit the length parameter to create a stream codec that reads until EOF. Stream codecs are typically bounded by a `prefixed` wrapper (covered in [Variable-Length Codecs](#variable-length-codecs)).

```java
Codec<String> stream = Codecs.utf8();  // reads all remaining bytes
```

## Hex and Binary Codecs

Some fields contain raw binary data or hex-encoded strings.

```java
// Fixed-length hex string (left-padded with '0' for byte alignment)
Codec<String> hex = Codecs.hex(4);

// Variable-length hex (reads to EOF)
Codec<String> hexStream = Codecs.hex();

// Fixed-length binary data
Codec<byte[]> binary = Codecs.binary(16);

// Constant bytes (magic numbers, version bytes, protocol signatures)
Codec<byte[]> magic = Codecs.constant(new byte[] {0x4D, 0x5A});

// Boolean (1 byte: 0x00 = false, 0x01 = true)
Codec<Boolean> bool = Codecs.bool();
```

## Variable-Length Codecs

Not all fields have a fixed size. Many protocols prepend a length value before variable-length content. codec-core's `prefixed` combinator pairs a length codec with a content codec — it writes the length on encode and reads exactly that many bytes (or items) on decode.

### Prefixed by byte count

The most common pattern: a length prefix specifies how many **bytes** the content occupies.

```java
// 2-byte unsigned length prefix, then variable-length ASCII content
Codec<String> prefixedByBytes = Codecs.prefixed(Codecs.uint16(), Codecs.ascii());
```

### Prefixed by code point or digit count

For string and hex codecs, you can prefix by the number of **code points** or **digits** instead of bytes.

```java
// 2-byte unsigned prefix = number of UTF-8 code points
Codec<String> prefixedByCount = Codecs.utf8(Codecs.uint16());

// 1-byte unsigned prefix = number of hex digits
Codec<String> prefixedHex = Codecs.hex(Codecs.uint8());
```

### LLVAR / LLLVAR style

In ISO 8583 and similar protocols, the length prefix is itself a text-encoded number. Combine `asciiInt` with `prefixed` to get LLVAR and LLLVAR patterns.

```java
// LLVAR — 2-digit ASCII length prefix
Codec<String> llvar = Codecs.prefixed(Codecs.asciiInt(2), Codecs.ascii());

// LLLVAR — 3-digit ASCII length prefix
Codec<String> lllvar = Codecs.prefixed(Codecs.asciiInt(3), Codecs.ascii());
```

## Type Mapping

Sometimes a codec reads the right bytes but produces the wrong type. `xmap` transforms a `Codec<A>` into a `Codec<B>` by supplying functions that convert between `A` and `B`.

### With functions

The first function maps the decoded value (`A` → `B`), and the second maps back for encoding (`B` → `A`).

```java
// UUID from a 36-character ASCII string
Codec<UUID> uuidCodec = Codecs.ascii(36).xmap(UUID::fromString, UUID::toString);

// LocalDate from a 10-character ASCII string (yyyy-MM-dd)
Codec<LocalDate> dateCodec = Codecs.ascii(10).xmap(LocalDate::parse, LocalDate::toString);

// String-encoded integer from a 4-character ASCII field
Codec<Integer> numericCodec = Codecs.ascii(4).xmap(Integer::parseInt, String::valueOf);
```

### With a Converter

The `Converter` interface bundles the forward and reverse functions into a single reusable object. The `Converters` utility class provides common converters like string padding and numeric parsing.

```java
Codec<Integer> numericCodec = Codecs.ascii(4).xmap(Converters.toInt(4));
Codec<Long> longCodec = Codecs.ascii(10).xmap(Converters.toLong(10));
```

### With a BiMap

For finite sets of known values, `BiMap` provides bidirectional lookup. It implements `Converter`, so it works directly with `xmap`.

```java
BiMap<Integer, Color> colors = BiMap.of(
    Map.entry(1, Color.RED),
    Map.entry(2, Color.GREEN),
    Map.entry(3, Color.BLUE)
);
Codec<Color> colorCodec = Codecs.uint8().xmap(colors);
```

## Object Codecs

Real protocols encode structured objects — messages with named fields. codec-core provides two patterns for building object codecs.

### Sequential

`Codecs.sequential()` reads fields in declaration order and maps them to a POJO via getter/setter pairs.

```java
Codec<Message> messageCodec = Codecs.<Message>sequential(Message::new)
    .field("name", Codecs.ascii(20), Message::getName, Message::setName)
    .field("age", Codecs.uint8(), Message::getAge, Message::setAge)
    .build();
```

### Tagged

`Codecs.tagged()` reads fields identified by a tag value rather than position. Each field on the wire is a tag-value pair: the tag is decoded first to determine which codec to use for the value. Fields can appear in any order, may repeat, and are read until EOF.

The target class must implement the `Tagged<T, K>` interface, which provides three methods: `tags()` returns the set of tags present, `getAll(tag)` returns all values for a tag, and `add(tag, value)` appends a value. This allows the codec to iterate tags for encoding and accumulate decoded tag-value pairs.

```java
Codec<MyObject> taggedCodec = Codecs.<MyObject, String>tagged(MyObject::new, Codecs.ascii(4))
    .tag("code", Codecs.uint16())
    .build();
```

### Conditional fields

Fields can be included or excluded based on previously decoded values using a predicate.

```java
Codec<Message> messageCodec = Codecs.<Message>sequential(Message::new)
    .field("type", Codecs.uint8(), Message::getType, Message::setType)
    .field("body", Codecs.ascii(100), Message::getBody, Message::setBody,
           msg -> msg.getType() > 0)  // only present when type > 0
    .build();
```

## List Codecs

When a protocol contains a repeating sequence of identically-typed elements, use a list codec.

```java
// Fixed-length list (exactly 3 items)
Codec<List<String>> fixed = Codecs.listOf(stringCodec, 3);

// Stream list (reads items until EOF)
Codec<List<String>> stream = Codecs.listOf(stringCodec);
```

Stream lists are typically bounded by a `prefixed` wrapper so they don't consume the entire input.

## Composition

Codecs compose naturally — use any codec as a field in an object codec, nest variable-length wrappers, or combine lists with objects.

### Variable-length list inside an object

```java
Codec<List<String>> memberListCodec = Codecs.prefixed(Codecs.uint8(),
    Codecs.listOf(Codecs.ascii(20)));

Codec<Team> teamCodec = Codecs.<Team>sequential(Team::new)
    .field("id", Codecs.int32(), Team::getId, Team::setId)
    .field("name", Codecs.ascii(20), Team::getName, Team::setName)
    .field("members", memberListCodec, Team::getMembers, Team::setMembers)
    .build();
```

## Tuple Codecs

When you need to compose two or three values without defining a full POJO, pair and triple codecs combine heterogeneous codecs into a domain type. Use `.as()` to map directly to your type.

```java
// Pair — two values
Codec<Rectangle> rect = Codecs.pair(Codecs.uint8(), Codecs.uint8())
    .as(Rectangle::new, r -> r.width, r -> r.height);

// Triple — three values
Codec<Color> color = Codecs.triple(Codecs.uint8(), Codecs.uint8(), Codecs.uint8())
    .as(Color::new, c -> c.r, c -> c.g, c -> c.b);
```

## Choice Codecs

Protocols sometimes carry different message types in the same field, distinguished by a tag or type code. A choice codec encodes these discriminated unions — a class tag selects which codec to use.

```java
// Map integer tags to shape classes
BiMap<Integer, Class<? extends Shape>> tags = BiMap.of(
    Map.entry(1, Circle.class),
    Map.entry(2, Rectangle.class));

// Build the choice codec
Codec<Shape> shapeCodec = Codecs.<Shape>choice(Codecs.uint8().xmap(tags))
    .on(Circle.class, circleCodec)
    .on(Rectangle.class, rectangleCodec)
    .build();
```

## Lazy Codecs

Some data structures are recursive — a tree node contains child tree nodes. A lazy codec defers resolution to first use, breaking the circular reference.

```java
Codec<TreeNode>[] holder = new Codec[1];
holder[0] = Codecs.pair(
    Codecs.utf8(10),
    Codecs.prefixed(Codecs.uint8(), Codecs.listOf(Codecs.lazy(() -> holder[0])))
).as(TreeNode::new, n -> n.name, n -> n.children);
```

## Stream Codecs

Codecs created without a length parameter — `Codecs.utf8()`, `Codecs.hex()`, `Codecs.listOf(codec)`, `Codecs.tagged(...)` — are **stream codecs**. They consume all remaining bytes from the input stream, which can silently swallow subsequent fields if used incorrectly.

**Safe usage patterns:**

1. **Wrap with `prefixed()`** to bound the stream by a length prefix
2. **Place as the last field** in a `sequential` codec
3. **Use inside `VariableByteLengthCodec` / `VariableItemLengthCodec`**, which provide bounded sub-streams automatically

```java
// WRONG — name consumes all bytes, age is never read
Codec<Person> broken = Codecs.<Person>sequential(Person::new)
    .field("name", Codecs.utf8(), Person::getName, Person::setName)
    .field("age", Codecs.uint8(), Person::getAge, Person::setAge)
    .build();

// FIX — wrap the stream codec with prefixed()
Codec<Person> fixed = Codecs.<Person>sequential(Person::new)
    .field("name", Codecs.prefixed(Codecs.uint16(), Codecs.utf8()),
           Person::getName, Person::setName)
    .field("age", Codecs.uint8(), Person::getAge, Person::setAge)
    .build();
```

## Available Codecs

| Method | Description |
|--------|-------------|
| `Codecs.uint8()` | Unsigned byte (1 byte) |
| `Codecs.uint16()` | Unsigned short (2 bytes big-endian) |
| `Codecs.uint32()` | Unsigned integer (4 bytes big-endian) |
| `Codecs.int16()` | Signed short (2 bytes big-endian) |
| `Codecs.int32()` | Signed integer (4 bytes big-endian) |
| `Codecs.int64()` | Signed long (8 bytes big-endian) |
| `Codecs.float32()` | IEEE 754 float (4 bytes) |
| `Codecs.float64()` | IEEE 754 double (8 bytes) |
| `Codecs.bcdInt(n)` | BCD-encoded integer (n digits, 1-9) |
| `Codecs.bcdLong(n)` | BCD-encoded long (n digits, 1-18) |
| `Codecs.asciiInt(n)` | ASCII numeric integer (n digits, 1-9) |
| `Codecs.asciiLong(n)` | ASCII numeric long (n digits, 1-18) |
| `Codecs.ebcdicInt(n)` | EBCDIC numeric integer (n digits, 1-9) |
| `Codecs.ebcdicLong(n)` | EBCDIC numeric long (n digits, 1-18) |
| `Codecs.ascii(n)` / `Codecs.ascii()` / `Codecs.ascii(lc)` | US-ASCII string (fixed, stream, or prefixed) |
| `Codecs.utf8(n)` / `Codecs.utf8()` / `Codecs.utf8(lc)` | UTF-8 string (fixed, stream, or prefixed) |
| `Codecs.latin1(n)` / `Codecs.latin1()` / `Codecs.latin1(lc)` | ISO-8859-1 string (fixed, stream, or prefixed) |
| `Codecs.ebcdic(n)` / `Codecs.ebcdic()` / `Codecs.ebcdic(lc)` | EBCDIC (IBM1047) string (fixed, stream, or prefixed) |
| `Codecs.ofCharset(charset, n)` / `Codecs.ofCharset(charset)` / `Codecs.ofCharset(charset, lc)` | String with explicit charset |
| `Codecs.hex(n)` / `Codecs.hex()` / `Codecs.hex(lc)` | Hexadecimal string (fixed, stream, or prefixed) |
| `Codecs.binary(n)` | Fixed-length binary data |
| `Codecs.constant(bytes)` | Constant byte sequence (magic numbers, signatures) |
| `Codecs.bool()` | Boolean (1 byte: 0x00/0x01) |
| `Codecs.listOf(codec, n)` / `Codecs.listOf(codec)` | List (fixed-length or stream) |
| `Codecs.prefixed(lc, vc)` | Variable-length with byte count prefix |
| `Codecs.prefixed(lc, lengthOf, factory)` | Variable-length with item count prefix |
| `Codecs.pair(a, b)` | Pair codec for two sequential values |
| `Codecs.triple(a, b, c)` | Triple codec for three sequential values |
| `Codecs.lazy(supplier)` | Lazy codec for recursive definitions |
| `Codecs.choice(classCodec)` | Discriminated union (choice) codec builder |
| `Codecs.sequential(factory)` | Sequential object codec builder |
| `Codecs.tagged(factory, tagCodec)` | Tagged object codec builder |
| `codec.xmap(decoder, encoder)` / `codec.xmap(converter)` | Bidirectional type mapping |

## Utilities

The `io.bytestreams.codec.core.util` package provides the following utility classes:

| Class | Key Methods | Description |
|-------|-------------|-------------|
| `Converter` | `to`, `from`, `andThen` | Bidirectional conversion interface |
| `ConverterException` | — | Exception thrown when a `Converter` conversion fails |
| `Converters` | `of`, `leftPad`, `rightPad`, `leftFitPad`, `rightFitPad`, `leftEvenPad`, `rightEvenPad`, `toInt`, `toLong` | Converter factories for common string transformations |
| `BiMap` | `of`, `to`, `from` | Immutable bidirectional map implementing `Converter` |
| `Strings` | `padStart`, `padEnd`, `stripStart`, `stripEnd`, `codePointCount`, `hexByteCount` | String padding, stripping, and counting utilities |
| `InputStreams` | `readFully` | Read exactly N bytes from an input stream |
| `Preconditions` | `check` | Validate conditions, throwing `IllegalArgumentException` on failure |
| `Predicates` | `alwaysTrue`, `alwaysFalse` | Common predicate factories |
| `CodePointReader` | `create`, `read` | Read Unicode code points from an input stream using a charset decoder |

## Requirements

- Java 17+

## License

[Apache-2.0](LICENSE)
