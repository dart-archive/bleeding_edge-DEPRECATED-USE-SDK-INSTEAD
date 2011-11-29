// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class PartialParser extends Parser {
  PartialParser(Listener listener) : super(listener);

  Token parseClassBody(Token token) => skipBlock(token);

  Token parseExpression(Token token) => skipExpression(token);

  Token skipExpression(Token token) {
    while (true) {
      final kind = token.kind;
      if ((token.kind === EOF_TOKEN) || (token.kind === SEMICOLON_TOKEN))
        return token;
      if (token is BeginGroupToken) {
        BeginGroupToken begin = token;
        token = (begin.endGroup !== null) ? begin.endGroup : token;
      }
      token = token.next;
    }
  }

  Token parseFunctionBody(Token token) {
    if (optional(';', token)) {
      return token;
    } else if (optional('=>', token)) {
      token = parseExpression(token.next);
      expectSemicolon(token);
      return token;
    } else {
      return skipBlock(token);
    }
  }
}
