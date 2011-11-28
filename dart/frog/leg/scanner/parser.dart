// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An event generating parser of Dart programs. This parser expects
 * all tokens in a linked list.
 */
class PartialParser {
  final Listener listener;

  PartialParser(Listener this.listener);

  void parseUnit(Token token) {
    while (token.kind !== EOF_TOKEN) {
      final value = token.stringValue;
      if (value === 'interface') {
        token = parseInterface(token);
      } else if (value === 'class') {
        token = parseClass(token);
      } else if (value === 'typedef') {
        token = parseNamedFunctionAlias(token);
      } else if (value === '#') {
        token = parseLibraryTags(token);
      } else {
        token = parseTopLevelMember(token);
      }
    }
  }

  Token parseInterface(Token token) {
    listener.beginInterface(token);
    token = parseIdentifier(token.next);
    token = parseTypeVariablesOpt(token);
    token = parseSupertypesClauseOpt(token);
    token = parseFactoryClauseOpt(token);
    return parseInterfaceBody(token);
  }

  Token parseInterfaceBody(Token token) {
    token = skipBlock(token);
    listener.endInterface(token);
    return token.next;
  }

  Token parseNamedFunctionAlias(Token token) {
    listener.beginFunctionTypeAlias(token);
    token = parseReturnTypeOpt(token.next);
    token = parseIdentifier(token);
    token = parseTypeVariablesOpt(token);
    token = parseFormalParameters(token);
    listener.endFunctionTypeAlias(token);
    return expect(';', token);
  }

  Token parseReturnTypeOpt(Token token) {
    if (token.stringValue === 'void') {
      listener.handleVoidKeyword(token);
      return token.next;
    } else {
      return parseTypeOpt(token);
    }
  }

  Token parseFormalParameters(Token token) {
    Token begin = token;
    listener.beginFormalParameters(begin);
    expect('(', token);
    int parameterCount = 0;
    if (optional(')', token.next)) {
      listener.endFormalParameters(parameterCount, begin, token.next);
      return token.next.next;
    }
    do {
      listener.beginFormalParameter(token);
      token = parseTypeOpt(token.next);
      token = parseIdentifier(token);
      listener.endFormalParameter(token);
      ++parameterCount;
    } while (optional(',', token));
    listener.endFormalParameters(parameterCount, begin, token);
    return expect(')', token);
  }

  Token parseTypeOpt(Token token) {
    final int kind = token.next.kind;
    if ((kind === LT_TOKEN) ||
        (kind === PERIOD_TOKEN) ||
        kind === IDENTIFIER_TOKEN) {
      return parseType(token);
    } else if (kind === KEYWORD_TOKEN && token.next.value.isPseudo) {
      return parseType(token);
    } else {
      listener.handleNoType(token);
      return token;
    }
  }

  bool isIdentifier(Token token) {
    final kind = token.kind;
    if (kind === IDENTIFIER_TOKEN) return true;
    if (kind === KEYWORD_TOKEN) return token.value.isPseudo;
    return false;
  }

  Token parseSupertypesClauseOpt(Token token) {
    if (optional('extends', token)) {
      do {
        token = parseType(token.next);
      } while (optional(',', token));
    }
    return token;
  }

  Token parseFactoryClauseOpt(Token token) {
    if (optional('factory', token)) {
      return parseType(token.next);
    }
    return token;
  }

  Token skipBlock(Token token) {
    if (!optional('{', token)) {
      return listener.expectedBlock(token);
    }
    BeginGroupToken beginGroupToken = token;
    assert(beginGroupToken.endGroup === null ||
           beginGroupToken.endGroup.kind === $RBRACE);
    return beginGroupToken.endGroup;
  }

  Token skipFormals(BeginGroupToken token) {
    return token.endGroup;
  }

