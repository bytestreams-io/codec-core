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
UnsignedByteCodec codec = new UnsignedByteCodec();
codec.encode(255, outputStream);

// Decode an unsigned byte
int value = codec.decode(inputStream);
```

## Available Codecs

| Codec | Type | Description |
|-------|------|-------------|
| `BcdStringCodec` | `String` | Fixed-length BCD (Binary-Coded Decimal) string |
| `BinaryCodec` | `byte[]` | Fixed-length binary data |
| `CodePointStringCodec` | `String` | Fixed-length string measured in code points |
| `FormattedStringCodec` | `String` | String with configurable left/right padding |
| `HexStringCodec` | `String` | Fixed-length hexadecimal string |
| `ListCodec<V>` | `List<V>` | List of values with configurable max items |
| `OrderedObjectCodec<T>` | `T` | Object with ordered fields, supports optional fields |
| `TaggedObjectCodec<T>` | `T extends Tagged<T>` | Object with tag-identified fields |
| `StringBigDecimalCodec` | `BigDecimal` | BigDecimal encoded as a string |
| `StringBigIntegerCodec` | `BigInteger` | BigInteger encoded as a string with configurable radix |
| `StringDoubleCodec` | `Double` | Double encoded as a string |
| `StringFloatCodec` | `Float` | Float encoded as a string |
| `StringIntegerCodec` | `Integer` | Integer encoded as a string with configurable radix |
| `StringLongCodec` | `Long` | Long encoded as a string with configurable radix |
| `StringShortCodec` | `Short` | Short encoded as a string with configurable radix |
| `ShortCodec` | `Short` | Signed short (-32768 to 32767) |
| `IntegerCodec` | `Integer` | Signed integer (-2147483648 to 2147483647) |
| `LongCodec` | `Long` | Signed long (-2^63 to 2^63-1) |
| `UnsignedByteCodec` | `Integer` | Unsigned byte (0 - 255) |
| `UnsignedShortCodec` | `Integer` | Unsigned short (0 - 65535) |
| `VariableLengthCodec<V>` | `V` | Variable-length value with length prefix |

## License

[Apache-2.0](LICENSE)
