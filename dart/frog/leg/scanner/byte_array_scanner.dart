// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Scanner that reads from a byte array and creates tokens that points
 * to the same array.
 */
class ByteArrayScanner extends ArrayBasedScanner<ByteString> {
  final List<int> bytes;

  ByteArrayScanner(List<int> this.bytes) : super();

  int nextByte() => byteAt(++byteOffset);

  int peek() => byteAt(byteOffset + 1);

  int byteAt(int index) => (bytes.length > index) ? bytes[index] : -1;

  AsciiString asciiString(int start) {
    return AsciiString.of(bytes, start, byteOffset - start);
  }

  Utf8String utf8String(int start, int offset) {
    return Utf8String.of(bytes, start, byteOffset - start + offset + 1);
  }

  void appendByteStringToken(int kind, ByteString value) {
    // assert(kind != $a || keywords.get(value) == null);
    tail.next = new ByteStringToken(kind, value, tokenStart);
    tail = tail.next;
  }

  int advance() {
    // This method should be equivalent to the one in super. However,
    // this is a *HOT* method and V8 performs better if it is easy to
    // inline.
    int index = ++byteOffset;
    int next = (bytes.length > index) ? bytes[index] : -1;
    return next;
  }
}