  Token parseClass(Token token) {
    Token begin = token;
    listener.beginClass(token);
    token = parseIdentifier(token.next);
    token = parseTypeVariablesOpt(token);
    Token extendsKeyword;
    if (optional('extends', token)) {
      extendsKeyword = token;
      token = parseType(token.next);
    } else {
      extendsKeyword = null;
      listener.handleNoType(token);
    }
    Token implementsKeyword;
    int interfacesCount = 0;
    if (optional('implements', token)) {
      do {
        token = parseType(token.next);
        ++interfacesCount;
      } while (optional(',', token));
    }
    token = parseNativeClassClauseOpt(token);
    token = parseClassBody(token);
    listener.endClass(interfacesCount, begin, extendsKeyword, implementsKeyword,
                      token);
    return token.next;
  }

  Token parseNativeClassClauseOpt(Token token) {
    if (optional('native', token)) {
      return parseString(token.next);
    }
    return token;
  }

  Token parseString(Token token) {
    if (token.kind === STRING_TOKEN) {
      return token.next;
    } else {
      return listener.expected('string', token);
    }
  }

  Token parseIdentifier(Token token) {
    if (isIdentifier(token)) {
      listener.handleIdentifier(token);
    } else {
      listener.expectedIdentifier(token);
    }
    return token.next;
  }

  Token expect(String string, Token token) {
    if (string !== token.stringValue) {
      if (string === '>') {
        if (token.stringValue === '>>') {
          Token gt = new StringToken(GT_TOKEN, '>', token.charOffset + 1);
          gt.next = token.next;
          return gt;
        } else if (token.stringValue === '>>>') {
          Token gtgt = new StringToken(UNKNOWN_TOKEN, '>>',
                                       token.charOffset + 1);
          gtgt.next = token.next;
          return gtgt;
        }
      }
      return listener.expected(string, token);
    }
    return token.next;
  }

  Token parseTypeVariable(Token token) {
    listener.beginTypeVariable(token);
    token = parseIdentifier(token);
    if (optional('extends', token)) {
      token = parseType(token.next);
    } else {
      listener.handleNoType(token);
    }
    listener.endTypeVariable(token);
    return token;
  }

  bool optional(String value, Token token) => value === token.stringValue;

  Token parseType(Token token) {
    // TODO(ahe): Rename this method to parseTypeOrVar?
    Token begin = token;
    int identifierCount = 1;
    if (isIdentifier(token)) {
      token = parseIdentifier(token);
      while (optional('.', token)) {
        // TODO(ahe): Validate that there are at most two identifiers.
        token = parseIdentifier(token.next);
        ++identifierCount;
      }
    } else if (optional('var', token)) {
      listener.handleVarKeyword(token);
      listener.endType(identifierCount, begin, token);
      return token.next;
    } else {
      token = listener.expectedType(token);
    }
    token = parseTypeArgumentsOpt(token);
    listener.endType(identifierCount, begin, token);
    return token;
  }

  Token parseTypeArgumentsOpt(Token token) {
    return parseStuff(token,
                      (t) => listener.beginTypeArguments(t),
                      (t) => parseType(t),
                      (c, bt, et) => listener.endTypeArguments(c, bt, et),
                      (t) => listener.handleNoTypeArguments(t));
  }

  Token parseTypeVariablesOpt(Token token) {
    if (optional('<', token)) {
      BeginGroupToken beginGroupToken = token;
      token = beginGroupToken.endGroup.next;
    }
    listener.handleNoTypeVariables(token);
    return token;
  }

  Token parseTypeVariablesOptX(Token token) {
    return parseStuff(token,
                      (t) => listener.beginTypeVariables(t),
                      (t) => parseTypeVariable(t),
                      (c, bt, et) => listener.endTypeVariables(c, bt, et),
                      (t) => listener.handleNoTypeVariables(t));
  }

  // TODO(ahe): Clean this up.
  Token parseStuff(Token token, Function beginStuff, Function stuffParser,
                   Function endStuff, Function handleNoStuff) {
    if (optional('<', token)) {
      Token begin = token;
      beginStuff(begin);
      int count = 0;
      do {
        token = stuffParser(token.next);
        ++count;
      } while (optional(',', token));
      endStuff(count, begin, token);
      return expect('>', token);
    }
    handleNoStuff(token);
    return token;
  }

