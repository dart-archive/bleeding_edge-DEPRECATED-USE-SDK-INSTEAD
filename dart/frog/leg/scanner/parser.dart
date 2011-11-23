// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An event generating parser of Dart programs. This parser expects
 * all tokens in a linked list.
 */
class PartialParser<L extends Listener> {
  final L listener;

  // TODO(ahe): Clean up the following fields.
  Function beginTypeArguments;
  Function parseTypeFunction;
  Function endTypeArguments;
  Function handleNoTypeArguments;

  // TODO(ahe): Clean up the following fields.
  Function beginTypeVariables;
  Function parseTypeVariableFunction;
  Function endTypeVariables;
  Function handleNoTypeVariables;

  PartialParser(L this.listener) {
    // TODO(ahe): Workaround this not being bound correctly in Frog:
    beginTypeArguments = (t) => listener.beginTypeArguments(t);
    parseTypeFunction = (t) => parseType(t);
    endTypeArguments = (c, bt, et) => listener.endTypeArguments(c, bt, et);
    handleNoTypeArguments = (t) => listener.handleNoTypeArguments(t);

    beginTypeVariables = (t) => listener.beginTypeVariables(t);
    parseTypeVariableFunction = (t) => parseTypeVariable(t);
    endTypeVariables = (c, bt, et) => listener.endTypeVariables(c, bt, et);
    handleNoTypeVariables = (t) => listener.handleNoTypeVariables(t);
  }

  // TODO(ahe): Rename this method. It is too subtle compared to token.next.
  Token next(Token token) => checkEof(token.next);

  Token checkEof(Token token) {
    if (token.kind === EOF_TOKEN) {
      listener.unexpectedEof();
      throw 'Unexpected EOF';
    }
    return token;
  }

  void parseUnit(Token token) {
    while (token.kind !== EOF_TOKEN) {
      final value = token.stringValue;
      switch (true) {
        case value === 'interface':
          token = parseInterface(token);
          break;
        case value === 'class':
          token = parseClass(token);
          break;
        case value === 'typedef':
          token = parseNamedFunctionAlias(token);
          break;
        case value === '#':
          token = parseLibraryTags(token);
          break;
        default:
          token = parseTopLevelMember(token);
          break;
      }
    }
  }

  Token parseInterface(Token token) {
    listener.beginInterface(token);
    token = parseIdentifier(next(token));
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
    token = parseReturnTypeOpt(next(token));
    token = parseIdentifier(token);
    token = parseTypeVariablesOpt(token);
    token = parseFormalParameters(token);
    listener.endFunctionTypeAlias(token);
    return expect(';', token);
  }

  Token parseReturnTypeOpt(Token token) {
    if (token.stringValue === 'void') {
      listener.handleVoidKeyword(token);
      return next(token);
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
      token = parseTypeOpt(next(token));
      token = parseIdentifier(token);
      listener.endFormalParameter(token);
      ++parameterCount;
    } while (optional(',', token));
    listener.endFormalParameters(parameterCount, begin, token);
    return expect(')', token);
  }

  Token parseTypeOpt(Token token) {
    final nextValue = token.next.stringValue;
    switch (true) {
      case nextValue === '<':
      case nextValue === '.':
      case isIdentifier(token.next):
        return parseType(token);
      default:
        listener.handleNoType(token);
        return token;
    }
  }

  bool isIdentifier(Token token) {
    final kind = token.kind;
    switch (true) {
      case kind === IDENTIFIER_TOKEN:
        return true;
      case kind === KEYWORD_TOKEN:
        return token.value.isPseudo;
      default:
        return false;
    }
  }

  Token parseSupertypesClauseOpt(Token token) {
    if (optional('extends', token)) {
      do {
        token = parseType(next(token));
      } while (optional(',', token));
    }
    return token;
  }

