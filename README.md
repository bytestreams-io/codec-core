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

```xml
<dependency>
  <groupId>io.bytestreams.codec</groupId>
  <artifactId>core</artifactId>
  <version>VERSION</version>
</dependency>
```

## Usage

```java
import io.bytestreams.codec.core.Codecs;

// Encode an unsigned byte
Codec<Integer> codec = Codecs.uint8();
EncodeResult result = codec.encode(255, outputStream);
result.count();  // logical count in codec-specific units
result.bytes();  // number of bytes written to the stream

// Decode an unsigned byte
int value = codec.decode(inputStream);
```

### Binary and Boolean Codecs

```java
// Fixed-length binary data
Codec<byte[]> binary = Codecs.binary(16);

// Boolean (1 byte: 0x00 = false, 0x01 = true)
Codec<Boolean> bool = Codecs.bool();

// Constant bytes (magic numbers, version bytes, protocol signatures)
Codec<byte[]> magic = Codecs.constant(new byte[] {0x4D, 0x5A});
```

### Number Codecs

```java
// Binary integer codec (4 bytes big-endian)
Codec<Integer> intCodec = Codecs.int32();

// Unsigned byte codec (1 byte)
Codec<Integer> unsignedByteCodec = Codecs.uint8();

// Unsigned short codec (2 bytes big-endian)
Codec<Integer> unsignedShortCodec = Codecs.uint16();

// Unsigned integer codec (4 bytes big-endian)
Codec<Long> unsignedIntCodec = Codecs.uint32();

// Signed short codec (2 bytes big-endian)
Codec<Short> shortCodec = Codecs.int16();

// Signed long codec (8 bytes big-endian)
Codec<Long> longCodec = Codecs.int64();

// IEEE 754 float (4 bytes)
Codec<Float> floatCodec = Codecs.float32();

// IEEE 754 double (8 bytes)
Codec<Double> doubleCodec = Codecs.float64();
```

### String Codecs

```java
// Fixed-length ASCII string (5 code points)
Codec<String> ascii = Codecs.ascii(5);

// Fixed-length UTF-8 string
Codec<String> utf8 = Codecs.utf8(5);

// Variable-length (reads to EOF)
Codec<String> stream = Codecs.utf8();

// Fixed-length with explicit charset
Codec<String> custom = Codecs.ofCharset(charset, 5);

// Fixed-length hex: left-padded with '0' for byte alignment
Codec<String> hex = Codecs.hex(4);

// Variable-length hex (reads to EOF)
Codec<String> hexStream = Codecs.hex();
```

### Type Mapping with xmap

Any codec can be transformed into a codec for a different type using `xmap`.
The first function maps the decoded value, and the second maps back for encoding.

```java
// UUID from a 36-character ASCII string
Codec<UUID> uuidCodec = Codecs.ascii(36).xmap(UUID::fromString, UUID::toString);

// LocalDate from a 10-character ASCII string (yyyy-MM-dd)
Codec<LocalDate> dateCodec = Codecs.ascii(10).xmap(LocalDate::parse, LocalDate::toString);

// String-encoded integer from a 4-character ASCII field
Codec<Integer> numericCodec = Codecs.ascii(4).xmap(Integer::parseInt, String::valueOf);

// Finite set mapping with BiMap (define the mapping once, use bidirectionally)
BiMap<Integer, Color> colors = BiMap.of(
    Map.entry(1, Color.RED),
    Map.entry(2, Color.GREEN),
    Map.entry(3, Color.BLUE)
);
Codec<Color> colorCodec = Codecs.uint8().xmap(colors);
```

### Object Codecs

```java
// Sequence object codec
Codec<Message> message = Codecs.<Message>sequential(Message::new)
    .field("name", nameCodec, Message::getName, Message::setName)
    .build();

// Tagged object codec
Codec<MyObject> tagged = Codecs.<MyObject, String>tagged(MyObject::new, Codecs.ascii(4))
    .tag("code", Codecs.uint16())
    .build();
```

### List Codecs

```java
// Fixed-length list (exactly 3 items)
Codec<List<String>> fixed = Codecs.listOf(stringCodec, 3);

// Stream list (reads items until EOF)
Codec<List<String>> stream = Codecs.listOf(stringCodec);
```

### Variable-Length Codecs

```java
// Variable-length by byte count
Codec<String> prefixedByBytes = Codecs.prefixed(Codecs.uint16(), stringCodec);

// Variable-length by code point count
Codec<String> prefixedByCount = Codecs.prefixed(Codecs.uint8(),
    Strings::codePointCount,
    Codecs::ascii);
```

