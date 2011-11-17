// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An abstract string representation.
 */
class ByteString {
  final List<int> bytes;
  final int offset;
  final int length;
  int _hashCode;

  ByteString(List<int> this.bytes, int this.offset, int this.length);

  abstract String get charset();

  String toString() {
    var list;
    try {
      list = bytes.getRange(offset, length);
    } catch (var ignored) {
      // An exception may occur when running this on node. This is
      // because [bytes] really is a buffer (or typed array).
      list = new List<int>(length);
      for (int i = 0; i < length; i++) {
        list[i] = bytes[i + offset];
      }
    }
    return new String.fromCharCodes(decodeUtf8(list));
  }

  static int decodeTrailing(int byte) {
    if (byte < 0x80 || 0xBF < byte) {
      return -0xFFFFFFF; // Force bad char.
    } else {
      return byte & 0x3F;
    }
  }

  static List<int> decodeUtf8(List<int> bytes) {
    List<int> result = new List<int>();
    for (int i = 0; i < bytes.length; i++) {
      if (bytes[i] < 0x80) {
        result.add(bytes[i]);
      } else if (bytes[i] < 0xC2) {
        result.add($QUESTION);
      } else if (bytes[i] < 0xE0) {
        int char = (bytes[i++] & 0x1F) << 6;
        char += decodeTrailing(bytes[i]);
        if (char < 0x80) {
          result.add($QUESTION);
        } else {
          result.add(char);
        }
      } else if (bytes[i] < 0xF0) {
        int char = (bytes[i++] & 0x0F) << 6;
        char += decodeTrailing(bytes[i++]);
        char <<= 6;
        char += decodeTrailing(bytes[i]);
        if (char < 0x800 || (0xD800 <= char && char <= 0xDFFF)) {
          result.add($QUESTION);
        } else {
          result.add(char);
        }
      } else if (bytes[i] < 0xF8) {
        int char = (bytes[i++] & 0x07) << 6;
        char += decodeTrailing(bytes[i++]);
        char <<= 6;
        char += decodeTrailing(bytes[i++]);
        char <<= 6;
        char += decodeTrailing(bytes[i]);
        if (char < 0x10000) {
          result.add($QUESTION);
        } else {
          result.add(char);
        }
      } else {
        throw new MalformedInputException('Cannot decode UTF-8 ${bytes[i]}');
      }
    }
    return result;
  }

  bool equals(Object other) {
    if (other is !ByteString) return false;
    ByteString o = other;
    if (charset() != o.charset()) return false;
    if (length != o.length) return false;
    for (int i = 0; i < length; i++) {
      if (bytes[offset + i] != o.bytes[o.offset + i]) {
        return false;
      }
    }
    return true;
  }

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

  ByteStringToken(int kind, ByteString this.value, int charOffset)
    : super(kind, charOffset);

  String toString() => value.toString();
}