  Token parseClassBody(Token token) => skipBlock(token);

  Token parseTopLevelMember(Token token) {
    Token start = token;
    listener.beginTopLevelMember(token);
    token = skipModifiers(token);
    Token peek = peekAfterType(token);
    while (isIdentifier(peek)) {
      token = peek;
      peek = peekAfterType(token);
    }
    listener.handleNoType(token);
    token = parseIdentifier(token);
    bool isField;
    while (true) {
      // Loop to allow the listener to rewrite the token stream to
      // make the parser happy.
      if (optional('(', token)) {
        isField = false;
        break;
      } else if (optional('=', token) || optional(';', token)) {
        isField = true;
        break;
      } else {
        token = listener.unexpected(token);
      }
    }
    if (isField) {
      if (optional('=', token)) {
        token = parseExpression(token.next);
      }
      expectSemicolon(token);
      listener.endTopLevelField(start, token);
    } else {
      token = skipFormals(token).next;
      token = parseFunctionBody(token);
      listener.endTopLevelMethod(start, token);
    }
    return token.next;
  }

  Token parseFunctionBody(Token token) {
    if (optional(';', token)) {
      return token;
    } else if (optional('=>', token)) {
      token = parseExpression(token.next);
      expectSemicolon(token);
      return token;
    } else if (optional('native', token)) {
      token = token.next;
      if (token.kind === STRING_TOKEN) {
        token = parseString(token);
      }
      expectSemicolon(token);
      return token;
    } else {
      return skipBlock(token);
    }
  }

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

  Token parseExpression(Token token) => skipExpression(token);

  Token parseInitializersOpt(Token token) {
    if (optional(':', token)) {
      return parseInitializers(token);
    } else {
      listener.handleNoInitializers();
      return token;
    }
  }

  Token parseInitializers(Token token) {
    Token begin = token;
    listener.beginInitializers(begin);
    expect(':', token);
    int count = 0;
    do {
      token = parseExpression(token.next);
      ++count;
    } while (optional(',', token));
    listener.endInitializers(count, begin, token);
    return token;
  }

  Token parseLibraryTags(Token token) {
    Token begin = token;
    listener.beginLibraryTag(token);
    token = parseIdentifier(token.next);
    token = expect('(', token);
    token = parseString(token);
    bool hasPrefix = false;
    if (optional(',', token)) {
      hasPrefix = true;
      token = parseIdentifier(token.next);
      token = expect(':', token);
      token = parseString(token);
    }
    token = expect(')', token);
    listener.endLibraryTag(hasPrefix, begin, token);
    return expectSemicolon(token);
  }

  Token expectSemicolon(Token token) {
    return expect(';', token);
  }

  Token skipModifiers(Token token) {
    while (token.kind === KEYWORD_TOKEN) {
      final String value = token.stringValue;
      if (value === 'void') break;
      token = token.next;
    }
    return token;
  }

  Token peekAfterType(Token token) {
    // TODO(ahe): Also handle var?
    if ('void' !== token.stringValue && !isIdentifier(token)) {
      listener.expectedIdentifier(token);
    }
    // We are looking at "identifier ...".
    Token peek = token.next;
    if (peek.kind === PERIOD_TOKEN) {
      if (peek.next.kind === IDENTIFIER_TOKEN) {
        // Look past a library prefix.
        peek = peek.next.next;
      }
    }
    // We are looking at "qualified ...".
    if (peek.kind === LT_TOKEN) {
      // Possibly generic type.
      // We are looking at "qualified '<'".
      BeginGroupToken beginGroupToken = peek;
      Token gtToken = beginGroupToken.endGroup;
      if (gtToken !== null) {
        // We are looking at "qualified '<' ... '>' ...".
        return gtToken.next;
      }
    }
    return peek;
  }
}

