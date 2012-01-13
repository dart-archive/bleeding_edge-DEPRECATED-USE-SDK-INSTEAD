// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An abstract string representation.
 */
class ByteString implements SourceString {
  final List<int> bytes;
  final int offset;
  final int length;
  int _hashCode;

  ByteString(List<int> this.bytes, int this.offset, int this.length);

  abstract String get charset();

  String toString() => new String.fromCharCodes(
      new Utf8Decoder(bytes, offset, length).decodeRest());

  bool operator ==(other) {
    throw "should be overridden in subclass";
  }

  Iterator<int> iterator() => new Utf8Decoder(bytes, offset, length);

  int hashCode() {
    if (_hashCode === null) {
      _hashCode = computeHashCode();
    }
    return _hashCode;
  }

  int computeHashCode() {
    int code = 1;
    int end = offset + length;
    for (int i = offset; i < end; i++) {
      code += 19 * code + bytes[i];
    }
    return code;
  }

  printOn(StringBuffer sb) {
    sb.add(toString());
  }
}

/**
 * A string that consists purely of 7bit ASCII characters.
 */
class AsciiString extends ByteString {
  final String charset = "ASCII";

  AsciiString(List<int> bytes, int offset, int length)
    : super(bytes, offset, length);

  static AsciiString of(List<int> bytes, int offset, int length) {
    AsciiString string = new AsciiString(bytes, offset, length);
    return string;
  }

  static AsciiString fromString(String string) {
    List<int> bytes = string.charCodes();
    return AsciiString.of(bytes, 0, bytes.length);
  }
}

/**
 * A string that consists of characters that can be encoded as UTF-8.
 */
class Utf8String extends ByteString {
  final String charset = "UTF8";

  Utf8String(List<int> bytes, int offset, int length)
    : super(bytes, offset, length);

  static Utf8String of(List<int> bytes, int offset, int length) {
    return new Utf8String(bytes, offset, length);
  }

  static Utf8String fromString(String string) {
    throw "not implemented yet";
  }
}

/**
 * A ByteString-valued token.
 */
class ByteStringToken extends Token {
  final ByteString value;

  ByteStringToken(PrecedenceInfo info, ByteString this.value, int charOffset)
    : super(info, charOffset);

  String toString() => value.toString();
}