### Composition

Codecs compose naturally — use any codec as a field in an object codec, or wrap it with
a variable-length prefix.

```java
// Variable-length list inside a sequence object
Codec<List<String>> memberListCodec = Codecs.prefixed(Codecs.uint8(),
    Codecs.listOf(Codecs.ascii(20)));

Codec<Team> teamCodec = Codecs.<Team>sequential(Team::new)
    .field("id", Codecs.int32(), Team::getId, Team::setId)
    .field("name", Codecs.ascii(20), Team::getName, Team::setName)
    .field("members", memberListCodec, Team::getMembers, Team::setMembers)
    .build();

// Optional field based on a previously decoded field
Codec<Message> messageCodec = Codecs.<Message>sequential(Message::new)
    .field("type", Codecs.uint8(), Message::getType, Message::setType)
    .field("body", Codecs.ascii(100), Message::getBody, Message::setBody,
           msg -> msg.getType() > 0)  // only present when type > 0
    .build();
```

### Tuple Codecs

Pair and triple codecs compose two or three heterogeneous codecs into a domain type.
Use `.as()` to map to your type without exposing internal tuple records.

```java
// Pair — two values
Codec<Rectangle> rect = Codecs.pair(Codecs.uint8(), Codecs.uint8())
    .as(Rectangle::new, r -> r.width, r -> r.height);

// Triple — three values
Codec<Color> color = Codecs.triple(Codecs.uint8(), Codecs.uint8(), Codecs.uint8())
    .as(Color::new, c -> c.r, c -> c.g, c -> c.b);
```

### Choice Codec

A choice codec encodes discriminated unions — a class tag selects which codec to use.
Use a `BiMap` with `xmap` to map between tags and class values.

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

### Lazy Codec

A lazy codec defers resolution to first use, enabling recursive codec definitions.

```java
Codec<TreeNode>[] holder = new Codec[1];
holder[0] = Codecs.pair(
    Codecs.utf8(10),
    Codecs.prefixed(Codecs.uint8(), Codecs.listOf(Codecs.lazy(() -> holder[0])))
).as(TreeNode::new, n -> n.name, n -> n.children);
```

### Stream Codecs

Codecs created without a length parameter — `Codecs.utf8()`, `Codecs.hex()`,
`Codecs.listOf(codec)`, `Codecs.tagged(...)` — are **stream codecs**. They consume all
remaining bytes from the input stream, which can silently swallow subsequent fields if
used incorrectly.

**Safe usage patterns:**

1. **Wrap with `prefixed()`** to bound the stream by a length prefix
2. **Place as the last field** in a `sequential` codec
3. **Use inside `VariableByteLengthCodec` / `VariableItemLengthCodec`**, which provide
   bounded sub-streams automatically

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
| `Codecs.ascii(n)` / `Codecs.ascii()` | US-ASCII string (fixed or stream) |
| `Codecs.utf8(n)` / `Codecs.utf8()` | UTF-8 string (fixed or stream) |
| `Codecs.latin1(n)` / `Codecs.latin1()` | ISO-8859-1 string (fixed or stream) |
| `Codecs.ebcdic(n)` / `Codecs.ebcdic()` | EBCDIC (IBM1047) string (fixed or stream) |
| `Codecs.ofCharset(charset, n)` / `Codecs.ofCharset(charset)` | String with explicit charset |
| `Codecs.hex(n)` / `Codecs.hex()` | Hexadecimal string (fixed or stream) |
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
| `codec.xmap(decoder, encoder)` | Bidirectional type mapping |

## Utilities

The `io.bytestreams.codec.core.util` package provides the following utility classes:

| Class | Key Methods | Description |
|-------|-------------|-------------|
| `BiMap` | `of`, `get`, `getKey` | Immutable bidirectional map for finite set mappings |
| `Strings` | `padStart`, `padEnd`, `codePointCount`, `hexByteCount` | String padding and counting utilities |
| `InputStreams` | `readFully` | Read exactly N bytes from an input stream |
| `Preconditions` | `check` | Validate conditions, throwing `IllegalArgumentException` on failure |
| `Predicates` | `alwaysTrue`, `alwaysFalse` | Common predicate factories |
| `CodePointReader` | `create`, `read` | Read Unicode code points from an input stream using a charset decoder |

## License

[Apache-2.0](LICENSE)