class Parser extends PartialParser {
  Parser(NodeListener listener) : super(listener);

  Token parseClassBody(Token token) {
    Token begin = token;
    listener.beginClassBody(token);
    if (!optional('{', token)) {
      return listener.expectedBlock(token);
    }
    token = token.next;
    int count = 0;
    while (!optional('}', token)) {
      token = parseMember(token);
      ++count;
    }
    listener.endClassBody(count, begin, token);
    return token;
  }

  Token parseMember(Token token) {
    Token start = token;
    listener.beginMember(token);
    token = skipModifiers(token);
    Token peek = peekAfterType(token);
    while (isIdentifier(peek)) {
      token = peek;
      peek = peekAfterType(token);
    }
    listener.handleNoType(token);
    token = parseIdentifier(token);
    bool isField;
    while (true) {
      // Loop to allow the listener to rewrite the token stream to
      // make the parser happy.
      if (optional('(', token)) {
        isField = false;
        break;
      } else if (optional('=', token) || optional(';', token)) {
        isField = true;
        break;
      } else {
        token = listener.unexpected(token);
      }
    }
    if (isField) {
      if (optional('=', token)) {
        token = parseExpression(token.next);
      } else {
        listener.handleNoFieldInitializer(token);
      }
      expectSemicolon(token);
      listener.endField(start, token);
    } else {
      token = skipFormals(token).next;
      token = parseInitializersOpt(token);
      if (!optional(';', token)) {
        token = skipBlock(token);
      }
      listener.endMethod(start, token);
    }
    return token.next;
  }

  Token parseFunction(Token token) {
    listener.beginFunction(token);
    token = skipModifiers(token);
    token = parseReturnTypeOpt(token);
    listener.beginFunctionName(token);
    token = parseIdentifier(token);
    listener.endFunctionName(token);
    token = parseFormalParameters(token);
    token = parseInitializersOpt(token);
    return parseFunctionBody(token).next;
  }

  Token parseFunctionBody(Token token) {
    if (optional(';', token)) {
      listener.endFunctionBody(0, null, token);
      return token.next;
    }
    // TODO(ahe): Handle '=>' syntax.
    Token begin = token;
    int statementCount = 0;
    listener.beginFunctionBody(begin);
    token = expect('{', token);
    while (!optional('}', token)) {
      token = parseStatement(token);
      ++statementCount;
    }
    listener.endFunctionBody(statementCount, begin, token);
    expect('}', token);
    return token;
  }

  Token parseStatement(Token token) {
    final value = token.stringValue;
    if (token.kind === IDENTIFIER_TOKEN) {
      return parseExpressionStatementOrDeclaration(token);
    } else if (value === '{') {
      return parseBlock(token);
    } else if (value === 'return') {
      return parseReturnStatement(token);
    } else if (value === 'var') {
      return parseVariablesDeclaration(token);
    } else if (value === 'if') {
      return parseIfStatement(token);
    } else if (value === 'for') {
      return parseForStatement(token);
    } else if (value === 'throw') {
      return parseThrowStatement(token);
    } else if (value === 'void') {
      return parseExpressionStatementOrDeclaration(token);
    } else if (value === 'while') {
      return parseWhileStatement(token);
    } else if (value === 'do') {
      return parseDoWhileStatement(token);
    } else {
      // TODO(ahe): Handle other statements.
      return parseExpressionStatement(token);
    }
  }

  Token parseReturnStatement(Token token) {
    Token begin = token;
    listener.beginReturnStatement(begin);
    assert('return' === token.stringValue);
    token = token.next;
    if (optional(';', token)) {
      listener.endReturnStatement(false, begin, token);
    } else {
      token = parseExpression(token);
      listener.endReturnStatement(true, begin, token);
    }
    return expectSemicolon(token);
  }

  Token peekIdentifierAfterType(Token token) {
    Token peek = peekAfterType(token);
    if (peek !== null && peek.kind === IDENTIFIER_TOKEN) {
      // We are looking at "type identifier".
      return peek;
    } else {
      return null;
    }
  }

