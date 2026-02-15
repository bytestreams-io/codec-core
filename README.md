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
// Encode an unsigned byte
Codec<Integer> codec = NumberCodecs.ofUnsignedByte();
EncodeResult result = codec.encode(255, outputStream);
result.length(); // logical length in codec-specific units
result.bytes();  // number of bytes written to the stream

// Decode an unsigned byte
int value = codec.decode(inputStream);
```

### Binary and Boolean Codecs

```java
// Fixed-length binary data
Codec<byte[]> binary = new BinaryCodec(16);

// Boolean (1 byte: 0x00 = false, 0x01 = true)
Codec<Boolean> bool = new BooleanCodec();
```

### Number Codecs

```java
// Binary integer codec (4 bytes big-endian)
Codec<Integer> intCodec = NumberCodecs.ofInt();

// String integer codec (radix 10)
Codec<Integer> decimalCodec = NumberCodecs.ofInt(stringCodec);

// String integer codec (radix 16)
Codec<Integer> hexCodec = NumberCodecs.ofInt(stringCodec, 16);

// Unsigned byte codec
Codec<Integer> unsignedByteCodec = NumberCodecs.ofUnsignedByte();
```

### String Codecs

```java
// Fixed-length code point string
Codec<String> fixed = StringCodecs.ofCodePoint(5).build();

// Fixed-length with explicit charset
Codec<String> fixedUtf8 = StringCodecs.ofCodePoint(5).charset(UTF_8).build();

// Variable-length (reads to EOF)
Codec<String> stream = StringCodecs.ofCodePoint().charset(UTF_8).build();

// Fixed-length hex: default left-pad with '0'
Codec<String> hex = StringCodecs.ofHex(4).build();

// Right-pad with 'f'
Codec<String> hexPadded = StringCodecs.ofHex(4).padRight('f').build();

// Variable-length hex
Codec<String> hexStream = StringCodecs.ofHex().padRight('f').build();

// Formatted string with delegate
Codec<String> formatted = StringCodecs.ofFormatted(delegate).padRight('0').build();
```

### Object Codecs

```java
// Ordered object codec
Codec<Message> ordered = ObjectCodecs.<Message>ofOrdered(Message::new)
    .field("name", nameCodec, Message::getName, Message::setName)
    .build();

// Tagged object codec
Codec<MyObject> tagged = ObjectCodecs.<MyObject>ofTagged(MyObject::new)
    .tagCodec(StringCodecs.ofCodePoint(4).build())
    .field("code", NumberCodecs.ofUnsignedShort())
    .build();
```

### List Codecs

```java
// Fixed-length list (exactly 3 items)
FixedLengthCodec<List<String>> fixed = ListCodecs.of(stringCodec, 3);

// Stream list (reads items until EOF)
Codec<List<String>> stream = ListCodecs.of(stringCodec);
```

### Variable-Length Codecs

```java
// Variable-length by byte count
VariableByteLengthCodec.Builder llvar = VariableLengthCodecs.ofByteLength(NumberCodecs.ofUnsignedShort());
Codec<String> varString = llvar.of(stringCodec);

// Variable-length by item count
Codec<String> varItems = VariableLengthCodecs.ofItemLength(NumberCodecs.ofUnsignedByte())
    .of(Strings::codePointCount,
        length -> StringCodecs.ofCodePoint(length).build());
```

### Composition

Codecs compose naturally — use any codec as a field in an object codec, or wrap it with
a variable-length prefix.

```java
// Variable-length list inside an ordered object
Codec<List<String>> nameListCodec = VariableLengthCodecs
    .ofItemLength(NumberCodecs.ofUnsignedByte())
    .of(Strings::codePointCount,
        length -> StringCodecs.ofCodePoint(length).build());

Codec<Team> teamCodec = ObjectCodecs.<Team>ofOrdered(Team::new)
    .field("id", NumberCodecs.ofInt(), Team::getId, Team::setId)
    .field("name", StringCodecs.ofCodePoint(20).build(), Team::getName, Team::setName)
    .field("members", nameListCodec, Team::getMembers, Team::setMembers)
    .build();

// Optional field based on a previously decoded field
Codec<Message> messageCodec = ObjectCodecs.<Message>ofOrdered(Message::new)
    .field("type", NumberCodecs.ofUnsignedByte(), Message::getType, Message::setType)
    .field("body", StringCodecs.ofCodePoint(100).build(), Message::getBody, Message::setBody,
           msg -> msg.getType() > 0)  // only present when type > 0
    .build();
```

## Available Codecs

| Facade | Description |
|--------|-------------|
| `NumberCodecs` | Number codecs: binary (`ofInt()`) and string-encoded (`ofInt(stringCodec)`) |
| `StringCodecs` | String codecs: code point, hex, and formatted |
| `ObjectCodecs` | Object codecs: ordered and tagged |
| `ListCodecs` | List codecs: stream (`of(codec)`) and fixed-length (`of(codec, length)`) |
| `VariableLengthCodecs` | Variable-length codecs with byte count or item count prefix |

| Codec | Type | Description |
|-------|------|-------------|
| `BinaryCodec` | `byte[]` | Fixed-length binary data |
| `BooleanCodec` | `Boolean` | Boolean (1 byte, strict 0x00/0x01) |
| `BinaryNumberCodec<V>` | `V extends Number` | Signed/unsigned number as fixed-length big-endian binary |
| `FixedCodePointStringCodec` | `String` | Fixed-length string measured in code points |
| `FixedHexStringCodec` | `String` | Fixed-length hexadecimal string with configurable padding |
| `FixedListCodec<V>` | `List<V>` | Fixed-length list that encodes/decodes exactly N items |
| `FormattedStringCodec` | `String` | String with configurable padding, delegates to another codec |
| `OrderedObjectCodec<T>` | `T` | Object with ordered fields, supports optional fields |
| `StreamCodePointStringCodec` | `String` | Variable-length string measured in code points (reads to EOF) |
| `StreamHexStringCodec` | `String` | Variable-length hexadecimal string with configurable padding |
| `StreamListCodec<V>` | `List<V>` | Variable-length list that reads items until EOF |
| `StringNumberCodec<V>` | `V extends Number` | Number encoded as a string, with configurable radix |
| `TaggedObjectCodec<T>` | `T extends Tagged<T>` | Object with tag-identified fields |
| `VariableByteLengthCodec<V>` | `V` | Variable-length value with byte count prefix |
| `VariableItemLengthCodec<V>` | `V` | Variable-length value with item count prefix |

## Utilities

The `io.bytestreams.codec.core.util` package provides the following utility classes:

| Class | Key Methods | Description |
|-------|-------------|-------------|
| `Strings` | `padStart`, `padEnd`, `trimStart`, `trimEnd`, `codePointCount`, `hexByteCount` | String padding, trimming, and counting |
| `InputStreams` | `readFully` | Read exactly N bytes from an input stream |
| `Preconditions` | `check` | Validate conditions, throwing `IllegalArgumentException` on failure |
| `Predicates` | `alwaysTrue`, `alwaysFalse` | Common predicate factories |
| `CodePointReader` | `create`, `read` | Read Unicode code points from an input stream using a charset decoder |

## License

[Apache-2.0](LICENSE)
