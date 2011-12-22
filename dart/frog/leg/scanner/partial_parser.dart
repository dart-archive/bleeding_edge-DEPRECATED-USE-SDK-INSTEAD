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
      if (!mayParseFunctionExpressions && value === '{')
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
    String value = token.stringValue;
    if (value === ';') {
      // No body.
    } else if (value === '=>') {
      token = parseExpression(token.next);
      expectSemicolon(token);
    } else {
      token = skipBlock(token);
    }
    // There is no "skipped function body event", so we use
    // handleNoFunctionBody instead.
    listener.handleNoFunctionBody(token);
    return token;
  }

  Token parseFormalParameters(Token token) => skipFormals(token);

  Token skipFormals(BeginGroupToken token) {
    listener.beginOptionalFormalParameters(token);
    expect('(', token);
    Token endToken = token.endGroup;
    listener.endFormalParameters(0, token, endToken);
    return endToken.next;
  }
}