  Token parseExpressionStatementOrDeclaration(Token token) {
    Token identifier = peekIdentifierAfterType(token);
    if (identifier !== null) {
      assert(identifier.kind === IDENTIFIER_TOKEN);
      Token afterId = identifier.next;
      int afterIdKind = afterId.kind;
      if (afterIdKind === EQ_TOKEN || afterIdKind === SEMICOLON_TOKEN) {
        // We are looking at "type identifier = ..." or "type identifier;".
        return parseVariablesDeclaration(token);
      } else if (afterIdKind === LPAREN_TOKEN) {
        // We are looking at "type identifier '('".
        BeginGroupToken beginParen = afterId;
        Token endParen = beginParen.endGroup;
        Token afterParens = endParen.next;
        if (optional('{', afterParens) || optional('=>', afterParens)) {
          // We are looking at "type identifier '(' ... ')' =>" or
          // "type identifier '(' ... ')' {".
          return parseFunction(token);
        }
      }
      // Fall-through to expression statement.
    }
    return parseExpressionStatement(token);
  }

  Token parseExpressionStatement(Token token) {
    listener.beginExpressionStatement(token);
    token = parseExpression(token);
    listener.endExpressionStatement(token);
    return expectSemicolon(token);
  }

  Token parseExpression(Token token) {
    token = parseConditionalExpression(token);
    if (isAssignmentOperator(token)) {
      Token operator = token;
      token = parseExpression(token.next);
      listener.handleAssignmentExpression(operator);
    }
    return token;
  }

  bool isAssignmentOperator(Token token) {
    return 2 === getPrecedence(token);
  }

  Token parseConditionalExpression(Token token) {
    token = parseBinaryExpression(token, 4);
    if (optional('?', token)) {
      Token question = token;
      token = parseExpression(token.next);
      Token colon = token;
      token = expect(':', token);
      token = parseExpression(token);
      listener.handleConditionalExpression(question, colon);
    }
    return token;
  }

  Token parseBinaryExpression(Token token, int precedence) {
    assert(precedence >= 4);
    token = parseUnaryExpression(token);
    var tokenLevel = getPrecedence(token);
    for (int level = tokenLevel; level >= precedence; --level) {
      while (tokenLevel === level) {
        Token operator = token;
        token = parseBinaryExpression(token.next, level + 1);
        listener.handleBinaryExpression(operator);
        tokenLevel = getPrecedence(token);
      }
    }
    return token;
  }

  int getPrecedence(Token token) {
    if (token === null) return 0;
    // TODO(ahe): Find a better way to represent this.
    var value = token.stringValue;
    if (value === null) return 0;
    if (value === '(') return 0;
    if (value === ')') return 0;
    if (value === '%=') return 2;
    if (value === '&=') return 2;
    if (value === '*=') return 2;
    if (value === '+=') return 2;
    if (value === '-=') return 2;
    if (value === '/=') return 2;
    if (value === '<<=') return 2;
    if (value === '=') return 2;
    if (value === '>>=') return 2;
    if (value === '>>>=') return 2;
    if (value === '^=') return 2;
    if (value === '|=') return 2;
    if (value === '~/=') return 2;
    if (value === '?') return 3;
    if (value === '||') return 4;
    if (value === '&&') return 5;
    if (value === '|') return 6;
    if (value === '^') return 7;
    if (value === '&') return 8;
    if (value === '!=') return 9;
    if (value === '!==') return 9;
    if (value === '==') return 9;
    if (value === '===') return 9;
    if (value === '<') return 10;
    if (value === '<=') return 10;
    if (value === '>') return 10;
    if (value === '>=') return 10;
    if (value === 'is') return 10;
    if (value === '<<') return 11;
    if (value === '>>') return 11;
    if (value === '>>>') return 11;
    if (value === '+') return 12;
    if (value === '-') return 12;
    if (value === '%') return 13;
    if (value === '*') return 13;
    if (value === '/') return 13;
    if (value === '~/') return 13;
    if (value === '.') return 14; // TODO(ahe): Remove this line.
    return 0;
  }

