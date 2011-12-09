// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class PartialParser extends Parser {
  PartialParser(Listener listener) : super(listener);

  Token parseClassBody(Token token) => skipBlock(token);

  Token fullParseClassBody(Token token) => super.parseClassBody(token);

  Token parseExpression(Token token) => skipExpression(token);

  Token skipExpression(Token token) {
    while (true) {
      final kind = token.kind;
      final value = token.stringValue;
      if ((kind === EOF_TOKEN) ||
          (value === ';') ||
          (value === ',') ||
          (value === ']'))
        return token;
      if ((value !== '<') && (token is BeginGroupToken)) {
        BeginGroupToken begin = token;
        token = (begin.endGroup !== null) ? begin.endGroup : token;
      }
      token = token.next;
    }
  }

  Token parseFunctionBody(Token token, bool isExpression) {
    assert(!isExpression);
    if (optional(';', token)) {
      listener.handleNoFunctionBody(token);
      return token;
    } else if (optional('=>', token)) {
      token = parseExpression(token.next);
      expectSemicolon(token);
      return token;
    } else {
      token = skipBlock(token);
      listener.handleNoFunctionBody(token);
      return token;
    }
  }
}
