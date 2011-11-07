// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An event generating parser of Dart programs. This parser expects
 * all tokens in a linked list.
 */
class Parser<L extends Listener> {
  final L listener;

  Parser(L this.listener);

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
    token = parseParameters(token);
    listener.endFunctionTypeAlias(token);
    return expect(';', token);
  }

  Token parseReturnTypeOpt(Token token) {
    if (token.stringValue === 'void') {
      listener.voidType(token);
      return next(token);
    } else {
      return parseTypeOpt(token);
    }
  }

  Token parseParameters(Token token) {
    expect('(', token);
    if (optional(')', next(token))) {
      return next(next(token));
    }
    do {
      // TODO(ahe): Handle 'final' and 'var'.
      token = parseTypeOpt(next(token));
      token = parseIdentifier(token);
    } while (optional(',', token));
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
    token = next(token);
    int nesting = 1;
    do {
      final kind = token.kind;
      switch (true) {
        case kind === LBRACE_TOKEN:
          nesting++;
          break;
        case kind === RBRACE_TOKEN:
          nesting--;
          if (nesting === 0) {
            return token;
          }
          break;
      }
      token = next(token);
    } while (token !== null);
    throw 'Internal error: unreachable code';
  }

  Token parseClass(Token token) {
    listener.beginClass(token);
    token = parseIdentifier(next(token));
    token = parseTypeVariablesOpt(token);
    token = parseSuperclassClauseOpt(token);
    token = parseImplementsOpt(token);
    token = parseNativeClassClauseOpt(token);
    return parseClassBody(token);
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
      listener.identifier(token);
    } else {
      listener.notIdentifier(token);
    }
    return next(token);
  }

  Token parseTypeVariablesOpt(Token token) {
    if (!optional('<', token)) {
      return token;
    }
    listener.beginTypeVariables(token);
    do {
      token = parseTypeVariable(next(token));
    } while (optional(',', token));
    return expect('>', token);
  }

  Token expect(String string, Token token) {
    if (string !== token.stringValue) {
      return listener.expected(string, token);
    }
    return token.next;
  }

  Token parseTypeVariable(Token token) {
    listener.beginTypeVariable(token);
    token = parseIdentifier(token);
    token = parseSuperclassClauseOpt(token);
    listener.endTypeVariable(token);
    return token;
  }

  bool optional(String value, Token token) => value === token.stringValue;

  Token parseSuperclassClauseOpt(Token token) {
    if (optional('extends', token)) {
      return parseType(next(token));
    }
    return token;
  }

  Token parseType(Token token) {
    if (isIdentifier(token)) {
      token = parseIdentifier(token);
      while (optional('.', token)) {
        // TODO(ahe): Validate that there are at most two identifiers.
        token = parseIdentifier(next(token));
      }
    } else {
      token = listener.expectedType(token);
    }
    return parseTypeArgumentsOpt(token);
  }

  Token parseTypeArgumentsOpt(Token token) {
    if (optional('<', token)) {
      listener.beginTypeArguments(next(token));
      do {
        token = parseType(next(token));
      } while (optional(',', token));
      return expect('>', token);
    }
    return token;
  }

  Token parseImplementsOpt(Token token) {
    if (optional('implements', token)) {
      do {
        token = parseType(next(token));
      } while (optional(',', token));
    }
    return token;
  }

  Token parseClassBody(Token token) {
    token = skipBlock(token);
    listener.endClass(token);
    return token.next;
  }

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
    if (optional('(', token)) {
      listener.topLevelMethod(start);
    } else if (optional('=', token) || optional(';', token)) {
      listener.topLevelField(start);
    } else {
      token = listener.unexpected(token);
    }
    while (token !== null &&
           token.kind !== LBRACE_TOKEN &&
           token.kind !== SEMICOLON_TOKEN) {
      token = next(token);
    }
    if (!optional(';', token)) {
      token = skipBlock(token);
    }
    listener.endTopLevelMember(token);
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

class BodyParser extends Parser/* <BodyListener> Frog bug #320 */ {
  BodyParser(BodyListener listener) : super(listener);

  Token parseFunction(Token token) {
    listener.beginFunction(token);
    token = parseReturnTypeOpt(token);
    listener.beginFunctionName(token);
    token = parseIdentifier(token);
    listener.endFunctionName(token);
    token = parseFormalParameters(token);
    return parseFunctionBody(token);
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
      token = parseType(next(token)); // TODO(ahe): Types are optional.
      token = parseIdentifier(token);
      ++parameterCount;
    } while (optional(',', token));
    listener.endFormalParameters(parameterCount, begin, token);
    return expect(')', token);
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
        return parseExpressionStatement(token);
      case value === '{':
        return parseBlock(token);
      case value === 'return':
        return parseReturnStatement(token);
      case value === 'var':
        return parseVariablesDeclaration(token);
      case value === 'if':
        return parseIfStatement(token);
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
    token = parseExpression(next(token));
    listener.endReturnStatement(true, begin, token);
    return expectSemicolon(token);
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
    token = parsePrimary(token);
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
      case value === '+': return 12;
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
      default: return 0;
    }
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

  Token parseSend(Token token) {
    listener.beginSend(token);
    token = parseIdentifier(token);
    token = parseArgumentsOpt(token);
    listener.endSend(token);
    return token;
  }

  Token parseArgumentsOpt(Token token) {
    if (!optional('(', token)) {
      listener.handleNoArgumentsOpt(token);
      return token;
    }
    else return parseArguments(token);
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
    // TODO(ahe): Handle types or final.
    listener.handleVarKeyword(token);
    return expect('var', token);
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
}