  Token parseFactoryClauseOpt(Token token) {
    if (optional('factory', token)) {
      return parseType(next(token));
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

  Token skipArguments(BeginGroupToken token) {
    return token.endGroup;
  }

  Token parseClass(Token token) {
    Token begin = token;
    listener.beginClass(token);
    token = parseIdentifier(next(token));
    token = parseTypeVariablesOpt(token);
    Token extendsKeyword;
    if (optional('extends', token)) {
      extendsKeyword = token;
      token = parseType(next(token));
    } else {
      extendsKeyword = null;
      listener.handleNoType(token);
    }
    Token implementsKeyword;
    int interfacesCount = 0;
    if (optional('implements', token)) {
      do {
        token = parseType(next(token));
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
      return parseString(next(token));
    }
    return token;
  }

  Token parseString(Token token) {
    if (token.kind === STRING_TOKEN) {
      return next(token);
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
    return next(token);
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
      token = parseType(next(token));
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
        token = parseIdentifier(next(token));
        ++identifierCount;
      }
    } else if (optional('var', token)) {
      listener.handleVarKeyword(token);
      listener.endType(identifierCount, begin, token);
      return next(token);
    } else {
      token = listener.expectedType(token);
    }
    token = parseTypeArgumentsOpt(token);
    listener.endType(identifierCount, begin, token);
    return token;
  }

  Token parseTypeArgumentsOpt(Token token) {
    return parseStuff(token, beginTypeArguments, parseTypeFunction,
                      endTypeArguments, handleNoTypeArguments);
  }

  Token parseTypeVariablesOpt(Token token) {
    if (optional('<', token)) {
      BeginGroupToken beginGroupToken = token;
      token = next(beginGroupToken.endGroup);
    }
    listener.handleNoTypeVariables(token);
    return token;
  }

  Token parseTypeVariablesOptX(Token token) {
    return parseStuff(token, beginTypeVariables, parseTypeVariableFunction,
                      endTypeVariables, handleNoTypeVariables);
  }

  // TODO(ahe): Clean this up.
  Token parseStuff(Token token, Function beginStuff, Function stuffParser,
                   Function endStuff, Function handleNoStuff) {
    if (optional('<', token)) {
      Token begin = token;
      beginStuff(begin);
      int count = 0;
      do {
        token = stuffParser(next(token));
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
    Token previous = token;
    LOOP: while (token !== null) {
      final kind = token.kind;
      switch (true) {
        case kind === LBRACE_TOKEN:
        case kind === SEMICOLON_TOKEN:
        case kind === LPAREN_TOKEN:
        case kind === EQ_TOKEN:
          break LOOP;
        default:
          previous = token;
          token = next(token);
          break;
      }
    }
    token = parseIdentifier(previous);
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
    if (!isField) {
      token = next(skipArguments(token));
    }
    while (token !== null &&
           token.kind !== LBRACE_TOKEN &&
           token.kind !== SEMICOLON_TOKEN) {
      token = next(token);
    }
    if (!optional(';', token)) {
      token = skipBlock(token);
    }
    if (isField) {
      listener.endTopLevelField(start, token);
    } else {
      listener.endTopLevelMethod(start, token);
    }
    return token.next;
  }

  Token parseLibraryTags(Token token) {
    listener.beginLibraryTag(token);
    token = parseIdentifier(next(token));
    token = expect('(', token);
    while (token !== null &&
           token.kind !== LPAREN_TOKEN &&
           token.kind !== RPAREN_TOKEN) {
      token = next(token);
    }
    token = expect(')', token);
    return expect(';', token);
  }
}

class Parser extends PartialParser/* <NodeListener> Frog bug #320 */ {
  Parser(NodeListener listener) : super(listener);

  Token parseFunction(Token token) {
    listener.beginFunction(token);
    token = parseReturnTypeOpt(token);
    listener.beginFunctionName(token);
    token = parseIdentifier(token);
    listener.endFunctionName(token);
    token = parseFormalParameters(token);
    return parseFunctionBody(token);
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
    token = checkEof(expect('{', token));
    while (!optional('}', token)) {
      token = parseStatement(token);
      ++statementCount;
    }
    listener.endFunctionBody(statementCount, begin, token);
    return expect('}', token);
  }

  Token parseStatement(Token token) {
    checkEof(token);
    final value = token.stringValue;
    switch (true) {
      case token.kind === IDENTIFIER_TOKEN:
        return parseExpressionStatementOrDeclaration(token);
      case value === '{':
        return parseBlock(token);
      case value === 'return':
        return parseReturnStatement(token);
      case value === 'var':
        return parseVariablesDeclaration(token);
      case value === 'if':
        return parseIfStatement(token);
      case value === 'for':
        return parseForStatement(token);
      case value === 'throw':
        return parseThrowStatement(token);
      case value === 'void':
        return parseExpressionStatementOrDeclaration(token);
      // TODO(ahe): Handle other statements.
      default:
        return parseExpressionStatement(token);
    }
  }

  Token expectSemicolon(Token token) {
    return expect(';', token);
  }

  Token parseReturnStatement(Token token) {
    Token begin = token;
    listener.beginReturnStatement(begin);
    assert('return' === token.stringValue);
    token = next(token);
    if (optional(';', token)) {
      listener.endReturnStatement(false, begin, token);
    } else {
      token = parseExpression(token);
      listener.endReturnStatement(true, begin, token);
    }
    return expectSemicolon(token);
  }

  Token peekAfterType(Token token) {
    // TODO(ahe): Also handle var?
    assert('void' === token.stringValue || token.kind === IDENTIFIER_TOKEN);
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
      token = parseExpression(next(token));
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
      token = parseExpression(next(token));
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
        token = parseBinaryExpression(next(token), level + 1);
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
    switch (true) {
      case value === '(': return 0;
      case value === ')': return 0;
      case value === '%=': return 2;
      case value === '&=': return 2;
      case value === '*=': return 2;
      case value === '+=': return 2;
      case value === '-=': return 2;
      case value === '/=': return 2;
      case value === '<<=': return 2;
      case value === '=': return 2;
      case value === '>>=': return 2;
      case value === '>>>=': return 2;
      case value === '^=': return 2;
      case value === '|=': return 2;
      case value === '~/=': return 2;
      case value === '?': return 3;
      case value === '||': return 4;
      case value === '&&': return 5;
      case value === '|': return 6;
      case value === '^': return 7;
      case value === '&': return 8;
      case value === '!=': return 9;
      case value === '!==': return 9;
      case value === '==': return 9;
      case value === '===': return 9;
      case value === '<': return 10;
      case value === '<=': return 10;
      case value === '>': return 10;
      case value === '>=': return 10;
      case value === 'is': return 10;
      case value === '<<': return 11;
      case value === '>>': return 11;
      case value === '>>>': return 11;
      case value === '+': return 12;
      case value === '-': return 12;
      case value === '%': return 13;
      case value === '*': return 13;
      case value === '/': return 13;
      case value === '~/': return 13;
      case value === '.': return 14; // TODO(ahe): Remove this line.
      default: return 0;
    }
  }

  Token parseUnaryExpression(Token token) {
    String value = token.stringValue;
    switch (true) {
      // Prefix:
      case value === '!':
      case value === '+': // TODO(ahe): Being removed from specification.
      case value === '-':
      case value === '++': // TODO(ahe): Validate this is used correctly.
      case value === '--': // TODO(ahe): Validate this is used correctly.
      case value === '~': {
        Token operator = token;
        token = next(token);
        token = parseUnaryExpression(token);
        listener.handleUnaryPrefixExpression(operator);
        break;
      }
      default:
        token = parsePrimary(token);
        value = token.stringValue;
        switch (true) {
          // Postfix:
          case value === '++': // TODO(ahe): Validate this is used correctly.
          case value === '--': // TODO(ahe): Validate this is used correctly.
            listener.handleUnaryPostfixExpression(token);
            token = next(token);
            break;
        }
        break;
    }
    return token;
  }

  Token parsePrimary(Token token) {
    // TODO(ahe): Handle other expressions.
    final kind = token.kind;
    switch (true) {
      case kind === IDENTIFIER_TOKEN:
        return parseSend(token);
      case kind === INT_TOKEN:
        return parseLiteralInt(token);
      case kind === DOUBLE_TOKEN:
        return parseLiteralDouble(token);
      case kind === STRING_TOKEN:
        return parseLiteralString(token);
      case kind === KEYWORD_TOKEN: {
        final value = token.stringValue;
        switch (true) {
          case value === 'true':
          case value === 'false':
            return parseLiteralBool(token);
          case value === 'null':
            return parseLiteralNull(token);
          default:
            listener.unexpected(token);
            throw 'not yet implemented';
        }
      }
      default:
        listener.unexpected(token);
        throw 'not yet implemented';
    }
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
      token = parseExpression(next(token));
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
      token = parseOptionallyInitializedIdentifier(next(token));
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
      token = parseExpression(next(token));
      listener.endInitializer(assignment);
    }
    listener.endInitializedIdentifier();
    return token;
  }

  Token parseFinalVarOrType(Token token) {
    String value = token.stringValue;
    switch (true) {
      case 'final' === value:
        listener.handleFinalKeyword(token);
        return next(token);
      default:
        return parseType(token);
    }
  }

  Token parseIfStatement(Token token) {
    Token ifToken = token;
    listener.beginIfStatement(ifToken);
    token = expect('if', token);
    expect('(', token);
    token = parseArguments(token);
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