  Token parseUnaryExpression(Token token) {
    String value = token.stringValue;
    // Prefix:
    if ((value === '!') ||
        (value === '+') || // TODO(ahe): Being removed from specification.
        (value === '-') ||
        (value === '~')) {
      Token operator = token;
      token = token.next;
      token = parseUnaryExpression(token);
      listener.handleUnaryPrefixExpression(operator);
    } else if ((value === '++') || value === '--') {
      // TODO(ahe): Validate this is used correctly.
      Token operator = token;
      token = token.next;
      token = parseUnaryExpression(token);
      listener.handleUnaryPrefixAssignmentExpression(operator);
    } else {
      token = parsePrimary(token);
      value = token.stringValue;
      // Postfix:
      if ((value === '++') || (value === '--')) {
        // TODO(ahe): Validate this is used correctly.
        listener.handleUnaryPostfixAssignmentExpression(token);
        token = token.next;
      }
    }
    return token;
  }

  Token parsePrimary(Token token) {
    // TODO(ahe): Handle other expressions.
    final kind = token.kind;
    if (kind === IDENTIFIER_TOKEN) {
      return parseSend(token);
    } else if (kind === INT_TOKEN) {
      return parseLiteralInt(token);
    } else if (kind === DOUBLE_TOKEN) {
      return parseLiteralDouble(token);
    } else if (kind === STRING_TOKEN) {
      return parseLiteralString(token);
    } else if (kind === KEYWORD_TOKEN) { {
        final value = token.stringValue;
        if ((value === 'true') || (value === 'false')) {
          return parseLiteralBool(token);
        } else if (value === 'null') {
          return parseLiteralNull(token);
        } else if (value === 'this') {
          return parseThisExpression(token);
        } else if (value === 'super') {
          return parseSuperExpression(token);
        } else {
          listener.unexpected(token);
          throw 'not yet implemented';
        }
      }
    } else if (kind === LPAREN_TOKEN) {
      return parseParenthesizedExpression(token);
    } else {
      listener.unexpected(token);
      throw 'not yet implemented';
    }
  }

  Token parseParenthesizedExpression(Token token) {
    BeginGroupToken begin = token;
    token = expect('(', token);
    token = parseExpression(token);
    assert(begin.endGroup === token);
    listener.handleParenthesizedExpression(begin);
    return expect(')', token);
  }

  Token parseThisExpression(Token token) {
    listener.handleThisExpression(token);
    token = token.next;
    if (optional('(', token)) {
      // Constructor forwarding.
      token = parseArgumentsOpt(token);
      listener.endSend(token);
    }
    return token;
  }

  Token parseSuperExpression(Token token) {
    listener.handleSuperExpression(token);
    token = token.next;
    if (optional('(', token)) {
      // Super constructor.
      token = parseArgumentsOpt(token);
      listener.endSend(token);
    }
    return token;
  }

  Token parseLiteralInt(Token token) {
    listener.handleLiteralInt(token);
    return token.next;
  }

  Token parseLiteralDouble(Token token) {
    listener.handleLiteralDouble(token);
    return token.next;
  }

  Token parseLiteralString(Token token) {
    listener.handleLiteralString(token);
    return token.next;
  }

  Token parseLiteralBool(Token token) {
    listener.handleLiteralBool(token);
    return token.next;
  }

  Token parseLiteralNull(Token token) {
    listener.handleLiteralNull(token);
    return token.next;
  }

  Token parseSend(Token token) {
    listener.beginSend(token);
    token = parseIdentifier(token);
    token = parseArgumentsOpt(token);
    listener.endSend(token);
    return token;
  }

