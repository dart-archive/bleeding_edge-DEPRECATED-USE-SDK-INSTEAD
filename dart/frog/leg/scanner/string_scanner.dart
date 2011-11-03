// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Scanner that reads from a String and creates tokens that points to
 * substrings.
 */
class StringScanner extends ArrayBasedScanner<String> {
  final String string;

  StringScanner(String this.string) : super();

  int nextByte() => charAt(++byteOffset);

  int peek() => charAt(byteOffset + 1);

  int charAt(index) => (string.length > index) ? string.charCodeAt(index) : -1;

  String asciiString(int start) => string.substring(start, byteOffset);

  String utf8String(int start, int offset) {
    return string.substring(start, byteOffset + offset + 1);
  }

  void appendByteStringToken(int kind, String value) {
    // assert(kind != $a || keywords.get(value) == null);
    tail.next = new StringToken(kind, value, tokenStart);
    tail = tail.next;
  }
}
