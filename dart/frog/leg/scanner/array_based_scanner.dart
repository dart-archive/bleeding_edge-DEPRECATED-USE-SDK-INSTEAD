// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ArrayBasedScanner<S> extends AbstractScanner<S> {
  int get charOffset() => byteOffset + extraCharOffset;
  final Token tokens;
  Token tail;
  int tokenStart;
  int byteOffset;

  /** Since the input is UTF8, some characters are represented by more
   * than one byte. [extraCharOffset] tracks the difference. */
  int extraCharOffset;
  Link<Token> groupingStack = const EmptyLink<Token>();

  ArrayBasedScanner()
    : this.extraCharOffset = 0,
      this.tokenStart = -1,
      this.byteOffset = -1,
      this.tokens = new Token(EOF_TOKEN, -1) {
    this.tail = this.tokens;
  }

  int advance() {
    int next = nextByte();
    return next;
  }

  int select(int choice, String yes, String no) {
    int next = advance();
    if (next === choice) {
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
    extraCharOffset += offset;
  }

  void appendWhiteSpace(int next) {
    // Do nothing, we don't collect white space.
  }

  void appendBeginGroup(int kind, String value) {
    Token token = new BeginGroupToken(kind, value, tokenStart);
    tail.next = token;
    tail = tail.next;
    while (kind !== LT_TOKEN &&
           !groupingStack.isEmpty() &&
           groupingStack.head.kind === LT_TOKEN) {
      groupingStack = groupingStack.tail;
    }
    groupingStack = groupingStack.prepend(token);
  }

  void appendEndGroup(int kind, String value, int openKind) {
    Token oldTail = tail;
    appendStringToken(kind, value);
    if (groupingStack.isEmpty()) {
      if (openKind === LT_TOKEN) return;
      throw new MalformedInputException('Unmatched $value');
    }
    while (openKind !== LT_TOKEN &&
           !groupingStack.isEmpty() &&
           groupingStack.head.kind === LT_TOKEN) {
      groupingStack = groupingStack.tail;
    }
    if (groupingStack.head.kind !== openKind) {
      if (openKind === LT_TOKEN) return;
      throw new MalformedInputException('Unmatched $value');
    }
    groupingStack.head.endGroup = oldTail.next;
    groupingStack = groupingStack.tail;
  }

  void appendGtGt(int kind, String value) {
    Token oldTail = tail;
    appendStringToken(kind, value);
    if (groupingStack.isEmpty()) return;
    if (groupingStack.head.kind === LT_TOKEN) {
      groupingStack = groupingStack.tail;
    }
    if (groupingStack.isEmpty()) return;
    if (groupingStack.head.kind === LT_TOKEN) {
      groupingStack.head.endGroup = oldTail.next;
      groupingStack = groupingStack.tail;
    }
  }

  void appendGtGtGt(int kind, String value) {
    Token oldTail = tail;
    appendStringToken(kind, value);
    if (groupingStack.isEmpty()) return;
    if (groupingStack.head.kind === LT_TOKEN) {
      groupingStack = groupingStack.tail;
    }
    if (groupingStack.isEmpty()) return;
    if (groupingStack.head.kind === LT_TOKEN) {
      groupingStack = groupingStack.tail;
    }
    if (groupingStack.isEmpty()) return;
    if (groupingStack.head.kind === LT_TOKEN) {
      groupingStack.head.endGroup = oldTail.next;
      groupingStack = groupingStack.tail;
    }
  }
}
