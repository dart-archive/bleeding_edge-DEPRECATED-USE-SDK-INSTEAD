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

  bool isEmpty() => length == 0;
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

  Iterator<int> iterator() => new AsciiStringIterator(bytes);

  SourceString copyWithoutQuotes(int initial, int terminal) {
    return new AsciiString(bytes, offset + initial,
                           length - initial - terminal);
  }


  static AsciiString fromString(String string) {
    List<int> bytes = string.charCodes();
    return AsciiString.of(bytes, 0, bytes.length);
  }
}


class AsciiStringIterator implements Iterator<int> {
  final List<int> bytes;
  int offset;
  final int end;
  AsciiStringIterator(List<int> bytes)
      : this.bytes = bytes, offset = 0, end = bytes.length;
  AsciiStringIterator.range(List<int> bytes, int from, int length)
      : this.bytes = bytes, offset = from, end = from + length;
  bool hasNext() => offset < end;
  int next() => bytes[offset++];
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

  Iterator<int> iterator() => new Utf8Decoder(bytes, 0, length);

  SourceString copyWithoutQuotes(int initial, int terminal) {
    assert(0 <= initial && initial <= 4);
    assert(initial !== 1 || bytes[offset] === $SQ ||
                            bytes[offset] === $DQ);
    assert(initial !== 2 || (bytes[offset] === $AT &&
                             (bytes[offset + 1] === $SQ ||
                              bytes[offset + 1] === $DQ)));
    assert(initial !== 3 || ((bytes[offset] === $SQ ||
                              bytes[offset] === $DQ) &&
                             bytes[offset] === bytes[offset + 1] &&
                             bytes[offset] === bytes[offset + 2]));
    assert(initial !== 4 || (bytes[offset] === $AT &&
                             (bytes[offset + 1] === $SQ ||
                              bytes[offset + 1] === $DQ) &&
                              bytes[offset + 2] === bytes[offset + 1] &&
                              bytes[offset + 3] === bytes[offset + 1]));
    assert(terminal === 0 || terminal === 1 || terminal === 3);
    assert(terminal === 0 || ((bytes[end - 1] == $AT ||
                               bytes[end - 1] === $DQ) &&
                               (terminal === 1 ||
                                bytes[end - 2] === bytes[end - 1] &&
                                bytes[end - 3] === bytes[end - 1])));
    // TODO(lrn): Check that first and last bytes use the same type of quotes.
    return new Utf8String(bytes, offset + initial,
                          length - initial - terminal);
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
