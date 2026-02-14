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
BinaryNumberCodec<Integer> codec = BinaryNumberCodec.ofUnsignedByte();
EncodeResult result = codec.encode(255, outputStream);
result.length(); // logical length in codec-specific units
result.bytes();  // number of bytes written to the stream

// Decode an unsigned byte
int value = codec.decode(inputStream);
```

### Code Point String Codecs

```java
// Fixed-length: default platform charset
FixedCodePointStringCodec codec = FixedCodePointStringCodec.builder(5).build();

// Fixed-length with explicit charset
FixedCodePointStringCodec codec = FixedCodePointStringCodec.builder(5).charset(UTF_8).build();

// Fixed-length with custom decoder
FixedCodePointStringCodec codec = FixedCodePointStringCodec.builder(5)
    .decoder(UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE))
    .build();

// Variable-length (reads to EOF)
StreamCodePointStringCodec codec = StreamCodePointStringCodec.builder().charset(UTF_8).build();
```

### Hex/BCD Codecs with Configurable Padding

```java
// Fixed-length hex: default left-pad with '0'
FixedHexStringCodec codec = FixedHexStringCodec.builder(4).build();

// Right-pad with 'f'
FixedHexStringCodec codec = FixedHexStringCodec.builder(4).padRight('f').build();

// Variable-length variant
StreamHexStringCodec codec = StreamHexStringCodec.builder().padRight('f').build();

// FormattedStringCodec: default left-pad with space
FormattedStringCodec codec = FormattedStringCodec.builder(delegate).padRight('0').build();
```

## Available Codecs

| Codec | Type | Description |
|-------|------|-------------|
| `BinaryCodec` | `byte[]` | Fixed-length binary data |
| `BooleanCodec` | `Boolean` | Boolean (1 byte, strict 0x00/0x01) |
| `BinaryNumberCodec<V>` | `V extends Number` | Signed/unsigned number as fixed-length big-endian binary (int, long, short, double, float, unsigned byte/short/int) |
| `FixedCodePointStringCodec` | `String` | Fixed-length string measured in code points |
| `FixedHexStringCodec` | `String` | Fixed-length hexadecimal string with configurable padding |
| `FixedListCodec<V>` | `List<V>` | Fixed-length list that encodes/decodes exactly N items |
| `FormattedStringCodec` | `String` | String with configurable padding, delegates to another codec |
| `OrderedObjectCodec<T>` | `T` | Object with ordered fields, supports optional fields |
| `StreamCodePointStringCodec` | `String` | Variable-length string measured in code points (reads to EOF) |
| `StreamHexStringCodec` | `String` | Variable-length hexadecimal string with configurable padding |
| `StreamListCodec<V>` | `List<V>` | Variable-length list that reads items until EOF |
| `StringNumberCodec<V>` | `V extends Number` | Number encoded as a string, with configurable radix for integer types |
| `TaggedObjectCodec<T>` | `T extends Tagged<T>` | Object with tag-identified fields |
| `VariableByteLengthCodec<V>` | `V` | Variable-length value with byte count prefix |
| `VariableItemLengthCodec<V>` | `V` | Variable-length value with item count prefix |

## License

[Apache-2.0](LICENSE)