  Token parseArgumentsOpt(Token token) {
    if (!optional('(', token)) {
      listener.handleNoArguments(token);
      return token;
    } else {
      return parseArguments(token);
    }
  }

  Token parseArguments(Token token) {
    Token begin = token;
    listener.beginArguments(begin);
    assert('(' === token.stringValue);
    int argumentCount = 0;
    if (optional(')', token.next)) {
      listener.endArguments(argumentCount, begin, token.next);
      return token.next.next;
    }
    do {
      token = parseExpression(token.next);
      ++argumentCount;
    } while (optional(',', token));
    listener.endArguments(argumentCount, begin, token);
    return expect(')', token);
  }

  Token parseVariablesDeclaration(Token token) {
    int count = 1;
    listener.beginVariablesDeclaration(token);
    token = parseFinalVarOrType(token);
    token = parseOptionallyInitializedIdentifier(token);
    while (optional(',', token)) {
      token = parseOptionallyInitializedIdentifier(token.next);
      ++count;
    }
    listener.endVariablesDeclaration(count, token);
    return expectSemicolon(token);
  }

  Token parseOptionallyInitializedIdentifier(Token token) {
    listener.beginInitializedIdentifier(token);
    token = parseIdentifier(token);
    if (optional('=', token)) {
      Token assignment = token;
      listener.beginInitializer(token);
      token = parseExpression(token.next);
      listener.endInitializer(assignment);
    }
    listener.endInitializedIdentifier();
    return token;
  }

  Token parseFinalVarOrType(Token token) {
    if ('final' === token.stringValue) {
      listener.handleFinalKeyword(token);
      return token.next;
    } else {
      return parseType(token);
    }
  }

  Token parseIfStatement(Token token) {
    Token ifToken = token;
    listener.beginIfStatement(ifToken);
    token = expect('if', token);
    token = parseParenthesizedExpression(token);
    token = parseStatement(token);
    Token elseToken = null;
    if (optional('else', token)) {
      elseToken = token;
      token = parseStatement(token.next);
    }
    listener.endIfStatement(ifToken, elseToken);
    return token;
  }

  Token parseForStatement(Token token) {
    // TODO(ahe): Support for-in.
    Token forToken = token;
    listener.beginForStatement(forToken);
    token = expect('for', token);
    token = expect('(', token);
    token = parseVariablesDeclaration(token); // TODO(ahe): Support other forms.
    token = parseExpressionStatement(token);
    token = parseExpression(token); // TODO(ahe): Support expression list here.
    token = expect(')', token);
    token = parseStatement(token);
    listener.endForStatement(forToken, token);
    return token;
  }

  Token parseWhileStatement(Token token) {
    Token whileToken = token;
    listener.beginWhileStatement(whileToken);
    token = expect('while', token);
    token = parseParenthesizedExpression(token);
    token = parseStatement(token);
    listener.endWhileStatement(whileToken, token);
    return token;
  }

  Token parseDoWhileStatement(Token token) {
    Token doToken = token;
    listener.beginDoWhileStatement(doToken);
    token = expect('do', token);
    token = parseStatement(token);
    Token whileToken = token;
    token = expect('while', token);
    token = parseParenthesizedExpression(token);
    listener.endDoWhileStatement(doToken, whileToken, token);
    return expectSemicolon(token);
  }

  Token parseBlock(Token token) {
    Token begin = token;
    listener.beginBlock(begin);
    int statementCount = 0;
    token = expect('{', token);
    while (!optional('}', token)) {
      token = parseStatement(token);
      ++statementCount;
    }
    listener.endBlock(statementCount, begin, token);
    return expect('}', token);
  }

  Token parseThrowStatement(Token token) {
    Token throwToken = token;
    listener.beginThrowStatement(throwToken);
    token = expect('throw', token);
    if (optional(';', token)) {
      listener.endRethrowStatement(throwToken, token);
      return token.next;
    } else {
      token = parseExpression(token);
      listener.endThrowStatement(throwToken, token);
      return expectSemicolon(token);
    }
  }
}
