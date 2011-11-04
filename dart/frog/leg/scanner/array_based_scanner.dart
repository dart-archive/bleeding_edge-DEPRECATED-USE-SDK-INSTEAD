// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ArrayBasedScanner<S> extends AbstractScanner<S> {
  int charOffset;
  final Token tokens;
  Token tail;
  int tokenStart;
  int byteOffset;

  ArrayBasedScanner()
    : this.charOffset = -1,
      this.tokenStart = -1,
      this.byteOffset = -1,
      this.tokens = new Token(EOF_TOKEN, -1) {
    this.tail = this.tokens;
  }

  int advance() {
    int next = nextByte();
    charOffset++;
    return next;
  }

  int select(int choice, String yes, String no) {
    int next = advance();
    if (next == choice) {
      appendStringToken(UNKNOWN_TOKEN, yes);
      return advance();
    } else {
      appendStringToken(UNKNOWN_TOKEN, no);
      return next;
    }
  }

  void appendStringToken(int kind, String value) {
    tail.next = new StringToken(kind, value, tokenStart);
    tail = tail.next;
  }

  void appendKeywordToken(Keyword keyword) {
    tail.next = new KeywordToken(keyword, tokenStart);
    tail = tail.next;
  }

  void appendEofToken() {
    tail.next = new Token(EOF_TOKEN, charOffset);
    tail = tail.next;
  }

  void beginToken() {
    tokenStart = charOffset;
  }

  Token firstToken() {
    return tokens.next;
  }

  void addToCharOffset(int offset) {
    charOffset += offset;
  }

  void appendWhiteSpace(int next) {
    // Do nothing, we don't collect white space.
  }
}
