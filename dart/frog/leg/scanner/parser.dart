// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An event generating parser of Dart programs. This parser expects
 * all tokens in a linked list.
 */
class Parser {
  final Listener listener;

  Parser(Listener this.listener);

  // TODO(ahe): Rename this method. It is too subtle compared to token.next.
  Token next(Token token) => checkEof(token.next);

  Token checkEof(Token token) {
    if (token == null) {
      listener.unexpectedEof();
      throw "Unexpected EOF";
    }
    return token;
  }

  void parseUnit(Token token) {
    while (token != null) {
      switch (token.value) {
        case Keyword.INTERFACE:
          token = parseInterface(token);
          break;
        case Keyword.CLASS:
          token = parseClass(token);
          break;
        case Keyword.TYPEDEF:
          token = parseNamedFunctionAlias(token);
          break;
        default:
          // TODO(ahe): Work around frog switch bug #314.
          if (token.value == const SourceString("#")) {
            token = parseLibraryTags(token);
          } else {
            token = parseTopLevelMember(token);
          }
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
    return expect(const SourceString(";"), token);
  }

  Token parseReturnTypeOpt(Token token) {
    if (token.value == Keyword.VOID) {
      listener.voidType(token);
      return next(token);
    } else {
      return parseTypeOpt(token);
    }
  }

  Token parseParameters(Token token) {
    expect(const SourceString("("), token);
    if (optional(const SourceString(")"), next(token))) {
      return next(next(token));
    }
    do {
      // TODO(ahe): Handle "final" and "var".
      token = parseTypeOpt(next(token));
      token = parseIdentifier(token);
    } while (optional(const SourceString(","), token));
    return expect(const SourceString(")"), token);
  }

  Token parseTypeOpt(Token token) {
    switch (true) {
      case optional(const SourceString("<"), next(token)):
      case optional(const SourceString("."), next(token)):
      case isIdentifier(next(token)):
        return parseType(token);
      default:
        return token;
    }
  }

  bool isIdentifier(Token token) {
    switch (token.kind) {
      case IDENTIFIER_TOKEN:
        return true;
      case KEYWORD_TOKEN:
        return token.value.isPseudo;
      default:
        return false;
    }
  }

  Token parseSupertypesClauseOpt(Token token) {
    if (optional(Keyword.EXTENDS, token)) {
      do {
        token = parseType(next(token));
      } while (optional(const SourceString(","), token));
    }
    return token;
  }

  Token parseFactoryClauseOpt(Token token) {
    if (optional(Keyword.FACTORY, token)) {
      return parseType(next(token));
    }
    return token;
  }

  Token skipBlock(Token token) {
    if (!optional(const SourceString("{"), token)) {
      return listener.expectedBlock(token);
    }
    token = next(token);
    int nesting = 1;
    do {
      switch (token.kind) {
        case LBRACE_TOKEN:
          nesting++;
          break;
        case RBRACE_TOKEN:
          nesting--;
          if (nesting == 0) {
            return token;
          }
          break;
      }
      token = next(token);
    } while (token != null);
    throw "Internal error: unreachable code";
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
    if (optional(Keyword.NATIVE, token)) {
      return parseString(next(token));
    }
    return token;
  }

  Token parseString(Token token) {
    switch (token.kind) {
      case STRING_TOKEN:
        return next(token);
      default:
        return listener.expected(const SourceString("string"), token);
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
    if (!optional(const SourceString("<"), token)) {
      return token;
    }
    listener.beginTypeVariables(token);
    do {
      token = parseTypeVariable(next(token));
    } while (optional(const SourceString(","), token));
    return expect(const SourceString(">"), token);
  }

  Token expect(SourceString string, Token token) {
    if (string != token.value) {
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

  bool optional(SourceString value, Token token) => value == token.value;

  Token parseSuperclassClauseOpt(Token token) {
    if (optional(Keyword.EXTENDS, token)) {
      return parseType(next(token));
    }
    return token;
  }

  Token parseType(Token token) {
    if (isIdentifier(token)) {
      token = parseIdentifier(token);
      while (optional(const SourceString("."), token)) {
        // TODO(ahe): Validate that there are at most two identifiers.
        token = parseIdentifier(next(token));
      }
    } else {
      token = listener.expectedType(token);
    }
    return parseTypeArgumentsOpt(token);
  }

  Token parseTypeArgumentsOpt(Token token) {
    if (optional(const SourceString("<"), token)) {
      listener.beginTypeArguments(next(token));
      do {
        token = parseType(next(token));
      } while (optional(const SourceString(","), token));
      return expect(const SourceString(">"), token);
    }
    return token;
  }

  Token parseImplementsOpt(Token token) {
    if (optional(Keyword.IMPLEMENTS, token)) {
      do {
        token = parseType(next(token));
      } while (optional(const SourceString(","), token));
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
    LOOP: while (token != null) {
      switch (token.kind) {
        case LBRACE_TOKEN:
        case SEMICOLON_TOKEN:
        case LPAREN_TOKEN:
        case EQ_TOKEN:
          break LOOP;
        default:
          previous = token;
          token = next(token);
          break;
      }
    }
    token = parseIdentifier(previous);
    if (optional(const SourceString("("), token)) {
      listener.topLevelMethod(start);
    } else if (optional(const SourceString("="), token) ||
               optional(const SourceString(";"), token)) {
      listener.topLevelField(start);
    } else {
      token = listener.unexpected(token);
    }
    while (token != null &&
           token.kind != LBRACE_TOKEN &&
           token.kind != SEMICOLON_TOKEN) {
      token = next(token);
    }
    if (!optional(const SourceString(";"), token)) {
      token = skipBlock(token);
    }
    listener.endTopLevelMember(token);
    return token.next;
  }

  Token parseLibraryTags(Token token) {
    listener.beginLibraryTag(token);
    token = parseIdentifier(next(token));
    token = expect(const SourceString("("), token);
    while (token != null &&
           token.kind != LPAREN_TOKEN &&
           token.kind != RPAREN_TOKEN) {
      token = next(token);
    }
    token = expect(const SourceString(")"), token);
    return expect(const SourceString(";"), token);
  }
}

class BodyParser extends Parser {
  // TODO(ahe): Breaks with frog.
  // BodyListener get listener() => super.listener;

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
    listener.beginFormalParameters(token);
    expect(const SourceString("("), token);
    if (optional(const SourceString(")"), token.next)) {
      listener.endFormalParameters(token.next);
      return token.next.next;
    }
    do {
      token = parseType(next(token)); // TODO(ahe): Types are optional.
      token = parseIdentifier(token);
    } while (optional(const SourceString(","), token));
    listener.endFormalParameters(token);
    return expect(const SourceString(")"), token);
  }

  Token parseFunctionBody(Token token) {
    if (optional(const SourceString(";"), token)) {
      listener.emptyFunctionBody(token);
      return token.next;
    }
    // TODO(ahe): Handle "=>" syntax.
    listener.beginFunctionBody(token);
    token = checkEof(expect(const SourceString("{"), token));
    while (!optional(const SourceString("}"), token)) {
      token = parseStatement(token);
    }
    listener.endFunctionBody(token);
    return expect(const SourceString("}"), token);
  }

  Token parseStatement(Token token) {
    checkEof(token);
    if (token.value == const SourceString('{')) {
      // TODO(ahe): Work around frog switch bug #314.
      return parseBlock(token);
    }
    switch (token.value) {
      case Keyword.RETURN:
        return parseReturnStatement(token);
      case Keyword.VAR:
        return parseVariablesDeclaration(token);
      case Keyword.IF:
        return parseIfStatement(token);
      // TODO(ahe): Handle other statements.
      default:
        return parseExpressionStatement(token);
    }
  }

  Token expectSemicolon(Token token) {
    return expect(const SourceString(";"), token);
  }

  Token parseReturnStatement(Token token) {
    listener.beginReturnStatement(token);
    assert(const SourceString("return") == token.value);
    token = parseExpression(next(token));
    listener.endReturnStatement(token);
    return expectSemicolon(token);
  }

  Token parseExpressionStatement(Token token) {
    listener.beginExpressionStatement(token);
    token = parseExpression(token);
    listener.endExpressionStatement(token);
    return expectSemicolon(token);
  }

  Token parseExpression(Token token) => parseBinaryExpression(token, 4);

  Token parseBinaryExpression(Token token, int precedence) {
    assert(precedence >= 4);
    token = parsePrimary(token);
    for (int level = getPrecedence(token); level >= precedence; --level) {
      while (getPrecedence(token) == level) {
        Token operator = token;
        token = parseBinaryExpression(next(token), level + 1);
        listener.binaryExpression(operator);
      }
    }
    return token;
  }

  int getPrecedence(Token token) {
    if (token === null) return 0;
    var value = token.value;
    if (value is !StringWrapper) return 0;
    // TODO(ahe): Find a better way to represent this.
    switch (value.toString()) {
      case "%=": return 2;
      case "&=": return 2;
      case "*=": return 2;
      case "+=": return 2;
      case "-=": return 2;
      case "/=": return 2;
      case "<<=": return 2;
      case "=": return 2;
      case ">>=": return 2;
      case ">>>=": return 2;
      case "^=": return 2;
      case "|=": return 2;
      case "~/=": return 2;
      case "?": return 3;
      case "||": return 4;
      case "&&": return 5;
      case "|": return 6;
      case "^": return 7;
      case "&": return 8;
      case "!=": return 9;
      case "!==": return 9;
      case "==": return 9;
      case "===": return 9;
      case "<": return 10;
      case "<=": return 10;
      case ">": return 10;
      case ">=": return 10;
      case "is": return 10;
      case "<<": return 11;
      case ">>": return 11;
      case ">>>": return 11;
      case "+": return 12;
      case "-": return 12;
      case "%": return 13;
      case "*": return 13;
      case "/": return 13;
      case "~/": return 13;
      default: return 0;
    }
  }

  Token parsePrimary(Token token) {
    // TODO(ahe): Handle other expressions.
    switch (token.kind) {
      case INT_TOKEN:
        return parseLiteralInt(token);
      case DOUBLE_TOKEN:
        return parseLiteralDouble(token);
      case STRING_TOKEN:
        return parseLiteralString(token);
      case KEYWORD_TOKEN: {
        switch (token.value) {
          case Keyword.TRUE:
          case Keyword.FALSE:
            return parseLiteralBool(token);
          default:
            listener.unexpected(token);
            throw "not yet implemented";
        }
      }
      case IDENTIFIER_TOKEN:
        return parseSend(token);
      default:
        listener.unexpected(token);
        throw "not yet implemented";
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
    if (optional(const SourceString("("), token)) {
      listener.beginArguments(token);
      if (optional(const SourceString(")"), token.next)) {
        listener.endArguments(token.next);
        return token.next.next;
      }
      do {
        token = parseExpression(next(token));
      } while (optional(const SourceString(","), token));
      listener.endArguments(token);
      return expect(const SourceString(")"), token);
    }
    return token;
  }

  Token parseVariablesDeclaration(Token token) {
    listener.beginVariablesDeclaration(token);
    token = parseFinalVarOrType(token);
    token = parseOptionallyInitializedIdentifier(token);
    while (optional(const SourceString(','), token)) {
      token = parseOptionallyInitializedIdentifier(next(token));
    }
    listener.endVariablesDeclaration(token);
    return expectSemicolon(token);
  }

  Token parseOptionallyInitializedIdentifier(Token token) {
    listener.beginInitializedIdentifier(token);
    token = parseIdentifier(token);
    if (optional(const SourceString('='), token)) {
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
    return expect(Keyword.VAR, token);
  }

  Token parseIfStatement(Token token) {
    listener.beginIfStatement(token);
    token = expect(Keyword.IF, token);
    token = expect(const SourceString('('), token);
    token = parseExpression(token);
    token = expect(const SourceString(')'), token);
    token = parseStatement(token);
    if (optional(Keyword.ELSE, token)) {
      token = parseStatement(token.next);
    }
    return token;
  }

  Token parseBlock(Token token) {
    token = expect(const SourceString('{'), token);
    while (!optional(const SourceString("}"), token)) {
      token = parseStatement(token);
    }
    return expect(const SourceString("}"), token);
  }
}
