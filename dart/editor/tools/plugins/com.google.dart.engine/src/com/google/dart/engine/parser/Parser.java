/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.parser;

import com.google.dart.engine.ast.*;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.parser.CommentAndMetadata;
import com.google.dart.engine.internal.parser.FinalConstVarOrType;
import com.google.dart.engine.internal.parser.Modifiers;
import com.google.dart.engine.scanner.BeginToken;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.dart.ParameterKind;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Instances of the class {@code Parser} are used to parse tokens into an AST structure.
 */
public class Parser {
  /**
   * The source being parsed.
   */
  private final Source source;

  /**
   * The error listener that will be informed of any errors that are found during the parse.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The next token to be parsed.
   */
  private Token currentToken;

  /**
   * A flag indicating whether the parser is currently in the body of a loop.
   */
  private boolean inLoop = false;

  /**
   * A flag indicating whether the parser is currently in a switch statement.
   */
  private boolean inSwitch = false;

  private static final String HIDE = "hide"; //$NON-NLS-1$
  private static final String OF = "of"; //$NON-NLS-1$
  private static final String ON = "on"; //$NON-NLS-1$
  private static final String SHOW = "show"; //$NON-NLS-1$

  /**
   * Initialize a newly created parser.
   * 
   * @param source the source being parsed
   * @param errorListener the error listener that will be informed of any errors that are found
   *          during the parse
   */
  public Parser(Source source, AnalysisErrorListener errorListener) {
    this.source = source;
    this.errorListener = errorListener;
  }

  /**
   * Parse a compilation unit, starting with the given token.
   * 
   * @param token the first token of the compilation unit
   * @return the compilation unit that was parsed
   */
  public CompilationUnit parseCompilationUnit(Token token) {
    currentToken = token;
    return parseCompilationUnit();
  }

  /**
   * Parse an expression, starting with the given token.
   * 
   * @param token the first token of the expression
   * @return the expression that was parsed, or {@code null} if the tokens do not represent a
   *         recognizable expression
   */
  public Expression parseExpression(Token token) {
    currentToken = token;
    return parseExpression();
  }

  /**
   * Parse a statement, starting with the given token.
   * 
   * @param token the first token of the statement
   * @return the statement that was parsed, or {@code null} if the tokens do not represent a
   *         recognizable statement
   */
  public Statement parseStatement(Token token) {
    currentToken = token;
    return parseStatement();
  }

  /**
   * Parse a sequence of statements, starting with the given token.
   * 
   * @param token the first token of the sequence of statement
   * @return the statements that were parsed, or {@code null} if the tokens do not represent a
   *         recognizable sequence of statements
   */
  public List<Statement> parseStatements(Token token) {
    currentToken = token;
    return parseStatements();
  }

  /**
   * Advance to the next token in the token stream.
   */
  private void advance() {
    currentToken = currentToken.getNext();
  }

  /**
   * Append the character equivalent of the given scalar value to the given builder. Use the start
   * and end indices to report an error, and don't append anything to the builder, if the scalar
   * value is invalid.
   * 
   * @param builder the builder to which the scalar value is to be appended
   * @param escapeSequence the escape sequence that was parsed to produce the scalar value
   * @param scalarValue the value to be appended
   * @param startIndex the index of the first character representing the scalar value
   * @param endIndex the index of the last character representing the scalar value
   */
  private void appendScalarValue(StringBuilder builder, String escapeSequence, int scalarValue,
      int startIndex, int endIndex) {
    if (scalarValue < 0 || scalarValue > Character.MAX_CODE_POINT
        || (scalarValue >= 0xD800 && scalarValue <= 0xDFFF)) {
      reportError(ParserErrorCode.INVALID_CODE_POINT, escapeSequence);
      return;
    }
    if (scalarValue < Character.MAX_VALUE) {
      builder.append((char) scalarValue);
    } else {
      builder.append(Character.toChars(scalarValue));
    }
  }

  /**
   * Compute the content of a string with the given literal representation.
   * 
   * @param lexeme the literal representation of the string
   * @return the actual value of the string
   */
  private String computeStringValue(String lexeme) {
    if (lexeme.startsWith("r\"\"\"") || lexeme.startsWith("r'''")) { //$NON-NLS-1$ //$NON-NLS-2$
      if (lexeme.length() > 4) {
        return lexeme.substring(4, lexeme.length() - 3);
      }
    } else if (lexeme.startsWith("r\"") || lexeme.startsWith("r'")) { //$NON-NLS-1$ //$NON-NLS-2$
      if (lexeme.length() > 2) {
        return lexeme.substring(2, lexeme.length() - 1);
      }
    }
    int start = 0;
    if (lexeme.startsWith("\"\"\"") || lexeme.startsWith("'''")) { //$NON-NLS-1$ //$NON-NLS-2$
      start += 3;
    } else if (lexeme.startsWith("\"") || lexeme.startsWith("'")) { //$NON-NLS-1$ //$NON-NLS-2$
      start += 1;
    }
    int end = lexeme.length();
    if (end > 3 && (lexeme.endsWith("\"\"\"") || lexeme.endsWith("'''"))) { //$NON-NLS-1$ //$NON-NLS-2$
      end -= 3;
    } else if (end > 1 && (lexeme.endsWith("\"") || lexeme.endsWith("'"))) { //$NON-NLS-1$ //$NON-NLS-2$
      end -= 1;
    }
    StringBuilder builder = new StringBuilder(end - start + 1);
    int index = start;
    while (index < end) {
      index = translateCharacter(builder, lexeme, index);
    }
    return builder.toString();
  }

  /**
   * Create a synthetic identifier.
   * 
   * @return the synthetic identifier that was created
   */
  private SimpleIdentifier createSyntheticIdentifier() {
    return new SimpleIdentifier(createSyntheticToken(TokenType.IDENTIFIER));
  }

  /**
   * Create a synthetic string literal.
   * 
   * @return the synthetic string literal that was created
   */
  private SimpleStringLiteral createSyntheticStringLiteral() {
    return new SimpleStringLiteral(createSyntheticToken(TokenType.STRING), "");
  }

  /**
   * Create a synthetic token with the given type.
   * 
   * @return the synthetic token that was created
   */
  private Token createSyntheticToken(TokenType type) {
    return new StringToken(type, "", currentToken.getOffset());
  }

  /**
   * Check that the given expression is assignable and report an error if it isn't.
   * 
   * <pre>
   * assignableExpression ::=
   *     primary (arguments* assignableSelector)+
   *   | 'super' assignableSelector
   *   | identifier
   *
   * assignableSelector ::=
   *     '[' expression ']'
   *   | '.' identifier
   * </pre>
   * 
   * @param expression the expression being checked
   */
  private void ensureAssignable(Expression expression) {
    if (expression != null && !expression.isAssignable()) {
      reportError(ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
    }
  }

//  /**
//   * If the current token is an identifier matching the given identifier, return it after advancing
//   * to the next token. Otherwise report an error and return the current token without advancing.
//   * 
//   * @param identifier the identifier that is expected
//   * @return the token that matched the given type
//   */
//  private Token expect(String identifier) {
//    if (matches(identifier)) {
//      return getAndAdvance();
//    }
//    // Remove uses of this method in favor of matches?
//    // Pass in the error code to use to report the error?
//    reportError(ParserErrorCode.EXPECTED_TOKEN, identifier);
//    return currentToken;
//  }

  /**
   * If the current token is a keyword matching the given string, return it after advancing to the
   * next token. Otherwise report an error and return the current token without advancing.
   * 
   * @param keyword the keyword that is expected
   * @return the token that matched the given type
   */
  private Token expect(Keyword keyword) {
    if (matches(keyword)) {
      return getAndAdvance();
    }
    // Remove uses of this method in favor of matches?
    // Pass in the error code to use to report the error?
    reportError(ParserErrorCode.EXPECTED_TOKEN, keyword.getSyntax());
    return currentToken;
  }

  /**
   * If the current token has the expected type, return it after advancing to the next token.
   * Otherwise report an error and return the current token without advancing.
   * 
   * @param type the type of token that is expected
   * @return the token that matched the given type
   */
  private Token expect(TokenType type) {
    if (matches(type)) {
      return getAndAdvance();
    }
    // Remove uses of this method in favor of matches?
    // Pass in the error code to use to report the error?
    reportError(ParserErrorCode.EXPECTED_TOKEN, type.getLexeme());
    return currentToken;
  }

  /**
   * Advance to the next token in the token stream, making it the new current token.
   * 
   * @return the token that was current before this method was invoked
   */
  private Token getAndAdvance() {
    Token token = currentToken;
    advance();
    return token;
  }

  /**
   * Return {@code true} if the current token is the first token of a return type that is followed
   * by an identifier, possibly followed by a list of type parameters, followed by a
   * left-parenthesis. This is used by parseTypeAlias to determine whether or not to parse a return
   * type.
   * 
   * @return {@code true} if we can successfully parse the rest of a type alias if we first parse a
   *         return type.
   */
  private boolean hasReturnTypeInTypeAlias() {
    Token next = skipReturnType(currentToken);
    if (next == null) {
      return false;
    }
    return matchesIdentifier(next);
  }

  /**
   * Return {@code true} if the current token appears to be the beginning of a function declaration.
   * 
   * @return {@code true} if the current token appears to be the beginning of a function declaration
   */
  private boolean isFunctionDeclaration() {
    if (matches(Keyword.VOID)) {
      return true;
    }
    Token afterReturnType = skipTypeName(currentToken);
    if (afterReturnType == null) {
      // There was no return type, but it is optional, so go back to where we started.
      afterReturnType = currentToken;
    }
    Token afterIdentifier = skipSimpleIdentifier(afterReturnType);
    if (afterIdentifier == null) {
      // It's possible that we parsed the function name as if it were a type name, so see whether
      // it makes sense if we assume that there is no type.
      afterIdentifier = skipSimpleIdentifier(currentToken);
    }
    if (afterIdentifier == null) {
      return false;
    }
    return isFunctionExpression(afterIdentifier);
  }

  /**
   * Return {@code true} if the given token appears to be the beginning of a function expression.
   * 
   * @param startToken the token that might be the start of a function expression
   * @return {@code true} if the given token appears to be the beginning of a function expression
   */
  private boolean isFunctionExpression(Token startToken) {
    Token afterParameters = skipFormalParameterList(startToken);
    if (afterParameters == null) {
      return false;
    }
    return matchesAny(afterParameters, TokenType.OPEN_CURLY_BRACKET, TokenType.FUNCTION);
  }

  /**
   * Return {@code true} if the given character is a valid hexadecimal digit.
   * 
   * @param character the character being tested
   * @return {@code true} if the character is a valid hexadecimal digit
   */
  private boolean isHexDigit(char character) {
    return ('0' <= character && character <= '9') || ('A' <= character && character <= 'F')
        || ('a' <= character && character <= 'f');
  }

  /**
   * Return {@code true} if the current token is the first token in an initialized variable
   * declaration rather than an expression. This method assumes that we have already skipped past
   * any metadata that might be associated with the declaration.
   * 
   * <pre>
   * initializedVariableDeclaration ::=
   *     declaredIdentifier ('=' expression)? (',' initializedIdentifier)*
   * 
   * declaredIdentifier ::=
   *     metadata finalConstVarOrType identifier
   * 
   * finalConstVarOrType ::=
   *     'final' type?
   *   | 'const' type?
   *   | 'var'
   *   | type
   * 
   * type ::=
   *     qualified typeArguments?
   * 
   * initializedIdentifier ::=
   *     identifier ('=' expression)?
   * </pre>
   * 
   * @return {@code true} if the current token is the first token in an initialized variable
   *         declaration
   */
  private boolean isInitializedVariableDeclaration() {
    if (matches(Keyword.FINAL) || matches(Keyword.CONST) || matches(Keyword.VAR)) {
      // An expression cannot start with a keyword other than 'throw'.
      return true;
    }
    // We know that we have an identifier, and need to see whether it might be a type name.
    Token token = skipTypeName(currentToken);
    if (token == null) {
      // There was no type name, so this can't be a declaration.
      return false;
    }
    token = skipSimpleIdentifier(token);
    if (token == null) {
      return false;
    }
    TokenType type = token.getType();
    return type == TokenType.EQ || type == TokenType.COMMA || type == TokenType.SEMICOLON
        || matches(token, Keyword.IN);
  }

  /**
   * Return {@code true} if the current token appears to be the beginning of a switch member.
   * 
   * @return {@code true} if the current token appears to be the beginning of a switch member
   */
  private boolean isSwitchMember() {
    Token token = currentToken;
    while (matches(token, TokenType.IDENTIFIER) && matches(token.getNext(), TokenType.COLON)) {
      token = token.getNext().getNext();
    }
    if (token.getType() == TokenType.KEYWORD) {
      Keyword keyword = ((KeywordToken) token).getKeyword();
      return keyword == Keyword.CASE || keyword == Keyword.DEFAULT;
    }
    return false;
  }

  /**
   * Compare the given tokens to find the token that appears first in the source being parsed. That
   * is, return the left-most of all of the tokens. The arguments are allowed to be {@code null}.
   * Return the token with the smallest offset, or {@code null} if there are no arguments or if all
   * of the arguments are {@code null}.
   * 
   * @param tokens the tokens being compared
   * @return the token with the smallest offset
   */
  private Token lexicallyFirst(Token... tokens) {
    Token first = null;
    int firstOffset = Integer.MAX_VALUE;
    for (Token token : tokens) {
      if (token != null) {
        int offset = token.getOffset();
        if (offset < firstOffset) {
          first = token;
        }
      }
    }
    return first;
  }

  /**
   * Return {@code true} if the current token matches the given keyword.
   * 
   * @param keyword the keyword that can optionally appear in the current location
   * @return {@code true} if the current token matches the given keyword
   */
  private boolean matches(Keyword keyword) {
    return matches(currentToken, keyword);
  }

  /**
   * Return {@code true} if the current token matches the given identifier.
   * 
   * @param identifier the identifier that can optionally appear in the current location
   * @return {@code true} if the current token matches the given identifier
   */
  private boolean matches(String identifier) {
    return currentToken.getType() == TokenType.IDENTIFIER
        && currentToken.getLexeme().equals(identifier);
  }

  /**
   * Return {@code true} if the given token matches the given keyword.
   * 
   * @param token the token being tested
   * @param keyword the keyword that is being tested for
   * @return {@code true} if the given token matches the given keyword
   */
  private boolean matches(Token token, Keyword keyword) {
    return token.getType() == TokenType.KEYWORD && ((KeywordToken) token).getKeyword() == keyword;
  }

  /**
   * Return {@code true} if the given token has the given type.
   * 
   * @param token the token being tested
   * @param type the type of token that is being tested for
   * @return {@code true} if the given token has the given type
   */
  private boolean matches(Token token, TokenType type) {
    return token.getType() == type;
  }

  /**
   * Return {@code true} if the current token has the given type. Note that this method, unlike
   * other variants, will modify the token stream if possible to match a wider range of tokens. In
   * particular, if we are attempting to match a '>' and the next token is either a '>>' or '>>>',
   * the token stream will be re-written and {@code true} will be returned.
   * 
   * @param type the type of token that can optionally appear in the current location
   * @return {@code true} if the current token has the given type
   */
  private boolean matches(TokenType type) {
    TokenType currentType = currentToken.getType();
    if (currentType != type) {
      if (type == TokenType.GT) {
        if (currentType == TokenType.GT_GT) {
          int offset = currentToken.getOffset();
          Token first = new Token(TokenType.GT, offset);
          Token second = new Token(TokenType.GT, offset + 1);
          second.setNext(currentToken.getNext());
          first.setNext(second);
          currentToken.getPrevious().setNext(first);
          currentToken = first;
          return true;
        } else if (currentType == TokenType.GT_GT_GT) {
          int offset = currentToken.getOffset();
          Token first = new Token(TokenType.GT, offset);
          Token second = new Token(TokenType.GT, offset + 1);
          Token third = new Token(TokenType.GT, offset + 2);
          third.setNext(currentToken.getNext());
          second.setNext(third);
          first.setNext(second);
          currentToken.getPrevious().setNext(first);
          currentToken = first;
          return true;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * Return {@code true} if the given token has any one of the given types.
   * 
   * @param token the token being tested
   * @param types the types of token that are being tested for
   * @return {@code true} if the given token has any of the given types
   */
  private boolean matchesAny(Token token, TokenType... types) {
    TokenType actualType = token.getType();
    for (TokenType type : types) {
      if (actualType == type) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return {@code true} if the current token is a valid identifier. Valid identifiers include
   * built-in identifiers (pseudo-keywords).
   * 
   * @return {@code true} if the current token is a valid identifier
   */
  private boolean matchesIdentifier() {
    return matchesIdentifier(currentToken);
  }

  /**
   * Return {@code true} if the given token is a valid identifier. Valid identifiers include
   * built-in identifiers (pseudo-keywords).
   * 
   * @return {@code true} if the given token is a valid identifier
   */
  private boolean matchesIdentifier(Token token) {
    return matches(token, TokenType.IDENTIFIER)
        || (matches(token, TokenType.KEYWORD) && ((KeywordToken) token).getKeyword().isPseudoKeyword());
  }

  /**
   * If the current token has the given type, then advance to the next token and return {@code true}
   * . Otherwise, return {@code false} without advancing.
   * 
   * @param type the type of token that can optionally appear in the current location
   * @return {@code true} if the current token has the given type
   */
  private boolean optional(TokenType type) {
    if (matches(type)) {
      advance();
      return true;
    }
    return false;
  }

  /**
   * Parse an additive expression.
   * 
   * <pre>
   * additiveExpression ::=
   *     multiplicativeExpression (additiveOperator multiplicativeExpression)*
   *   | 'super' (additiveOperator multiplicativeExpression)+
   * </pre>
   * 
   * @return the additive expression that was parsed
   */
  private Expression parseAdditiveExpression() {
    Expression expression;
    if (matches(Keyword.SUPER) && currentToken.getNext().getType().isAdditiveOperator()) {
      expression = new SuperExpression(getAndAdvance());
    } else {
      expression = parseMultiplicativeExpression();
    }
    while (currentToken.getType().isAdditiveOperator()) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseMultiplicativeExpression());
    }
    return expression;
  }

  /**
   * Parse an annotation.
   * 
   * <pre>
   * annotation ::=
   *     '@' qualified (‘.’ identifier)? arguments?
   * </pre>
   * 
   * @return the annotation that was parsed
   */
  private Annotation parseAnnotation() {
    Token atSign = expect(TokenType.AT);
    Identifier name = parsePrefixedIdentifier();
    Token period = null;
    SimpleIdentifier constructorName = null;
    if (matches(TokenType.PERIOD)) {
      period = getAndAdvance();
      constructorName = parseSimpleIdentifier();
    }
    ArgumentList arguments = null;
    if (matches(TokenType.OPEN_PAREN)) {
      arguments = parseArgumentList();
    }
    return new Annotation(atSign, name, period, constructorName, arguments);
  }

  /**
   * Parse an argument.
   * 
   * <pre>
   * argument ::=
   *     namedArgument
   *   | expression
   *
   * namedArgument ::=
   *     label expression
   * </pre>
   * 
   * @return the argument that was parsed
   */
  private Expression parseArgument() {
    //
    // Both namedArgument and expression can start with an identifier, but only namedArgument can
    // have an identifier followed by a colon.
    //
    if (matchesIdentifier() && matches(peek(), TokenType.COLON)) {
      SimpleIdentifier label = new SimpleIdentifier(getAndAdvance());
      Label name = new Label(label, getAndAdvance());
      return new NamedExpression(name, parseExpression());
    } else {
      return parseExpression();
    }
  }

  /**
   * Parse an argument definition test.
   * 
   * <pre>
   * argumentDefinitionTest ::=
   *     '?' identifier
   * </pre>
   * 
   * @return the argument definition test that was parsed
   */
  private ArgumentDefinitionTest parseArgumentDefinitionTest() {
    Token question = expect(TokenType.QUESTION);
    SimpleIdentifier identifier = parseSimpleIdentifier();
    return new ArgumentDefinitionTest(question, identifier);
  }

  /**
   * Parse a list of arguments.
   * 
   * <pre>
   * arguments ::=
   *     '(' argumentList? ')'
   * 
   * argumentList ::=
   *     namedArgument (',' namedArgument)*
   *   | expressionList (',' namedArgument)*
   * </pre>
   * 
   * @return the argument list that was parsed
   */
  private ArgumentList parseArgumentList() {
    Token leftParenthesis = expect(TokenType.OPEN_PAREN);
    List<Expression> arguments = new ArrayList<Expression>();
    if (matches(TokenType.CLOSE_PAREN)) {
      return new ArgumentList(leftParenthesis, arguments, getAndAdvance());
    }
    //
    // Even though unnamed arguments must all appear before any named arguments, we allow them to
    // appear in any order so that we can recover faster.
    //
    Expression argument = parseArgument();
    arguments.add(argument);
    boolean foundNamedArgument = argument instanceof NamedExpression;
    boolean generatedError = false;
    while (optional(TokenType.COMMA)) {
      argument = parseArgument();
      arguments.add(argument);
      if (foundNamedArgument) {
        if (!generatedError && !(argument instanceof NamedExpression)) {
          // Report the error, once, but allow the arguments to be in any order in the AST.
          reportError(ParserErrorCode.POSITIONAL_AFTER_NAMED_ARGUMENT);
          generatedError = true;
        }
      } else if (argument instanceof NamedExpression) {
        foundNamedArgument = true;
      }
    }
    // TODO(brianwilkerson) Recovery: Look at the left parenthesis to see whether there is a
    // matching right parenthesis. If there is, then we're more likely missing a comma and should
    // go back to parsing arguments.
    Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
    return new ArgumentList(leftParenthesis, arguments, rightParenthesis);
  }

  /**
   * Parse an assert statement.
   * 
   * <pre>
   * assertStatement ::=
   *     'assert' '(' conditionalExpression ')' ';'
   * </pre>
   * 
   * @return the assert statement
   */
  private AssertStatement parseAssertStatement() {
    Token keyword = expect(Keyword.ASSERT);
    Token leftParen = expect(TokenType.OPEN_PAREN);
    Expression expression = parseConditionalExpression();
    Token rightParen = expect(TokenType.CLOSE_PAREN);
    Token semicolon = expect(TokenType.SEMICOLON);
    return new AssertStatement(keyword, leftParen, expression, rightParen, semicolon);
  }

  /**
   * Parse an assignable expression.
   * 
   * <pre>
   * assignableExpression ::=
   *     primary (arguments* assignableSelector)+
   *   | 'super' assignableSelector
   *   | identifier
   * </pre>
   * 
   * @param primaryAllowed {@code true} if the expression is allowed to be a primary without any
   *          assignable selector
   * @return the assignable expression that was parsed
   */
  private Expression parseAssignableExpression(boolean primaryAllowed) {
    if (matches(Keyword.SUPER)) {
      return parseAssignableSelector(new SuperExpression(getAndAdvance()), false);
    }
    //
    // A primary expression can start with an identifier. We resolve the ambiguity by determining
    // whether the primary consists of anything other than an identifier and/or is followed by an
    // assignableSelector.
    //
    Expression expression = parsePrimaryExpression();
    boolean isOptional = primaryAllowed || expression instanceof SimpleIdentifier;
    while (true) {
      while (matches(TokenType.OPEN_PAREN)) {
        ArgumentList argumentList = parseArgumentList();
        if (expression instanceof SimpleIdentifier) {
          expression = new MethodInvocation(null, null, (SimpleIdentifier) expression, argumentList);
        } else if (expression instanceof PrefixedIdentifier) {
          PrefixedIdentifier identifier = (PrefixedIdentifier) expression;
          expression = new MethodInvocation(
              identifier.getPrefix(),
              identifier.getPeriod(),
              identifier.getIdentifier(),
              argumentList);
        } else if (expression instanceof PropertyAccess) {
          PropertyAccess access = (PropertyAccess) expression;
          expression = new MethodInvocation(
              access.getTarget(),
              access.getOperator(),
              access.getPropertyName(),
              argumentList);
        } else {
          expression = new FunctionExpressionInvocation(expression, argumentList);
        }
        if (!primaryAllowed) {
          isOptional = false;
        }
      }
      Expression selectorExpression = parseAssignableSelector(expression, isOptional
          || (expression instanceof PrefixedIdentifier));
      if (selectorExpression == expression) {
        if (!isOptional && (expression instanceof PrefixedIdentifier)) {
          PrefixedIdentifier identifier = (PrefixedIdentifier) expression;
          expression = new PropertyAccess(
              identifier.getPrefix(),
              identifier.getPeriod(),
              identifier.getIdentifier());
        }
        return expression;
      }
      expression = selectorExpression;
      isOptional = true;
    }
  }

  /**
   * Parse an assignable selector.
   * 
   * <pre>
   * assignableSelector ::=
   *     '[' expression ']'
   *   | '.' identifier
   * </pre>
   * 
   * @param prefix the expression preceding the selector
   * @param optional {@code true} if the selector is optional
   * @return the assignable selector that was parsed
   */
  private Expression parseAssignableSelector(Expression prefix, boolean optional) {
    if (matches(TokenType.OPEN_SQUARE_BRACKET)) {
      Token leftBracket = getAndAdvance();
      Expression index = parseExpression();
      Token rightBracket = expect(TokenType.CLOSE_SQUARE_BRACKET);
      return new IndexExpression(prefix, leftBracket, index, rightBracket);
    } else if (matches(TokenType.PERIOD)) {
      Token period = getAndAdvance();
      return new PropertyAccess(prefix, period, parseSimpleIdentifier());
    } else {
      if (!optional) {
        // Report the missing selector.
        reportError(ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR);
      }
      return prefix;
    }
  }

  /**
   * Parse a bitwise and expression.
   * 
   * <pre>
   * bitwiseAndExpression ::=
   *     equalityExpression ('&' equalityExpression)*
   *   | 'super' ('&' equalityExpression)+
   * </pre>
   * 
   * @return the bitwise and expression that was parsed
   */
  private Expression parseBitwiseAndExpression() {
    Expression expression;
    if (matches(Keyword.SUPER) && matches(peek(), TokenType.AMPERSAND)) {
      expression = new SuperExpression(getAndAdvance());
    } else {
      expression = parseEqualityExpression();
    }
    while (matches(TokenType.AMPERSAND)) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseEqualityExpression());
    }
    return expression;
  }

  /**
   * Parse a bitwise or expression.
   * 
   * <pre>
   * bitwiseOrExpression ::=
   *     bitwiseXorExpression ('|' bitwiseXorExpression)*
   *   | 'super' ('|' bitwiseXorExpression)+
   * </pre>
   * 
   * @return the bitwise or expression that was parsed
   */
  private Expression parseBitwiseOrExpression() {
    Expression expression;
    if (matches(Keyword.SUPER) && matches(peek(), TokenType.BAR)) {
      expression = new SuperExpression(getAndAdvance());
    } else {
      expression = parseBitwiseXorExpression();
    }
    while (matches(TokenType.BAR)) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseBitwiseXorExpression());
    }
    return expression;
  }

  /**
   * Parse a bitwise exclusive-or expression.
   * 
   * <pre>
   * bitwiseXorExpression ::=
   *     bitwiseAndExpression ('^' bitwiseAndExpression)*
   *   | 'super' ('^' bitwiseAndExpression)+
   * </pre>
   * 
   * @return the bitwise exclusive-or expression that was parsed
   */
  private Expression parseBitwiseXorExpression() {
    Expression expression;
    if (matches(Keyword.SUPER) && matches(peek(), TokenType.CARET)) {
      expression = new SuperExpression(getAndAdvance());
    } else {
      expression = parseBitwiseAndExpression();
    }
    while (matches(TokenType.CARET)) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseBitwiseAndExpression());
    }
    return expression;
  }

  /**
   * Parse a block.
   * 
   * <pre>
   * block ::=
   *     '{' statements '}'
   * </pre>
   * 
   * @return the block that was parsed
   */
  private Block parseBlock() {
    Token leftBracket = expect(TokenType.OPEN_CURLY_BRACKET);
    List<Statement> statements = new ArrayList<Statement>();
    Token statementStart = currentToken;
    while (!matches(TokenType.EOF) && !matches(TokenType.CLOSE_CURLY_BRACKET)) {
      Statement statement = parseStatement();
      if (statement != null) {
        statements.add(statement);
      }
      if (currentToken == statementStart) {
        // Ensure that we are making progress and report an error if we're not.
        reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
        advance();
      }
      statementStart = currentToken;
    }
    Token rightBracket = expect(TokenType.CLOSE_CURLY_BRACKET);
    return new Block(leftBracket, statements, rightBracket);
  }

  /**
   * Parse a break statement.
   * 
   * <pre>
   * breakStatement ::=
   *     'break' identifier? ';'
   * </pre>
   * 
   * @return the break statement that was parsed
   */
  private Statement parseBreakStatement() {
    Token breakKeyword = expect(Keyword.BREAK);
    SimpleIdentifier label = null;
    if (matchesIdentifier()) {
      label = parseSimpleIdentifier();
    }
    if (!inLoop && !inSwitch && label == null) {
      reportError(ParserErrorCode.BREAK_OUTSIDE_OF_LOOP, breakKeyword);
    }
    Token semicolon = expect(TokenType.SEMICOLON);
    return new BreakStatement(breakKeyword, label, semicolon);
  }

  /**
   * Parse a cascade section.
   * 
   * <pre>
   * cascadeSection ::=
   *     '..' cascadeSelector arguments* (assignableSelector arguments*)* cascadeAssignment?
   *
   * cascadeSelector ::=
   *     '[' expression ']'
   *   | identifier
   *
   * cascadeAssignment ::=
   *     assignmentOperator expressionWithoutCascade
   * </pre>
   * 
   * @return the expression representing the cascaded method invocation
   */
  private Expression parseCascadeSection() {
    Token period = expect(TokenType.PERIOD_PERIOD);
    Expression expression = null;
    SimpleIdentifier functionName = null;
    if (matchesIdentifier()) {
      functionName = parseSimpleIdentifier();
    } else if (currentToken.getType() == TokenType.OPEN_SQUARE_BRACKET) {
      Token leftBracket = getAndAdvance();
      Expression index = parseExpression();
      Token rightBracket = expect(TokenType.CLOSE_SQUARE_BRACKET);
      expression = new IndexExpression(period, leftBracket, index, rightBracket);
      period = null;
    } else {
      reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
      return expression;
    }
    if (currentToken.getType() == TokenType.OPEN_PAREN) {
      while (currentToken.getType() == TokenType.OPEN_PAREN) {
        if (functionName != null) {
          expression = new MethodInvocation(expression, period, functionName, parseArgumentList());
          period = null;
          functionName = null;
        } else if (expression == null) {
          return null;
        } else {
          expression = new FunctionExpressionInvocation(expression, parseArgumentList());
        }
      }
    } else if (functionName != null) {
      expression = new PropertyAccess(expression, period, functionName);
      period = null;
    }
    boolean progress = true;
    while (progress) {
      progress = false;
      Expression selector = parseAssignableSelector(expression, true);
      if (selector != expression) {
        expression = selector;
        progress = true;
        while (currentToken.getType() == TokenType.OPEN_PAREN) {
          expression = new FunctionExpressionInvocation(expression, parseArgumentList());
        }
      }
    }
    if (currentToken.getType().isAssignmentOperator()) {
      Token operator = getAndAdvance();
      ensureAssignable(expression);
      expression = new AssignmentExpression(expression, operator, parseExpression());
    }
    return expression;
  }

  /**
   * Parse a class declaration.
   * 
   * <pre>
   * classDeclaration ::=
   *     metadata 'abstract'? 'class' name typeParameterList? extendsClause? implementsClause? '{' (metadata memberDefinition)* '}'
   * </pre>
   * 
   * @param commentAndMetadata the metadata to be associated with the member
   * @param abstractKeyword the token for the keyword 'abstract', or {@code null} if the keyword was
   *          not given
   * @return the class declaration that was parsed
   */
  private ClassDeclaration parseClassDeclaration(CommentAndMetadata commentAndMetadata,
      Token abstractKeyword) {
    Token keyword = expect(Keyword.CLASS);
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
    String className = name.getName();
    TypeParameterList typeParameters = null;
    if (matches(TokenType.LT)) {
      typeParameters = parseTypeParameterList();
    }
    ExtendsClause extendsClause = null;
    if (matches(Keyword.EXTENDS)) {
      extendsClause = parseExtendsClause();
    }
    ImplementsClause implementsClause = null;
    if (matches(Keyword.IMPLEMENTS)) {
      implementsClause = parseImplementsClause();
    }
    Token leftBracket = expect(TokenType.OPEN_CURLY_BRACKET);
    List<ClassMember> members = new ArrayList<ClassMember>();
    Token memberStart = currentToken;
    while (!matches(TokenType.EOF) && !matches(TokenType.CLOSE_CURLY_BRACKET)
        && !matches(Keyword.CLASS)) {
      if (matches(TokenType.SEMICOLON)) {
        reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
        advance();
      } else {
        ClassMember member = parseClassMember(className);
        if (member != null) {
          members.add(member);
        }
      }
      if (currentToken == memberStart) {
        reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
        advance();
      }
      memberStart = currentToken;
    }
    Token rightBracket = expect(TokenType.CLOSE_CURLY_BRACKET);
    return new ClassDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        abstractKeyword,
        keyword,
        name,
        typeParameters,
        extendsClause,
        implementsClause,
        leftBracket,
        members,
        rightBracket);
  }

  /**
   * Parse a class member.
   * 
   * <pre>
   * classMemberDefinition ::=
   *     declaration ';'
   *   | methodSignature functionBody
   * </pre>
   * 
   * @param className the name of the class containing the member being parsed
   * @return the class member that was parsed
   */
  private ClassMember parseClassMember(String className) {
    CommentAndMetadata commentAndMetadata = parseCommentAndMetadata();
    Modifiers modifiers = parseModifiers();
    if (matches(Keyword.VOID)) {
      TypeName returnType = parseReturnType();
      if (matches(Keyword.GET) && matchesIdentifier(peek())) {
        validateModifiersForGetterOrSetterOrMethod(modifiers);
        return parseGetter(
            commentAndMetadata,
            modifiers.getExternalKeyword(),
            modifiers.getStaticKeyword(),
            returnType);
      } else if (matches(Keyword.SET) && matchesIdentifier(peek())) {
        validateModifiersForGetterOrSetterOrMethod(modifiers);
        return parseSetter(
            commentAndMetadata,
            modifiers.getExternalKeyword(),
            modifiers.getStaticKeyword(),
            returnType);
      } else if (matches(Keyword.OPERATOR) && peek().isOperator()) {
        validateModifiersForOperator(modifiers);
        return parseOperator(commentAndMetadata, modifiers.getExternalKeyword(), returnType);
      } else if (matchesIdentifier()
          && matchesAny(
              peek(),
              TokenType.OPEN_PAREN,
              TokenType.OPEN_CURLY_BRACKET,
              TokenType.FUNCTION)) {
        validateModifiersForGetterOrSetterOrMethod(modifiers);
        return parseMethodDeclaration(
            commentAndMetadata,
            modifiers.getExternalKeyword(),
            modifiers.getStaticKeyword(),
            returnType);
      } else {
        // We have found an error of some kind. Try to recover.
        if (matchesIdentifier()) {
          if (matchesAny(peek(), TokenType.EQ, TokenType.COMMA, TokenType.SEMICOLON)) {
            // We appear to have a variable declaration with a type of "void".
            reportError(ParserErrorCode.VOID_VARIABLE, returnType);
            return parseInitializedIdentifierList(
                commentAndMetadata,
                modifiers.getStaticKeyword(),
                validateModifiersForField(modifiers),
                returnType);
          }
        }
        // TODO(brianwilkerson) Report this error.
        return null;
      }
    } else if (matches(Keyword.GET) && matchesIdentifier(peek())) {
      validateModifiersForGetterOrSetterOrMethod(modifiers);
      return parseGetter(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          modifiers.getStaticKeyword(),
          null);
    } else if (matches(Keyword.SET) && matchesIdentifier(peek())) {
      validateModifiersForGetterOrSetterOrMethod(modifiers);
      return parseSetter(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          modifiers.getStaticKeyword(),
          null);
    } else if (matches(Keyword.OPERATOR) && peek().isOperator()) {
      validateModifiersForOperator(modifiers);
      return parseOperator(commentAndMetadata, modifiers.getExternalKeyword(), null);
    } else if (!matchesIdentifier()) {
      // TODO(brianwilkerson) Report an error and recover.
      // reportError(ParserErrorCode.?);
      return null;
    } else if (matches(peek(), TokenType.PERIOD) && matchesIdentifier(peek(2))
        && matches(peek(3), TokenType.OPEN_PAREN)) {
      return parseConstructor(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          validateModifiersForConstructor(modifiers),
          modifiers.getFactoryKeyword(),
          parseSimpleIdentifier(),
          getAndAdvance(),
          parseSimpleIdentifier(),
          parseFormalParameterList());
    } else if (matches(peek(), TokenType.OPEN_PAREN)) {
      SimpleIdentifier methodName = parseSimpleIdentifier();
      FormalParameterList parameters = parseFormalParameterList();
      if (matches(TokenType.COLON) || modifiers.getFactoryKeyword() != null
          || methodName.getName().equals(className)) {
        return parseConstructor(
            commentAndMetadata,
            modifiers.getExternalKeyword(),
            validateModifiersForConstructor(modifiers),
            modifiers.getFactoryKeyword(),
            methodName,
            null,
            null,
            parameters);
      }
      validateModifiersForGetterOrSetterOrMethod(modifiers);
      validateFormalParameterList(parameters);
      return parseMethodDeclaration(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          modifiers.getStaticKeyword(),
          null,
          methodName,
          parameters);
    } else if (matchesAny(peek(), TokenType.EQ, TokenType.COMMA, TokenType.SEMICOLON)) {
      return parseInitializedIdentifierList(
          commentAndMetadata,
          modifiers.getStaticKeyword(),
          validateModifiersForField(modifiers),
          null);
    }
    TypeName type = parseTypeName();
    if (matches(Keyword.GET) && matchesIdentifier(peek())) {
      validateModifiersForGetterOrSetterOrMethod(modifiers);
      return parseGetter(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          modifiers.getStaticKeyword(),
          type);
    } else if (matches(Keyword.SET) && matchesIdentifier(peek())) {
      validateModifiersForGetterOrSetterOrMethod(modifiers);
      return parseSetter(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          modifiers.getStaticKeyword(),
          type);
    } else if (matches(Keyword.OPERATOR) && peek().isOperator()) {
      validateModifiersForOperator(modifiers);
      return parseOperator(commentAndMetadata, modifiers.getExternalKeyword(), type);
    } else if (!matchesIdentifier()) {
      // TODO(brianwilkerson) Report an error and recover.
      // reportError(ParserErrorCode.?);
    } else if (matches(peek(), TokenType.OPEN_PAREN)) {
      validateModifiersForGetterOrSetterOrMethod(modifiers);
      return parseMethodDeclaration(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          modifiers.getStaticKeyword(),
          type);
    }
    return parseInitializedIdentifierList(
        commentAndMetadata,
        modifiers.getStaticKeyword(),
        validateModifiersForField(modifiers),
        type);
  }

  /**
   * Parse a list of combinators in a directive.
   * 
   * <pre>
   * combinator ::=
   *     'show' identifier (',' identifier)*
   *   | 'hide' identifier (',' identifier)*
   * </pre>
   * 
   * @return the combinators that were parsed
   */
  private List<Combinator> parseCombinators() {
    List<Combinator> combinators = new ArrayList<Combinator>();
    while (matches(SHOW) || matches(HIDE)) {
      Token keyword = expect(TokenType.IDENTIFIER);
      if (keyword.getLexeme().equals(SHOW)) {
        List<SimpleIdentifier> shownNames = parseIdentifierList();
        combinators.add(new ShowCombinator(keyword, shownNames));
      } else {
        List<SimpleIdentifier> hiddenNames = parseIdentifierList();
        combinators.add(new HideCombinator(keyword, hiddenNames));
      }
    }
    return combinators;
  }

  /**
   * Parse the documentation comment and metadata preceeding a declaration. This method allows any
   * number of documentation comments to occur before, after or between the metadata, but only
   * returns the last (right-most) documentation comment that is found.
   * 
   * <pre>
   * metadata ::=
   *     annotation*
   * </pre>
   * 
   * @return the documentation comment and metadata that were parsed
   */
  private CommentAndMetadata parseCommentAndMetadata() {
    Comment comment = parseDocumentationComment();
    List<Annotation> metadata = new ArrayList<Annotation>();
    while (matches(TokenType.AT)) {
      metadata.add(parseAnnotation());
      Comment optionalComment = parseDocumentationComment();
      if (optionalComment != null) {
        comment = optionalComment;
      }
    }
    return new CommentAndMetadata(comment, metadata);
  }

  /**
   * Parse a comment reference from the source between square brackets.
   * 
   * <pre>
   * commentReference ::=
   *     'new'? prefixedIdentifier
   * </pre>
   * 
   * @param referenceSource the source occurring between the square brackets within a documentation
   *          comment
   * @param sourceOffset the offset of the first character of the reference source
   * @return the comment reference that was parsed
   */
  private CommentReference parseCommentReference(String referenceSource, int sourceOffset) {
    // TODO(brianwilkerson) The errors are not getting the right offset/length and are being duplicated.
    if (referenceSource.length() == 0) {
      return null;
    }
    try {
      final boolean[] errorFound = {false};
      AnalysisErrorListener listener = new AnalysisErrorListener() {
        @Override
        public void onError(AnalysisError error) {
          errorFound[0] = true;
        }
      };
      StringScanner scanner = new StringScanner(null, sourceOffset, referenceSource, listener);
      Token firstToken = scanner.tokenize();
      if (!errorFound[0]) {
        Token newKeyword = null;
        if (matches(firstToken, Keyword.NEW)) {
          newKeyword = firstToken;
          firstToken = firstToken.getNext();
        }
        if (matchesIdentifier(firstToken)) {
          Token secondToken = firstToken.getNext();
          Token thirdToken = secondToken.getNext();
          Token nextToken;
          Identifier identifier;
          if (matches(secondToken, TokenType.PERIOD) && matchesIdentifier(thirdToken)) {
            identifier = new PrefixedIdentifier(
                new SimpleIdentifier(firstToken),
                secondToken,
                new SimpleIdentifier(thirdToken));
            nextToken = thirdToken.getNext();
          } else {
            identifier = new SimpleIdentifier(firstToken);
            nextToken = firstToken.getNext();
          }
          if (nextToken.getType() != TokenType.EOF) {
            // TODO(brianwilkerson) Determine whether this should really be reported in these cases.
            // reportError(ParserErrorCode.INVALID_COMMENT_REFERENCE);
          }
          return new CommentReference(newKeyword, identifier);
        } else if (matches(firstToken, Keyword.THIS) || matches(firstToken, Keyword.NULL)
            || matches(firstToken, Keyword.TRUE) || matches(firstToken, Keyword.FALSE)) {
          // TODO(brianwilkerson) If we want to support this we will need to extend the definition
          // of CommentReference to take an expression rather than an identifier. For now we just
          // ignore it to reduce the number of errors produced, but that's probably not a valid
          // long term approach.
          return null;
        } else if (matches(firstToken, TokenType.STRING)) {
          // This is a valid link to a URI and should be ignored
        } else {
          // TODO(brianwilkerson) Determine whether this should really be reported in these cases.
          // reportError(ParserErrorCode.INVALID_COMMENT_REFERENCE);
        }
      }
    } catch (Exception exception) {
      // TODO(brianwilkerson) Determine whether this should really be reported in these cases.
      // reportError(ParserErrorCode.INVALID_COMMENT_REFERENCE);
    }
    return null;
  }

  /**
   * Parse all of the comment references occurring in the given array of documentation comments.
   * 
   * <pre>
   * commentReference ::=
   *     '[' 'new'? qualified ']' libraryReference?
   * 
   * libraryReference ::=
   *      '(' stringLiteral ')'
   * </pre>
   * 
   * @param tokens the comment tokens representing the documentation comments to be parsed
   * @return the comment references that were parsed
   */
  private List<CommentReference> parseCommentReferences(Token[] tokens) {
    List<CommentReference> references = new ArrayList<CommentReference>();
    for (Token token : tokens) {
      String comment = token.getLexeme();
      int leftIndex = comment.indexOf('[');
      while (leftIndex >= 0) {
        int rightIndex = comment.indexOf(']', leftIndex);
        if (rightIndex >= 0) {
          char firstChar = comment.charAt(leftIndex + 1);
          if (firstChar != '\'' && firstChar != '"' && firstChar != ':') {
            // TODO(brianwilkerson) Handle the case where there's a library reference
            CommentReference reference = parseCommentReference(
                comment.substring(leftIndex + 1, rightIndex),
                token.getOffset() + leftIndex + 1);
            if (reference != null) {
              references.add(reference);
            }
          }
        } else {
          rightIndex = leftIndex + 1;
        }
        leftIndex = comment.indexOf('[', rightIndex);
      }
    }
    return references;
  }

  /**
   * Parse a compilation unit.
   * <p>
   * Specified:
   * 
   * <pre>
   * compilationUnit ::=
   *     scriptTag? directive* topLevelDeclaration*
   * </pre>
   * Actual:
   * 
   * <pre>
   * compilationUnit ::=
   *     scriptTag? topLevelElement*
   * 
   * topLevelElement ::=
   *     directive
   *   | topLevelDeclaration
   * </pre>
   * 
   * @return the compilation unit that was parsed
   */
  private CompilationUnit parseCompilationUnit() {
    Token firstToken = currentToken.getPrecedingComments();
    if (firstToken == null) {
      firstToken = currentToken;
    }
    ScriptTag scriptTag = null;
    if (matches(TokenType.SCRIPT_TAG)) {
      scriptTag = new ScriptTag(getAndAdvance());
    }
    //
    // Even though all directives must appear before declarations and must occur in a given order,
    // we allow directives and declarations to occur in any order so that we can recover better.
    //
    boolean libraryDirectiveFound = false;
    boolean partOfDirectiveFound = false;
    boolean partDirectiveFound = false;
    boolean directiveFoundAfterDeclaration = false;
    List<Directive> directives = new ArrayList<Directive>();
    List<CompilationUnitMember> declarations = new ArrayList<CompilationUnitMember>();
    Token memberStart = currentToken;
    while (!matches(TokenType.EOF)) {
      CommentAndMetadata commentAndMetadata = parseCommentAndMetadata();
      if (matches(Keyword.IMPORT) || matches(Keyword.EXPORT) || matches(Keyword.LIBRARY)
          || matches(Keyword.PART)) {
        Directive directive = parseDirective(commentAndMetadata);
        if (declarations.size() > 0 && !directiveFoundAfterDeclaration) {
          reportError(ParserErrorCode.DIRECTIVE_AFTER_DECLARATION);
          directiveFoundAfterDeclaration = true;
        }
        if (directive instanceof LibraryDirective) {
          if (libraryDirectiveFound) {
            reportError(ParserErrorCode.MULTIPLE_LIBRARY_DIRECTIVES);
          } else {
            if (directives.size() > 0) {
              reportError(ParserErrorCode.LIBRARY_DIRECTIVE_NOT_FIRST);
            }
            libraryDirectiveFound = true;
          }
        } else if (directive instanceof PartDirective) {
          partDirectiveFound = true;
        } else if (partDirectiveFound) {
          if (directive instanceof ExportDirective) {
            reportError(ParserErrorCode.EXPORT_DIRECTIVE_AFTER_PART_DIRECTIVE);
          } else if (directive instanceof ImportDirective) {
            reportError(ParserErrorCode.IMPORT_DIRECTIVE_AFTER_PART_DIRECTIVE);
          }
        }
        if (directive instanceof PartOfDirective) {
          if (partOfDirectiveFound) {
            reportError(ParserErrorCode.MULTIPLE_PART_OF_DIRECTIVES);
          } else {
            for (Directive preceedingDirective : directives) {
              reportError(
                  ParserErrorCode.NON_PART_OF_DIRECTIVE_IN_PART,
                  preceedingDirective.getKeyword());
            }
            partOfDirectiveFound = true;
          }
        } else {
          if (partOfDirectiveFound) {
            reportError(ParserErrorCode.NON_PART_OF_DIRECTIVE_IN_PART, directive.getKeyword());
          }
        }
        directives.add(directive);
      } else if (matches(TokenType.SEMICOLON)) {
        reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
        advance();
      } else {
        CompilationUnitMember member = parseCompilationUnitMember(commentAndMetadata);
        if (member != null) {
          declarations.add(member);
        }
      }
      if (currentToken == memberStart) {
        reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
        advance();
      }
      memberStart = currentToken;
    }
    return new CompilationUnit(firstToken, scriptTag, directives, declarations, currentToken);
  }

  /**
   * Parse a compilation unit member.
   * 
   * <pre>
   * compilationUnitMember ::=
   *     classDefinition
   *   | functionTypeAlias
   *   | external functionSignature
   *   | external getterSignature
   *   | external setterSignature
   *   | functionSignature functionBody
   *   | returnType? getOrSet identifier formalParameterList functionBody
   *   | (final | const) type? staticFinalDeclarationList ';'
   *   | variableDeclaration ';'
   * </pre>
   * 
   * @param commentAndMetadata the metadata to be associated with the member
   * @return the compilation unit member that was parsed
   */
  private CompilationUnitMember parseCompilationUnitMember(CommentAndMetadata commentAndMetadata) {
    Modifiers modifiers = parseModifiers();
    if (matches(Keyword.CLASS)) {
      return parseClassDeclaration(commentAndMetadata, validateModifiersForClass(modifiers));
    } else if (matches(Keyword.TYPEDEF)) {
      validateModifiersForTypedef(modifiers);
      return parseTypeAlias(commentAndMetadata);
    }
    if (matches(Keyword.VOID)) {
      TypeName returnType = parseReturnType();
      if ((matches(Keyword.GET) || matches(Keyword.SET)) && matchesIdentifier(peek())) {
        validateModifiersForTopLevelFunction(modifiers);
        return parseFunctionDeclaration(
            commentAndMetadata,
            modifiers.getExternalKeyword(),
            null,
            false);
      } else if (matches(Keyword.OPERATOR) && peek().isOperator()) {
        // TODO(brianwilkerson) Report this error and recover.
        return null;
      } else if (matchesIdentifier()
          && matchesAny(
              peek(),
              TokenType.OPEN_PAREN,
              TokenType.OPEN_CURLY_BRACKET,
              TokenType.FUNCTION)) {
        validateModifiersForTopLevelFunction(modifiers);
        return parseFunctionDeclaration(
            commentAndMetadata,
            modifiers.getExternalKeyword(),
            null,
            false);
      } else {
        // We have found an error of some kind. Try to recover.
        if (matchesIdentifier()) {
          if (matchesAny(peek(), TokenType.EQ, TokenType.COMMA, TokenType.SEMICOLON)) {
            // We appear to have a variable declaration with a type of "void".
            reportError(ParserErrorCode.VOID_VARIABLE, returnType);
            return new TopLevelVariableDeclaration(
                commentAndMetadata.getComment(),
                commentAndMetadata.getMetadata(),
                parseVariableDeclarationList(validateModifiersForTopLevelVariable(modifiers), null),
                expect(TokenType.SEMICOLON));
          }
        }
        // TODO(brianwilkerson) Report this error.
        return null;
      }
    } else if ((matches(Keyword.GET) || matches(Keyword.SET)) && matchesIdentifier(peek())) {
      validateModifiersForTopLevelFunction(modifiers);
      return parseFunctionDeclaration(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          null,
          false);
    } else if (matches(Keyword.OPERATOR) && peek().isOperator()) {
      // TODO(brianwilkerson) Report this error and recover.
      return null;
    } else if (!matchesIdentifier()) {
      // TODO(brianwilkerson) Report this error and recover.
      return null;
    } else if (matches(peek(), TokenType.OPEN_PAREN)) {
      validateModifiersForTopLevelFunction(modifiers);
      return parseFunctionDeclaration(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          null,
          false);
    } else if (matchesAny(peek(), TokenType.EQ, TokenType.COMMA, TokenType.SEMICOLON)) {
      return new TopLevelVariableDeclaration(
          commentAndMetadata.getComment(),
          commentAndMetadata.getMetadata(),
          parseVariableDeclarationList(validateModifiersForTopLevelVariable(modifiers), null),
          expect(TokenType.SEMICOLON));
    }
    TypeName returnType = parseReturnType();
    if (matches(Keyword.GET) || matches(Keyword.SET)) {
      validateModifiersForTopLevelFunction(modifiers);
      return parseFunctionDeclaration(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          returnType,
          false);
    } else if (!matchesIdentifier()) {
      // TODO(brianwilkerson) Report this error and recover.
      return null;
    }
    if (matchesAny(peek(), TokenType.OPEN_PAREN, TokenType.FUNCTION, TokenType.OPEN_CURLY_BRACKET)) {
      validateModifiersForTopLevelFunction(modifiers);
      return parseFunctionDeclaration(
          commentAndMetadata,
          modifiers.getExternalKeyword(),
          returnType,
          false);
    }
    return new TopLevelVariableDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        parseVariableDeclarationList(validateModifiersForTopLevelVariable(modifiers), returnType),
        expect(TokenType.SEMICOLON));
  }

  /**
   * Parse a conditional expression.
   * 
   * <pre>
   * conditionalExpression ::=
   *     logicalOrExpression ('?' expressionWithoutCascade ':' expressionWithoutCascade)?
   * </pre>
   * 
   * @return the conditional expression that was parsed
   */
  private Expression parseConditionalExpression() {
    Expression condition = parseLogicalOrExpression();
    if (!matches(TokenType.QUESTION)) {
      return condition;
    }
    Token question = getAndAdvance();
    Expression thenExpression = parseExpressionWithoutCascade();
    Token colon = expect(TokenType.COLON);
    Expression elseExpression = parseExpressionWithoutCascade();
    return new ConditionalExpression(condition, question, thenExpression, colon, elseExpression);
  }

  /**
   * Parse a const expression.
   * 
   * <pre>
   * constExpression ::=
   *     instanceCreationExpression
   *   | listLiteral
   *   | mapLiteral
   * </pre>
   * 
   * @return the const expression that was parsed
   */
  private Expression parseConstExpression() {
    Token keyword = expect(Keyword.CONST);
    if (matches(TokenType.OPEN_SQUARE_BRACKET) || matches(TokenType.INDEX)) {
      return parseListLiteral(keyword, null);
    } else if (matches(TokenType.OPEN_CURLY_BRACKET)) {
      return parseMapLiteral(keyword, null);
    } else if (matches(TokenType.LT)) {
      return parseListOrMapLiteral(keyword);
    }
    return parseInstanceCreationExpression(keyword);
  }

  private ConstructorDeclaration parseConstructor(CommentAndMetadata commentAndMetadata,
      Token externalKeyword, Token constKeyword, Token factoryKeyword, SimpleIdentifier returnType,
      Token period, SimpleIdentifier name, FormalParameterList parameters) {
    boolean bodyAllowed = externalKeyword == null;
    Token separator = null;
    List<ConstructorInitializer> initializers = null;
    if (matches(TokenType.COLON)) {
      separator = getAndAdvance();
      initializers = new ArrayList<ConstructorInitializer>();
      do {
        if (matches(Keyword.THIS)) {
          if (matches(peek(), TokenType.OPEN_PAREN)) {
            bodyAllowed = false;
            initializers.add(parseRedirectingConstructorInvocation());
          } else if (matches(peek(), TokenType.PERIOD) && matches(peek(3), TokenType.OPEN_PAREN)) {
            bodyAllowed = false;
            initializers.add(parseRedirectingConstructorInvocation());
          } else {
            initializers.add(parseConstructorFieldInitializer());
          }
        } else if (matches(Keyword.SUPER)) {
          initializers.add(parseSuperConstructorInvocation());
        } else {
          initializers.add(parseConstructorFieldInitializer());
        }
      } while (optional(TokenType.COMMA));
    }
    ConstructorName redirectedConstructor = null;
    FunctionBody body;
    if (matches(TokenType.EQ)) {
      separator = getAndAdvance();
      redirectedConstructor = parseConstructorName();
      body = new EmptyFunctionBody(expect(TokenType.SEMICOLON));
    } else {
      body = parseFunctionBody(true, false);
      if (!bodyAllowed && !(body instanceof EmptyFunctionBody)) {
        reportError(ParserErrorCode.EXTERNAL_CONSTRUCTOR_WITH_BODY);
      }
    }
    return new ConstructorDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        externalKeyword,
        constKeyword,
        factoryKeyword,
        returnType,
        period,
        name,
        parameters,
        separator,
        initializers,
        redirectedConstructor,
        body);
  }

  /**
   * Parse a field initializer within a constructor.
   * 
   * <pre>
   * fieldInitializer:
   *     ('this' '.')? identifier '=' conditionalExpression cascadeSection*
   * </pre>
   * 
   * @return the field initializer that was parsed
   */
  private ConstructorFieldInitializer parseConstructorFieldInitializer() {
    Token keyword = null;
    Token period = null;
    if (matches(Keyword.THIS)) {
      keyword = getAndAdvance();
      period = expect(TokenType.PERIOD);
    }
    SimpleIdentifier fieldName = parseSimpleIdentifier();
    Token equals = expect(TokenType.EQ);
    Expression expression = parseConditionalExpression();
    TokenType tokenType = currentToken.getType();
    if (tokenType == TokenType.PERIOD_PERIOD) {
      List<Expression> cascadeSections = new ArrayList<Expression>();
      while (tokenType == TokenType.PERIOD_PERIOD) {
        Expression section = parseCascadeSection();
        if (section != null) {
          cascadeSections.add(section);
        }
        tokenType = currentToken.getType();
      }
      expression = new CascadeExpression(expression, cascadeSections);
    }
    return new ConstructorFieldInitializer(keyword, period, fieldName, equals, expression);
  }

  /**
   * Parse the name of a constructor.
   * 
   * <pre>
   * constructorName:
   *     type ('.' identifier)?
   * </pre>
   * 
   * @return the constructor name that was parsed
   */
  private ConstructorName parseConstructorName() {
    TypeName type = parseTypeName();
    Token period = null;
    SimpleIdentifier name = null;
    if (matches(TokenType.PERIOD)) {
      period = getAndAdvance();
      name = parseSimpleIdentifier();
    }
    return new ConstructorName(type, period, name);
  }

  /**
   * Parse a continue statement.
   * 
   * <pre>
   * continueStatement ::=
   *     'continue' identifier? ';'
   * </pre>
   * 
   * @return the continue statement that was parsed
   */
  private Statement parseContinueStatement() {
    Token continueKeyword = expect(Keyword.CONTINUE);
    if (!inLoop && !inSwitch) {
      reportError(ParserErrorCode.CONTINUE_OUTSIDE_OF_LOOP, continueKeyword);
    }
    SimpleIdentifier label = null;
    if (matchesIdentifier()) {
      label = parseSimpleIdentifier();
    }
    if (inSwitch && !inLoop && label == null) {
      reportError(ParserErrorCode.CONTINUE_WITHOUT_LABEL_IN_CASE, continueKeyword);
    }
    Token semicolon = expect(TokenType.SEMICOLON);
    return new ContinueStatement(continueKeyword, label, semicolon);
  }

  /**
   * Parse a directive.
   * 
   * <pre>
   * directive ::=
   *     exportDirective
   *   | libraryDirective
   *   | importDirective
   *   | partDirective
   * </pre>
   * 
   * @param commentAndMetadata the metadata to be associated with the directive
   * @return the directive that was parsed
   */
  private Directive parseDirective(CommentAndMetadata commentAndMetadata) {
    if (matches(Keyword.IMPORT)) {
      return parseImportDirective(commentAndMetadata);
    } else if (matches(Keyword.EXPORT)) {
      return parseExportDirective(commentAndMetadata);
    } else if (matches(Keyword.LIBRARY)) {
      return parseLibraryDirective(commentAndMetadata);
    } else if (matches(Keyword.PART)) {
      return parsePartDirective(commentAndMetadata);
    } else {
      // Internal error
      return null;
    }
  }

  /**
   * Parse a documentation comment.
   * 
   * <pre>
   * documentationComment ::=
   *     multiLineComment?
   *   | singleLineComment*
   * </pre>
   * 
   * @return the documentation comment that was parsed, or {@code null} if there was no comment
   */
  private Comment parseDocumentationComment() {
    List<Token> commentTokens = new ArrayList<Token>();
    Token commentToken = currentToken.getPrecedingComments();
    while (commentToken != null) {
      if (commentToken.getType() == TokenType.SINGLE_LINE_COMMENT) {
        if (commentToken.getLexeme().startsWith("///")) { //$NON-NLS-1$
          if (commentTokens.size() == 1 && commentTokens.get(0).getLexeme().startsWith("/**")) { //$NON-NLS-1$
            commentTokens.clear();
          }
          commentTokens.add(commentToken);
        }
      } else {
        if (commentToken.getLexeme().startsWith("/**")) { //$NON-NLS-1$
          commentTokens.clear();
          commentTokens.add(commentToken);
        }
      }
      commentToken = commentToken.getNext();
    }
    if (commentTokens.isEmpty()) {
      return null;
    }
    Token[] tokens = commentTokens.toArray(new Token[commentTokens.size()]);
    List<CommentReference> references = parseCommentReferences(tokens);
    return Comment.createDocumentationComment(tokens, references);
  }

  /**
   * Parse a do statement.
   * 
   * <pre>
   * doStatement ::=
   *     'do' statement 'while' '(' expression ')' ';'
   * </pre>
   * 
   * @return the do statement that was parsed
   */
  private Statement parseDoStatement() {
    boolean wasInLoop = inLoop;
    inLoop = true;
    try {
      Token doKeyword = expect(Keyword.DO);
      Statement body = parseStatement();
      Token whileKeyword = expect(Keyword.WHILE);
      Token leftParenthesis = expect(TokenType.OPEN_PAREN);
      Expression condition = parseExpression();
      Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
      Token semicolon = expect(TokenType.SEMICOLON);
      return new DoStatement(
          doKeyword,
          body,
          whileKeyword,
          leftParenthesis,
          condition,
          rightParenthesis,
          semicolon);
    } finally {
      inLoop = wasInLoop;
    }
  }

  /**
   * Parse an empty statement.
   * 
   * <pre>
   * emptyStatement ::=
   *     ';'
   * </pre>
   * 
   * @return the empty statement that was parsed
   */
  private Statement parseEmptyStatement() {
    return new EmptyStatement(getAndAdvance());
  }

  /**
   * Parse an equality expression.
   * 
   * <pre>
   * equalityExpression ::=
   *     relationalExpression (equalityOperator relationalExpression)?
   *   | 'super' equalityOperator relationalExpression
   * </pre>
   * 
   * @return the equality expression that was parsed
   */
  private Expression parseEqualityExpression() {
    Expression expression;
    if (matches(Keyword.SUPER) && currentToken.getNext().getType().isEqualityOperator()) {
      expression = new SuperExpression(getAndAdvance());
    } else {
      expression = parseRelationalExpression();
    }
    while (currentToken.getType().isEqualityOperator()) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseRelationalExpression());
    }
    return expression;
  }

  /**
   * Parse an export directive.
   * 
   * <pre>
   * exportDirective ::=
   *     metadata 'export' stringLiteral combinator*';'
   * </pre>
   * 
   * @param commentAndMetadata the metadata to be associated with the directive
   * @return the export directive that was parsed
   */
  private ExportDirective parseExportDirective(CommentAndMetadata commentAndMetadata) {
    Token exportKeyword = expect(Keyword.EXPORT);
    StringLiteral libraryUri = parseStringLiteral();
    List<Combinator> combinators = parseCombinators();
    Token semicolon = expect(TokenType.SEMICOLON);
    return new ExportDirective(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        exportKeyword,
        libraryUri,
        combinators,
        semicolon);
  }

  /**
   * Parse an expression that does not contain any cascades.
   * 
   * <pre>
   * expression ::=
   *     assignableExpression assignmentOperator expression
   *   | conditionalExpression cascadeSection*
   *   | throwExpression
   * </pre>
   * 
   * @return the expression that was parsed
   */
  private Expression parseExpression() {
    if (matches(Keyword.THROW)) {
      return parseThrowExpression();
    }
    //
    // assignableExpression is a subset of conditionalExpression, so we can parse a conditional
    // expression and then determine whether it is followed by an assignmentOperator, checking for
    // conformance to the restricted grammar after making that determination.
    //
    Expression expression = parseConditionalExpression();
    TokenType tokenType = currentToken.getType();
    if (tokenType == TokenType.PERIOD_PERIOD) {
      List<Expression> cascadeSections = new ArrayList<Expression>();
      while (tokenType == TokenType.PERIOD_PERIOD) {
        Expression section = parseCascadeSection();
        if (section != null) {
          cascadeSections.add(section);
        }
        tokenType = currentToken.getType();
      }
      return new CascadeExpression(expression, cascadeSections);
    } else if (tokenType.isAssignmentOperator()) {
      Token operator = getAndAdvance();
      ensureAssignable(expression);
      return new AssignmentExpression(expression, operator, parseExpression());
    }
    return expression;
  }

  /**
   * Parse a list of expressions.
   * 
   * <pre>
   * expressionList ::=
   *     expression (',' expression)*
   * </pre>
   * 
   * @return the expression that was parsed
   */
  private List<Expression> parseExpressionList() {
    List<Expression> expressions = new ArrayList<Expression>();
    expressions.add(parseExpression());
    while (optional(TokenType.COMMA)) {
      expressions.add(parseExpression());
    }
    return expressions;
  }

  /**
   * Parse an expression that does not contain any cascades.
   * 
   * <pre>
   * expressionWithoutCascade ::=
   *     assignableExpression assignmentOperator expressionWithoutCascade
   *   | conditionalExpression
   *   | throwExpressionWithoutCascade
   * </pre>
   * 
   * @return the expression that was parsed
   */
  private Expression parseExpressionWithoutCascade() {
    if (matches(Keyword.THROW)) {
      return parseThrowExpressionWithoutCascade();
    }
    //
    // assignableExpression is a subset of conditionalExpression, so we can parse a conditional
    // expression and then determine whether it is followed by an assignmentOperator, checking for
    // conformance to the restricted grammar after making that determination.
    //
    Expression expression = parseConditionalExpression();
    if (currentToken.getType().isAssignmentOperator()) {
      Token operator = getAndAdvance();
      ensureAssignable(expression);
      expression = new AssignmentExpression(expression, operator, parseExpressionWithoutCascade());
    }
    return expression;
  }

  /**
   * Parse a class extends clause.
   * 
   * <pre>
   * classExtendsClause ::=
   *     'extends' type
   * </pre>
   * 
   * @return the class extends clause that was parsed
   */
  private ExtendsClause parseExtendsClause() {
    Token keyword = expect(Keyword.EXTENDS);
    TypeName superclass = parseTypeName();
    return new ExtendsClause(keyword, superclass);
  }

  /**
   * Parse the 'final', 'const', 'var' or type preceding a variable declaration.
   * 
   * <pre>
   * finalConstVarOrType ::=
   *   | 'final' type?
   *   | 'const' type?
   *   | 'var'
   *   | type
   * </pre>
   * 
   * @param optional {@code true} if the keyword and type are optional
   * @return the 'final', 'const', 'var' or type that was parsed
   */
  private FinalConstVarOrType parseFinalConstVarOrType(boolean optional) {
    Token keyword = null;
    TypeName type = null;
    if (matches(Keyword.FINAL) || matches(Keyword.CONST)) {
      keyword = getAndAdvance();
      if (matchesIdentifier(peek()) || matches(peek(), TokenType.LT)
          || matches(peek(), Keyword.THIS)) {
        type = parseTypeName();
      }
    } else if (matches(Keyword.VAR)) {
      keyword = getAndAdvance();
    } else {
      // We are assuming that the current token is an identifier.
      if (matchesIdentifier(peek())
          || matches(peek(), TokenType.LT)
          || matches(peek(), Keyword.THIS)
          || (matches(peek(), TokenType.PERIOD) && matchesIdentifier(peek(2)) && (matchesIdentifier(peek(3))
              || matches(peek(3), TokenType.LT) || matches(peek(3), Keyword.THIS)))) {
        type = parseReturnType();
      } else if (!optional) {
        reportError(ParserErrorCode.MISSING_CONST_FINAL_VAR_OR_TYPE);
      }
    }
    return new FinalConstVarOrType(keyword, type);
  }

  /**
   * Parse a formal parameter. At most one of {@code isOptional} and {@code isNamed} can be
   * {@code true}.
   * 
   * <pre>
   * defaultFormalParameter ::=
   *     normalFormalParameter ('=' expression)?
   * 
   * defaultNamedParameter ::=
   *     normalFormalParameter (':' expression)?
   * </pre>
   * 
   * @param kind the kind of parameter being expected based on the presence or absence of group
   *          delimiters
   * @return the formal parameter that was parsed
   */
  private FormalParameter parseFormalParameter(ParameterKind kind) {
    NormalFormalParameter parameter = parseNormalFormalParameter();
    if (matches(TokenType.EQ)) {
      Token seperator = getAndAdvance();
      Expression defaultValue = parseExpression();
      if (kind == ParameterKind.NAMED) {
        reportError(ParserErrorCode.WRONG_SEPARATOR_FOR_NAMED_PARAMETER, seperator);
      } else if (kind == ParameterKind.REQUIRED) {
        reportError(ParserErrorCode.POSITIONAL_PARAMETER_OUTSIDE_GROUP, parameter);
      }
      return new DefaultFormalParameter(parameter, kind, seperator, defaultValue);
    } else if (matches(TokenType.COLON)) {
      Token seperator = getAndAdvance();
      Expression defaultValue = parseExpression();
      if (kind == ParameterKind.POSITIONAL) {
        reportError(ParserErrorCode.WRONG_SEPARATOR_FOR_POSITIONAL_PARAMETER, seperator);
      } else if (kind == ParameterKind.REQUIRED) {
        reportError(ParserErrorCode.NAMED_PARAMETER_OUTSIDE_GROUP, parameter);
      }
      return new DefaultFormalParameter(parameter, kind, seperator, defaultValue);
    } else if (kind != ParameterKind.REQUIRED) {
      return new DefaultFormalParameter(parameter, kind, null, null);
    }
    return parameter;
  }

  /**
   * Parse a list of formal parameters.
   * 
   * <pre>
   * formalParameterList ::=
   *     '(' ')'
   *   | '(' normalFormalParameters (',' optionalFormalParameters)? ')'
   *   | '(' optionalFormalParameters ')'
   *
   * normalFormalParameters ::=
   *     normalFormalParameter (',' normalFormalParameter)*
   *
   * optionalFormalParameters ::=
   *     optionalPositionalFormalParameters
   *   | namedFormalParameters
   *
   * optionalPositionalFormalParameters ::=
   *     '[' defaultFormalParameter (',' defaultFormalParameter)* ']'
   *
   * namedFormalParameters ::=
   *     '{' defaultNamedParameter (',' defaultNamedParameter)* '}'
   * </pre>
   * 
   * @return the formal parameters that were parsed
   */
  private FormalParameterList parseFormalParameterList() {
    Token leftParenthesis = expect(TokenType.OPEN_PAREN);
    if (matches(TokenType.CLOSE_PAREN)) {
      return new FormalParameterList(leftParenthesis, null, null, null, getAndAdvance());
    }
    //
    // Even though it is invalid to have default parameters outside of brackets, required parameters
    // inside of brackets, or multiple groups of default and named parameters, we allow all of these
    // cases so that we can recover better.
    //
    List<FormalParameter> parameters = new ArrayList<FormalParameter>();
    List<FormalParameter> normalParameters = new ArrayList<FormalParameter>();
    List<FormalParameter> positionalParameters = new ArrayList<FormalParameter>();
    List<FormalParameter> namedParameters = new ArrayList<FormalParameter>();
    List<FormalParameter> currentParameters = normalParameters;

    Token leftSquareBracket = null;
    Token rightSquareBracket = null;
    Token leftCurlyBracket = null;
    Token rightCurlyBracket = null;

    ParameterKind kind = ParameterKind.REQUIRED;
    boolean firstParameter = true;
    boolean reportedMuliplePositionalGroups = false;
    boolean reportedMulipleNamedGroups = false;
    boolean reportedMixedGroups = false;
    Token initialToken = null;
    do {
      if (firstParameter) {
        firstParameter = false;
      } else if (!optional(TokenType.COMMA)) {
        if (((BeginToken) leftParenthesis).getEndToken() != null) {
          reportError(ParserErrorCode.EXPECTED_TOKEN, TokenType.COMMA.getLexeme());
        } else {
          // TODO(brianwilkerson) Report an error.
          // reportError(ParserErrorCode.MISSING_CLOSING_PARENTHESIS);
          break;
        }
      }
      initialToken = currentToken;
      //
      // Handle the beginning of parameter groups.
      //
      if (matches(TokenType.OPEN_SQUARE_BRACKET)) {
        if (leftSquareBracket != null && !reportedMuliplePositionalGroups) {
          reportError(ParserErrorCode.MULTIPLE_POSITIONAL_PARAMETER_GROUPS);
          reportedMuliplePositionalGroups = true;
        }
        if (leftCurlyBracket != null && !reportedMixedGroups) {
          reportError(ParserErrorCode.MIXED_PARAMETER_GROUPS);
          reportedMixedGroups = true;
        }
        leftSquareBracket = getAndAdvance();
        currentParameters = positionalParameters;
        kind = ParameterKind.POSITIONAL;
      } else if (matches(TokenType.OPEN_CURLY_BRACKET)) {
        if (leftCurlyBracket != null && !reportedMulipleNamedGroups) {
          reportError(ParserErrorCode.MULTIPLE_NAMED_PARAMETER_GROUPS);
          reportedMulipleNamedGroups = true;
        }
        if (leftSquareBracket != null && !reportedMixedGroups) {
          reportError(ParserErrorCode.MIXED_PARAMETER_GROUPS);
          reportedMixedGroups = true;
        }
        leftCurlyBracket = getAndAdvance();
        currentParameters = namedParameters;
        kind = ParameterKind.NAMED;
      }
      //
      // Parse and record the parameter.
      //
      FormalParameter parameter = parseFormalParameter(kind);
      parameters.add(parameter);
      currentParameters.add(parameter);
      //
      // Handle the end of parameter groups.
      //
      // TODO(brianwilkerson) Improve the detection and reporting of missing and mismatched delimiters.
      if (matches(TokenType.CLOSE_SQUARE_BRACKET)) {
        rightSquareBracket = getAndAdvance();
        currentParameters = normalParameters;
        if (leftSquareBracket == null) {
          // TODO(brianwilkerson) Report an error.
          // reportError(ParserErrorCode.);
        }
        kind = ParameterKind.REQUIRED;
      } else if (matches(TokenType.CLOSE_CURLY_BRACKET)) {
        rightCurlyBracket = getAndAdvance();
        currentParameters = normalParameters;
        if (leftCurlyBracket == null) {
          // TODO(brianwilkerson) Report an error.
          // reportError(ParserErrorCode.);
        }
        kind = ParameterKind.REQUIRED;
      }
    } while (!matches(TokenType.CLOSE_PAREN) && initialToken != currentToken);
    Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
    //
    // Check that the groups were closed correctly.
    //
    if (leftSquareBracket != null && rightSquareBracket == null) {
      // TODO(brianwilkerson) Report an error.
      // reportError(ParserErrorCode.?);
    }
    if (leftCurlyBracket != null && rightCurlyBracket == null) {
      // TODO(brianwilkerson) Report an error.
      // reportError(ParserErrorCode.?);
    }
    //
    // Build the parameter list.
    //
    if (leftSquareBracket == null) {
      leftSquareBracket = leftCurlyBracket;
    }
    if (rightSquareBracket == null) {
      rightSquareBracket = rightCurlyBracket;
    }
    return new FormalParameterList(
        leftParenthesis,
        parameters,
        leftSquareBracket,
        rightSquareBracket,
        rightParenthesis);
  }

  /**
   * Parse a for statement.
   * 
   * <pre>
   * forStatement ::=
   *     'for' '(' forLoopParts ')' statement
   * 
   * forLoopParts ::=
   *     forInitializerStatement expression? ';' expressionList?
   *   | declaredIdentifier 'in' expression
   *   | identifier 'in' expression
   * 
   * forInitializerStatement ::=
   *     variableDeclarationList ';'
   *   | expression? ';'
   * </pre>
   * 
   * @return the for statement that was parsed
   */
  private Statement parseForStatement() {
    boolean wasInLoop = inLoop;
    inLoop = true;
    try {
      Token forKeyword = expect(Keyword.FOR);
      Token leftParenthesis = expect(TokenType.OPEN_PAREN);
      VariableDeclarationList variableList = null;
      Expression initialization = null;
      if (!matches(TokenType.SEMICOLON)) {
        if (matchesIdentifier() && matches(peek(), Keyword.IN)) {
          List<VariableDeclaration> variables = new ArrayList<VariableDeclaration>();
          SimpleIdentifier variableName = parseSimpleIdentifier();
          variables.add(new VariableDeclaration(null, null, variableName, null, null));
          variableList = new VariableDeclarationList(null, null, variables);
        } else if (isInitializedVariableDeclaration()) {
          variableList = parseVariableDeclarationList();
        } else {
          initialization = parseExpression();
        }
        if (matches(Keyword.IN)) {
          SimpleFormalParameter loopParameter = null;
          if (variableList == null) {
            // We found: <expression> 'in'
            reportError(ParserErrorCode.MISSING_VARIABLE_IN_FOR_EACH);
          } else {
            NodeList<VariableDeclaration> variables = variableList.getVariables();
            if (variables.size() > 1) {
              reportError(
                  ParserErrorCode.MULTIPLE_VARIABLES_IN_FOR_EACH,
                  Integer.toString(variables.size()));
            }
            VariableDeclaration variable = variables.get(0);
            if (variable.getInitializer() != null) {
              reportError(ParserErrorCode.INITIALIZED_VARIABLE_IN_FOR_EACH);
            }
            loopParameter = new SimpleFormalParameter(
                null,
                null,
                variableList.getKeyword(),
                variableList.getType(),
                variable.getName());
          }
          Token inKeyword = expect(Keyword.IN);
          Expression iterator = parseExpression();
          Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
          Statement body = parseStatement();
          return new ForEachStatement(
              forKeyword,
              leftParenthesis,
              loopParameter,
              inKeyword,
              iterator,
              rightParenthesis,
              body);
        }
      }
      Token leftSeparator = expect(TokenType.SEMICOLON);
      Expression condition = null;
      if (!matches(TokenType.SEMICOLON)) {
        condition = parseExpression();
      }
      Token rightSeparator = expect(TokenType.SEMICOLON);
      List<Expression> updaters = null;
      if (!matches(TokenType.CLOSE_PAREN)) {
        updaters = parseExpressionList();
      }
      Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
      Statement body = parseStatement();
      return new ForStatement(
          forKeyword,
          leftParenthesis,
          variableList,
          initialization,
          leftSeparator,
          condition,
          rightSeparator,
          updaters,
          rightParenthesis,
          body);
    } finally {
      inLoop = wasInLoop;
    }
  }

  /**
   * Parse a function body.
   * 
   * <pre>
   * functionBody ::=
   *     '=>' expression ';'
   *   | block
   * 
   * functionExpressionBody ::=
   *     '=>' expression
   *   | block
   * </pre>
   * 
   * @param mayBeEmpty {@code true} if the function body is allowed to be empty
   * @param inExpression {@code true} if the function body is being parsed as part of an expression
   *          and therefore does not have a terminating semicolon
   * @return the function body that was parsed
   */
  private FunctionBody parseFunctionBody(boolean mayBeEmpty, boolean inExpression) {
    boolean wasInLoop = inLoop;
    boolean wasInSwitch = inSwitch;
    inLoop = false;
    inSwitch = false;
    try {
      if (matches(TokenType.SEMICOLON)) {
        if (!mayBeEmpty) {
          reportError(ParserErrorCode.MISSING_FUNCTION_BODY);
        }
        return new EmptyFunctionBody(getAndAdvance());
      } else if (matches(TokenType.FUNCTION)) {
        Token functionDefinition = getAndAdvance();
        Expression expression = parseExpression();
        Token semicolon = null;
        if (!inExpression) {
          semicolon = expect(TokenType.SEMICOLON);
        }
        return new ExpressionFunctionBody(functionDefinition, expression, semicolon);
      } else if (matches(TokenType.OPEN_CURLY_BRACKET)) {
        return new BlockFunctionBody(parseBlock());
      } else {
        // Invalid function body
        reportError(ParserErrorCode.MISSING_FUNCTION_BODY);
        return new EmptyFunctionBody(createSyntheticToken(TokenType.SEMICOLON));
      }
    } finally {
      inLoop = wasInLoop;
      inSwitch = wasInSwitch;
    }
  }

  /**
   * Parse a function declaration.
   * 
   * <pre>
   * functionDeclaration ::=
   *     functionSignature functionBody
   *   | returnType? getOrSet identifier formalParameterList functionBody
   * </pre>
   * 
   * @param commentAndMetadata the documentation comment and metadata to be associated with the
   *          declaration
   * @param externalKeyword the 'external' keyword, or {@code null} if the function is not external
   * @param returnType the return type, or {@code null} if there is no return type
   * @param isStatement {@code true} if the function declaration is being parsed as a statement
   * @return the function declaration that was parsed
   */
  private FunctionDeclaration parseFunctionDeclaration(CommentAndMetadata commentAndMetadata,
      Token externalKeyword, TypeName returnType, boolean isStatement) {
    Token keyword = null;
    boolean isGetter = false;
    if (matches(Keyword.GET) && !matches(peek(), TokenType.OPEN_PAREN)) {
      keyword = getAndAdvance();
      isGetter = true;
    } else if (matches(Keyword.SET) && !matches(peek(), TokenType.OPEN_PAREN)) {
      keyword = getAndAdvance();
    }
    SimpleIdentifier name = parseSimpleIdentifier();
    FormalParameterList parameters = null;
    if (!isGetter) {
      if (matches(TokenType.OPEN_PAREN)) {
        parameters = parseFormalParameterList();
        validateFormalParameterList(parameters);
      } else {
        reportError(ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
      }
    } else if (matches(TokenType.OPEN_PAREN)) {
      reportError(ParserErrorCode.GETTER_WITH_PARAMETERS);
      parseFormalParameterList();
    }
    FunctionBody body = null;
    if (externalKeyword == null) {
      body = parseFunctionBody(false, false);
    }
    if (!isStatement && matches(TokenType.SEMICOLON)) {
      // TODO(brianwilkerson) Improve this error message.
      reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken.getLexeme());
      advance();
    }
    return new FunctionDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        externalKeyword,
        returnType,
        keyword,
        name,
        new FunctionExpression(parameters, body));
  }

  /**
   * Parse a function declaration statement.
   * 
   * <pre>
   * functionDeclarationStatement ::=
   *     functionSignature functionBody
   * </pre>
   * 
   * @return the function declaration statement that was parsed
   */
  private Statement parseFunctionDeclarationStatement() {
    return parseFunctionDeclarationStatement(parseCommentAndMetadata(), parseOptionalReturnType());
  }

  /**
   * Parse a function declaration statement.
   * 
   * <pre>
   * functionDeclarationStatement ::=
   *     functionSignature functionBody
   * </pre>
   * 
   * @param commentAndMetadata the documentation comment and metadata to be associated with the
   *          declaration
   * @param returnType the return type, or {@code null} if there is no return type
   * @return the function declaration statement that was parsed
   */
  private Statement parseFunctionDeclarationStatement(CommentAndMetadata commentAndMetadata,
      TypeName returnType) {
    return new FunctionDeclarationStatement(parseFunctionDeclaration(
        commentAndMetadata,
        null,
        returnType,
        true));
  }

  /**
   * Parse a function expression.
   * 
   * <pre>
   * functionExpression ::=
   *     (returnType? identifier)? formalParameterList functionExpressionBody
   * </pre>
   * 
   * @return the function expression that was parsed
   */
  private FunctionExpression parseFunctionExpression() {
    FormalParameterList parameters = parseFormalParameterList();
    validateFormalParameterList(parameters);
    FunctionBody body = parseFunctionBody(false, true);
    return new FunctionExpression(parameters, body);
  }

  /**
   * Parse a getter.
   * 
   * <pre>
   * getter ::=
   *     getterSignature functionBody?
   *
   * getterSignature ::=
   *     'external'? 'static'? returnType? 'get' identifier
   * </pre>
   * 
   * @param commentAndMetadata the documentation comment and metadata to be associated with the
   *          declaration
   * @param externalKeyword the 'external' token
   * @param staticKeyword the static keyword, or {@code null} if the getter is not static
   * @param the return type that has already been parsed, or {@code null} if there was no return
   *          type
   * @return the getter that was parsed
   */
  private MethodDeclaration parseGetter(CommentAndMetadata commentAndMetadata,
      Token externalKeyword, Token staticKeyword, TypeName returnType) {
    Token propertyKeyword = expect(Keyword.GET);
    SimpleIdentifier name = parseSimpleIdentifier();
    if (matches(TokenType.OPEN_PAREN) && matches(peek(), TokenType.CLOSE_PAREN)) {
      reportError(ParserErrorCode.GETTER_WITH_PARAMETERS);
      advance();
      advance();
    }
    FunctionBody body = parseFunctionBody(true, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      reportError(ParserErrorCode.EXTERNAL_GETTER_WITH_BODY);
    }
    return new MethodDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        externalKeyword,
        staticKeyword,
        returnType,
        propertyKeyword,
        null,
        name,
        null,
        body);
  }

  /**
   * Parse a list of identifiers.
   * 
   * <pre>
   * identifierList ::=
   *     identifier (',' identifier)*
   * </pre>
   * 
   * @return the list of identifiers that were parsed
   */
  private List<SimpleIdentifier> parseIdentifierList() {
    List<SimpleIdentifier> identifiers = new ArrayList<SimpleIdentifier>();
    identifiers.add(parseSimpleIdentifier());
    while (matches(TokenType.COMMA)) {
      advance();
      identifiers.add(parseSimpleIdentifier());
    }
    return identifiers;
  }

  /**
   * Parse an if statement.
   * 
   * <pre>
   * ifStatement ::=
   *     'if' '(' expression ')' statement ('else' statement)?
   * </pre>
   * 
   * @return the if statement that was parsed
   */
  private Statement parseIfStatement() {
    Token ifKeyword = expect(Keyword.IF);
    Token leftParenthesis = expect(TokenType.OPEN_PAREN);
    Expression condition = parseExpression();
    Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
    Statement thenStatement = parseStatement();
    Token elseKeyword = null;
    Statement elseStatement = null;
    if (matches(Keyword.ELSE)) {
      elseKeyword = getAndAdvance();
      elseStatement = parseStatement();
    }
    return new IfStatement(
        ifKeyword,
        leftParenthesis,
        condition,
        rightParenthesis,
        thenStatement,
        elseKeyword,
        elseStatement);
  }

  /**
   * Parse an implements clause.
   * 
   * <pre>
   * implementsClause ::=
   *     'implements' type (',' type)*
   * </pre>
   * 
   * @return the implements clause that was parsed
   */
  private ImplementsClause parseImplementsClause() {
    Token keyword = expect(Keyword.IMPLEMENTS);
    List<TypeName> interfaces = new ArrayList<TypeName>();
    interfaces.add(parseTypeName());
    while (optional(TokenType.COMMA)) {
      interfaces.add(parseTypeName());
    }
    return new ImplementsClause(keyword, interfaces);
  }

  /**
   * Parse an import directive.
   * 
   * <pre>
   * importDirective ::=
   *     metadata 'import' stringLiteral ('as' identifier)? combinator*';'
   * </pre>
   * 
   * @param commentAndMetadata the metadata to be associated with the directive
   * @return the import directive that was parsed
   */
  private ImportDirective parseImportDirective(CommentAndMetadata commentAndMetadata) {
    Token importKeyword = expect(Keyword.IMPORT);
    StringLiteral libraryUri = parseStringLiteral();
    Token asToken = null;
    SimpleIdentifier prefix = null;
    if (matches(Keyword.AS)) {
      asToken = getAndAdvance();
      prefix = parseSimpleIdentifier();
    }
    List<Combinator> combinators = parseCombinators();
    Token semicolon = expect(TokenType.SEMICOLON);
    return new ImportDirective(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        importKeyword,
        libraryUri,
        asToken,
        prefix,
        combinators,
        semicolon);
  }

  /**
   * Parse a list of initialized identifiers.
   * 
   * <pre>
   * ?? ::=
   *     'static'? ('var' | type) initializedIdentifierList ';'
   *   | 'final' type? initializedIdentifierList ';'
   * 
   * initializedIdentifierList ::=
   *     initializedIdentifier (',' initializedIdentifier)*
   * 
   * initializedIdentifier ::=
   *     identifier ('=' expression)?
   * </pre>
   * 
   * @param commentAndMetadata the documentation comment and metadata to be associated with the
   *          declaration
   * @param staticKeyword the static keyword, or {@code null} if the getter is not static
   * @param keyword the token representing the 'final', 'const' or 'var' keyword, or {@code null} if
   *          there is no keyword
   * @param type the type that has already been parsed, or {@code null} if 'var' was provided
   * @return the getter that was parsed
   */
  private FieldDeclaration parseInitializedIdentifierList(CommentAndMetadata commentAndMetadata,
      Token staticKeyword, Token keyword, TypeName type) {
    VariableDeclarationList fieldList = parseVariableDeclarationList(keyword, type);
    return new FieldDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        staticKeyword,
        fieldList,
        expect(TokenType.SEMICOLON));
  }

  /**
   * Parse an instance creation expression.
   * 
   * <pre>
   * instanceCreationExpression ::=
   *     ('new' | 'const') type ('.' identifier)? argumentList
   * </pre>
   * 
   * @param keyword the 'new' or 'const' keyword that introduces the expression
   * @return the instance creation expression that was parsed
   */
  private InstanceCreationExpression parseInstanceCreationExpression(Token keyword) {
    ConstructorName constructorName = parseConstructorName();
    ArgumentList argumentList = parseArgumentList();
    return new InstanceCreationExpression(keyword, constructorName, argumentList);
  }

  /**
   * Parse a library directive.
   * 
   * <pre>
   * libraryDirective ::=
   *     metadata 'library' identifier ';'
   * </pre>
   * 
   * @param commentAndMetadata the metadata to be associated with the directive
   * @return the library directive that was parsed
   */
  private LibraryDirective parseLibraryDirective(CommentAndMetadata commentAndMetadata) {
    Token keyword = expect(Keyword.LIBRARY);
    SimpleIdentifier libraryName = parseLibraryName(
        ParserErrorCode.MISSING_NAME_IN_LIBRARY_DIRECTIVE,
        keyword);
    Token semicolon = expect(TokenType.SEMICOLON);
    return new LibraryDirective(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        keyword,
        libraryName,
        semicolon);
  }

  /**
   * Parse a library name.
   * 
   * <pre>
   * libraryName ::=
   *     identifier
   * </pre>
   * 
   * @param missingNameError the error code to be used if the library name is missing
   * @param missingNameToken the token associated with the error produced if the library name is
   *          missing
   * @return the library name that was parsed
   */
  private SimpleIdentifier parseLibraryName(ParserErrorCode missingNameError, Token missingNameToken) {
    if (matchesIdentifier()) {
      return parseSimpleIdentifier();
    } else if (matches(TokenType.STRING)) {
      // TODO(brianwilkerson) Recovery: This should be extended to handle arbitrary tokens until we
      // can find a token that can start a compilation unit member.
      StringLiteral string = parseStringLiteral();
      reportError(ParserErrorCode.NON_IDENTIFIER_LIBRARY_NAME, string);
    } else {
      reportError(missingNameError, missingNameToken);
    }
    return createSyntheticIdentifier();
  }

  /**
   * Parse a list literal.
   * 
   * <pre>
   * listLiteral ::=
   *     'const'? typeArguments? '[' (expressionList ','?)? ']'
   * </pre>
   * 
   * @param modifier the 'const' modifier appearing before the literal, or {@code null} if there is
   *          no modifier
   * @param typeArguments the type arguments appearing before the literal, or {@code null} if there
   *          are no type arguments
   * @return the list literal that was parsed
   */
  private ListLiteral parseListLiteral(Token modifier, TypeArgumentList typeArguments) {
    if (matches(TokenType.INDEX)) {
      BeginToken leftBracket = new BeginToken(
          TokenType.OPEN_SQUARE_BRACKET,
          currentToken.getOffset());
      Token rightBracket = new Token(TokenType.CLOSE_SQUARE_BRACKET, currentToken.getOffset() + 1);
      leftBracket.setEndToken(rightBracket);
      rightBracket.setNext(currentToken.getNext());
      leftBracket.setNext(rightBracket);
      currentToken.getPrevious().setNext(leftBracket);
      currentToken = currentToken.getNext();
      return new ListLiteral(modifier, typeArguments, leftBracket, null, rightBracket);
    }
    Token leftBracket = expect(TokenType.OPEN_SQUARE_BRACKET);
    if (matches(TokenType.CLOSE_SQUARE_BRACKET)) {
      return new ListLiteral(modifier, typeArguments, leftBracket, null, getAndAdvance());
    }
    List<Expression> elements = new ArrayList<Expression>();
    elements.add(parseExpression());
    while (optional(TokenType.COMMA)) {
      if (matches(TokenType.CLOSE_SQUARE_BRACKET)) {
        return new ListLiteral(modifier, typeArguments, leftBracket, elements, getAndAdvance());
      }
      elements.add(parseExpression());
    }
    Token rightBracket = expect(TokenType.CLOSE_SQUARE_BRACKET);
    return new ListLiteral(modifier, typeArguments, leftBracket, elements, rightBracket);
  }

  /**
   * Parse a list or map literal.
   * 
   * <pre>
   * listOrMapLiteral ::=
   *     listLiteral
   *   | mapLiteral
   * </pre>
   * 
   * @param modifier the 'const' modifier appearing before the literal, or {@code null} if there is
   *          no modifier
   * @return the list or map literal that was parsed
   */
  private TypedLiteral parseListOrMapLiteral(Token modifier) {
    TypeArgumentList typeArguments = null;
    if (matches(TokenType.LT)) {
      typeArguments = parseTypeArgumentList();
    }
    if (matches(TokenType.OPEN_CURLY_BRACKET)) {
      return parseMapLiteral(modifier, typeArguments);
    } else if (matches(TokenType.OPEN_SQUARE_BRACKET) || matches(TokenType.INDEX)) {
      return parseListLiteral(modifier, typeArguments);
    }
    reportError(ParserErrorCode.EXPECTED_LIST_OR_MAP_LITERAL);
    return new ListLiteral(
        modifier,
        typeArguments,
        createSyntheticToken(TokenType.OPEN_SQUARE_BRACKET),
        null,
        createSyntheticToken(TokenType.CLOSE_SQUARE_BRACKET));
  }

  /**
   * Parse a logical and expression.
   * 
   * <pre>
   * logicalAndExpression ::=
   *     bitwiseOrExpression ('&&' bitwiseOrExpression)*
   * </pre>
   * 
   * @return the logical and expression that was parsed
   */
  private Expression parseLogicalAndExpression() {
    Expression expression = parseBitwiseOrExpression();
    while (matches(TokenType.AMPERSAND_AMPERSAND)) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseBitwiseOrExpression());
    }
    return expression;
  }

  /**
   * Parse a logical or expression.
   * 
   * <pre>
   * logicalOrExpression ::=
   *     logicalAndExpression ('||' logicalAndExpression)*
   * </pre>
   * 
   * @return the logical or expression that was parsed
   */
  private Expression parseLogicalOrExpression() {
    Expression expression = parseLogicalAndExpression();
    while (matches(TokenType.BAR_BAR)) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseLogicalAndExpression());
    }
    return expression;
  }

  /**
   * Parse a map literal.
   * 
   * <pre>
   * mapLiteral ::=
   *     'const'? typeArguments? '{' (mapLiteralEntry (',' mapLiteralEntry)* ','?)? '}'
   * </pre>
   * 
   * @param modifier the 'const' modifier appearing before the literal, or {@code null} if there is
   *          no modifier
   * @param typeArguments the type arguments that were declared, or {@code null} if there are no
   *          type arguments
   * @return the map literal that was parsed
   */
  private MapLiteral parseMapLiteral(Token modifier, TypeArgumentList typeArguments) {
    Token leftBracket = expect(TokenType.OPEN_CURLY_BRACKET);
    List<MapLiteralEntry> entries = new ArrayList<MapLiteralEntry>();
    if (matches(TokenType.CLOSE_CURLY_BRACKET)) {
      return new MapLiteral(modifier, typeArguments, leftBracket, entries, getAndAdvance());
    }
    entries.add(parseMapLiteralEntry());
    while (optional(TokenType.COMMA)) {
      if (matches(TokenType.CLOSE_CURLY_BRACKET)) {
        return new MapLiteral(modifier, typeArguments, leftBracket, entries, getAndAdvance());
      }
      entries.add(parseMapLiteralEntry());
    }
    Token rightBracket = expect(TokenType.CLOSE_CURLY_BRACKET);
    return new MapLiteral(modifier, typeArguments, leftBracket, entries, rightBracket);
  }

  /**
   * Parse a map literal entry.
   * 
   * <pre>
   * mapLiteralEntry ::=
   *     stringLiteral ':' expression
   * </pre>
   * 
   * @return the map literal entry that was parsed
   */
  private MapLiteralEntry parseMapLiteralEntry() {
    StringLiteral key = parseStringLiteral();
    Token separator = expect(TokenType.COLON);
    Expression value = parseExpression();
    return new MapLiteralEntry(key, separator, value);
  }

  /**
   * Parse a method declaration.
   * 
   * <pre>
   * functionDeclaration ::=
   *     'external'? 'static'? functionSignature functionBody
   *   | 'external'? functionSignature ';'
   * </pre>
   * 
   * @param commentAndMetadata the documentation comment and metadata to be associated with the
   *          declaration
   * @param externalKeyword the 'external' token
   * @param staticKeyword the static keyword, or {@code null} if the getter is not static
   * @param returnType the return type of the method
   * @return the method declaration that was parsed
   */
  private MethodDeclaration parseMethodDeclaration(CommentAndMetadata commentAndMetadata,
      Token externalKeyword, Token staticKeyword, TypeName returnType) {
    SimpleIdentifier methodName = parseSimpleIdentifier();
    FormalParameterList parameters = parseFormalParameterList();
    validateFormalParameterList(parameters);
    return parseMethodDeclaration(
        commentAndMetadata,
        externalKeyword,
        staticKeyword,
        returnType,
        methodName,
        parameters);
  }

  /**
   * Parse a method declaration.
   * 
   * <pre>
   * functionDeclaration ::=
   *     ('external' 'static'?)? functionSignature functionBody
   *   | 'external'? functionSignature ';'
   * </pre>
   * 
   * @param commentAndMetadata the documentation comment and metadata to be associated with the
   *          declaration
   * @param externalKeyword the 'external' token
   * @param staticKeyword the static keyword, or {@code null} if the getter is not static
   * @param returnType the return type of the method
   * @param name the name of the method
   * @param parameters the parameters to the method
   * @return the method declaration that was parsed
   */
  private MethodDeclaration parseMethodDeclaration(CommentAndMetadata commentAndMetadata,
      Token externalKeyword, Token staticKeyword, TypeName returnType, SimpleIdentifier name,
      FormalParameterList parameters) {
    FunctionBody body = parseFunctionBody(externalKeyword != null || staticKeyword == null, false);
    if (externalKeyword != null) {
      if (!(body instanceof EmptyFunctionBody)) {
        reportError(ParserErrorCode.EXTERNAL_METHOD_WITH_BODY, body);
      }
    } else if (staticKeyword != null) {
      if (body instanceof EmptyFunctionBody) {
        reportError(ParserErrorCode.ABSTRACT_STATIC_METHOD, body);
      }
    }
    return new MethodDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        externalKeyword,
        staticKeyword,
        returnType,
        null,
        null,
        name,
        parameters,
        body);
  }

  /**
   * Parse the modifiers preceding a declaration. This method allows the modifiers to appear in any
   * order but does generate errors for duplicated modifiers. Checks for other problems, such as
   * having the modifiers appear in the wrong order or specifying both 'const' and 'final', are
   * reported in one of the methods whose name is prefixed with {@code validateModifiersFor}.
   * 
   * <pre>
   * modifiers ::=
   *     ('abstract' | 'const' | 'external' | 'factory' | 'final' | 'static' | 'var')*
   * </pre>
   * 
   * @return the modifiers that were parsed
   */
  private Modifiers parseModifiers() {
    Modifiers modifiers = new Modifiers();
    boolean progress = true;
    while (progress) {
      if (matches(Keyword.ABSTRACT)) {
        if (modifiers.getAbstractKeyword() != null) {
          reportError(ParserErrorCode.DUPLICATED_MODIFIER, currentToken.getLexeme());
          advance();
        } else {
          modifiers.setAbstractKeyword(getAndAdvance());
        }
      } else if (matches(Keyword.CONST)) {
        if (modifiers.getConstKeyword() != null) {
          reportError(ParserErrorCode.DUPLICATED_MODIFIER, currentToken.getLexeme());
          advance();
        } else {
          modifiers.setConstKeyword(getAndAdvance());
        }
      } else if (matches(Keyword.EXTERNAL)) {
        if (modifiers.getExternalKeyword() != null) {
          reportError(ParserErrorCode.DUPLICATED_MODIFIER, currentToken.getLexeme());
          advance();
        } else {
          modifiers.setExternalKeyword(getAndAdvance());
        }
      } else if (matches(Keyword.FACTORY)) {
        if (modifiers.getFactoryKeyword() != null) {
          reportError(ParserErrorCode.DUPLICATED_MODIFIER, currentToken.getLexeme());
          advance();
        } else {
          modifiers.setFactoryKeyword(getAndAdvance());
        }
      } else if (matches(Keyword.FINAL)) {
        if (modifiers.getFinalKeyword() != null) {
          reportError(ParserErrorCode.DUPLICATED_MODIFIER, currentToken.getLexeme());
          advance();
        } else {
          modifiers.setFinalKeyword(getAndAdvance());
        }
      } else if (matches(Keyword.STATIC)) {
        if (modifiers.getStaticKeyword() != null) {
          reportError(ParserErrorCode.DUPLICATED_MODIFIER, currentToken.getLexeme());
          advance();
        } else {
          modifiers.setStaticKeyword(getAndAdvance());
        }
      } else if (matches(Keyword.VAR)) {
        if (modifiers.getVarKeyword() != null) {
          reportError(ParserErrorCode.DUPLICATED_MODIFIER, currentToken.getLexeme());
          advance();
        } else {
          modifiers.setVarKeyword(getAndAdvance());
        }
      } else {
        progress = false;
      }
    }
    return modifiers;
  }

  /**
   * Parse a multiplicative expression.
   * 
   * <pre>
   * multiplicativeExpression ::=
   *     unaryExpression (multiplicativeOperator unaryExpression)*
   *   | 'super' (multiplicativeOperator unaryExpression)+
   * </pre>
   * 
   * @return the multiplicative expression that was parsed
   */
  private Expression parseMultiplicativeExpression() {
    Expression expression;
    if (matches(Keyword.SUPER) && currentToken.getNext().getType().isMultiplicativeOperator()) {
      expression = new SuperExpression(getAndAdvance());
    } else {
      expression = parseUnaryExpression();
    }
    while (currentToken.getType().isMultiplicativeOperator()) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseUnaryExpression());
    }
    return expression;
  }

  /**
   * Parse a new expression.
   * 
   * <pre>
   * newExpression ::=
   *     instanceCreationExpression
   * </pre>
   * 
   * @return the new expression that was parsed
   */
  private InstanceCreationExpression parseNewExpression() {
    return parseInstanceCreationExpression(expect(Keyword.NEW));
  }

  /**
   * Parse a non-labeled statement.
   * 
   * <pre>
   * nonLabeledStatement ::=
   *     block
   *   | assertStatement
   *   | breakStatement
   *   | continueStatement
   *   | doStatement
   *   | forStatement
   *   | ifStatement
   *   | returnStatement
   *   | switchStatement
   *   | tryStatement
   *   | whileStatement
   *   | variableDeclarationList ';'
   *   | expressionStatement
   *   | functionSignature functionBody
   * </pre>
   * 
   * @return the non-labeled statement that was parsed
   */
  private Statement parseNonLabeledStatement() {
    // TODO(brianwilkerson) Pass the comment and metadata on where appropriate.
    CommentAndMetadata commentAndMetadata = parseCommentAndMetadata();
    if (matches(TokenType.OPEN_CURLY_BRACKET)) {
      if (matches(peek(), TokenType.STRING)) {
        Token afterString = skipStringLiteral(currentToken.getNext());
        if (afterString != null && afterString.getType() == TokenType.COLON) {
          return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
        }
      }
      return parseBlock();
    } else if (matches(TokenType.KEYWORD)
        && !((KeywordToken) currentToken).getKeyword().isPseudoKeyword()) {
      Keyword keyword = ((KeywordToken) currentToken).getKeyword();
      if (keyword == Keyword.ASSERT) {
        return parseAssertStatement();
      } else if (keyword == Keyword.BREAK) {
        return parseBreakStatement();
      } else if (keyword == Keyword.CONTINUE) {
        return parseContinueStatement();
      } else if (keyword == Keyword.DO) {
        return parseDoStatement();
      } else if (keyword == Keyword.FOR) {
        return parseForStatement();
      } else if (keyword == Keyword.IF) {
        return parseIfStatement();
      } else if (keyword == Keyword.RETURN) {
        return parseReturnStatement();
      } else if (keyword == Keyword.SWITCH) {
        return parseSwitchStatement();
      } else if (keyword == Keyword.THROW) {
        return new ExpressionStatement(parseThrowExpression(), expect(TokenType.SEMICOLON));
      } else if (keyword == Keyword.TRY) {
        return parseTryStatement();
      } else if (keyword == Keyword.WHILE) {
        return parseWhileStatement();
      } else if (keyword == Keyword.VAR || keyword == Keyword.FINAL) {
        return parseVariableDeclarationStatement();
      } else if (keyword == Keyword.VOID) {
        TypeName returnType = parseReturnType();
        if (matchesIdentifier()
            && matchesAny(
                peek(),
                TokenType.OPEN_PAREN,
                TokenType.OPEN_CURLY_BRACKET,
                TokenType.FUNCTION)) {
          return parseFunctionDeclarationStatement(commentAndMetadata, returnType);
        } else {
          // We have found an error of some kind. Try to recover.
          if (matchesIdentifier()) {
            if (matchesAny(peek(), TokenType.EQ, TokenType.COMMA, TokenType.SEMICOLON)) {
              // We appear to have a variable declaration with a type of "void".
              reportError(ParserErrorCode.VOID_VARIABLE, returnType);
              return parseVariableDeclarationStatement();
            }
          }
          // TODO(brianwilkerson) Report this error.
          return null;
        }
      } else if (keyword == Keyword.CONST) {
        if (matchesAny(
            peek(),
            TokenType.LT,
            TokenType.OPEN_CURLY_BRACKET,
            TokenType.OPEN_SQUARE_BRACKET,
            TokenType.INDEX)) {
          return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
        } else if (matches(peek(), TokenType.IDENTIFIER)) {
          Token afterType = skipTypeName(peek());
          if (afterType != null) {
            if (matches(afterType, TokenType.OPEN_PAREN)
                || (matches(afterType, TokenType.PERIOD)
                    && matches(afterType.getNext(), TokenType.IDENTIFIER) && matches(
                      afterType.getNext().getNext(),
                      TokenType.OPEN_PAREN))) {
              return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
            }
          }
        }
        return parseVariableDeclarationStatement();
      } else if (keyword == Keyword.NEW || keyword == Keyword.TRUE || keyword == Keyword.FALSE
          || keyword == Keyword.NULL || keyword == Keyword.SUPER || keyword == Keyword.THIS) {
        return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
      } else {
        // Expected a statement
        // TODO(brianwilkerson) Report an error.
        // reportError(ParserErrorCode.?);
        return null;
      }
    } else if (matches(TokenType.SEMICOLON)) {
      return parseEmptyStatement();
    } else if (isInitializedVariableDeclaration()) {
      return parseVariableDeclarationStatement();
    } else if (isFunctionDeclaration()) {
      return parseFunctionDeclarationStatement();
    } else {
      return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
    }
  }

  /**
   * Parse a normal formal parameter.
   * 
   * <pre>
   * normalFormalParameter ::=
   *     functionSignature
   *   | fieldFormalParameter
   *   | simpleFormalParameter
   * 
   * functionSignature:
   *     metadata returnType? identifier formalParameterList
   * 
   * fieldFormalParameter ::=
   *     metadata finalConstVarOrType? 'this' '.' identifier
   * 
   * simpleFormalParameter ::=
   *     declaredIdentifier
   *   | metadata identifier
   * </pre>
   * 
   * @return the normal formal parameter that was parsed
   */
  private NormalFormalParameter parseNormalFormalParameter() {
    CommentAndMetadata commentAndMetadata = parseCommentAndMetadata();
    FinalConstVarOrType holder = parseFinalConstVarOrType(true);
    Token thisKeyword = null;
    Token period = null;
    if (matches(Keyword.THIS)) {
      thisKeyword = getAndAdvance();
      period = expect(TokenType.PERIOD);
    }
    SimpleIdentifier identifier = parseSimpleIdentifier();
    if (matches(TokenType.OPEN_PAREN)) {
      if (thisKeyword != null) {
        // TODO(brianwilkerson) Report this error (we have found "this.id(").
        // TODO(brianwilkerson) Decide how to recover from this error.
        // reportError(ParserErrorCode.?);
      }
      FormalParameterList parameters = parseFormalParameterList();
      return new FunctionTypedFormalParameter(
          commentAndMetadata.getComment(),
          commentAndMetadata.getMetadata(),
          holder.getType(),
          identifier,
          parameters);
    }
    TypeName type = holder.getType();
    if (type != null && matches(type.getName().getBeginToken(), Keyword.VOID)) {
      reportError(ParserErrorCode.VOID_PARAMETER, type.getName().getBeginToken());
    }
    if (thisKeyword != null) {
      return new FieldFormalParameter(
          commentAndMetadata.getComment(),
          commentAndMetadata.getMetadata(),
          holder.getKeyword(),
          holder.getType(),
          thisKeyword,
          period,
          identifier);
    }
    return new SimpleFormalParameter(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        holder.getKeyword(),
        holder.getType(),
        identifier);
  }

  /**
   * Parse an operator declaration.
   * 
   * <pre>
   * operatorDeclaration ::=
   *     operatorSignature (';' | functionBody)
   *
   * operatorSignature ::=
   *     'external'? returnType? 'operator' operator formalParameterList
   * </pre>
   * 
   * @param commentAndMetadata the documentation comment and metadata to be associated with the
   *          declaration
   * @param externalKeyword the 'external' token
   * @param the return type that has already been parsed, or {@code null} if there was no return
   *          type
   * @return the operator declaration that was parsed
   */
  private MethodDeclaration parseOperator(CommentAndMetadata commentAndMetadata,
      Token externalKeyword, TypeName returnType) {
    Token operatorKeyword = expect(Keyword.OPERATOR);
    if (!currentToken.isUserDefinableOperator()) {
      reportError(ParserErrorCode.NON_USER_DEFINABLE_OPERATOR, currentToken.getLexeme());
    }
    SimpleIdentifier name = new SimpleIdentifier(getAndAdvance());
    FormalParameterList parameters = parseFormalParameterList();
    validateFormalParameterList(parameters);
    FunctionBody body = parseFunctionBody(true, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      reportError(ParserErrorCode.EXTERNAL_OPERATOR_WITH_BODY);
    }
    return new MethodDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        externalKeyword,
        null,
        returnType,
        null,
        operatorKeyword,
        name,
        parameters,
        body);
  }

  /**
   * Parse a return type if one is given, otherwise return {@code null} without advancing.
   * 
   * @return the return type that was parsed
   */
  private TypeName parseOptionalReturnType() {
    if (matches(Keyword.VOID)) {
      return parseReturnType();
    } else if (matchesIdentifier() && !matches(Keyword.GET) && !matches(Keyword.SET)
        && !matches(Keyword.OPERATOR)
        && (matchesIdentifier(peek()) || matches(peek(), TokenType.LT))) {
      return parseReturnType();
    } else if (matchesIdentifier() && matches(peek(), TokenType.PERIOD)
        && matchesIdentifier(peek(2))
        && (matchesIdentifier(peek(3)) || matches(peek(3), TokenType.LT))) {
      return parseReturnType();
    }
    return null;
  }

  /**
   * Parse a part or part-of directive.
   * 
   * <pre>
   * partDirective ::=
   *     metadata 'part' stringLiteral ';'
   * 
   * partOfDirective ::=
   *     metadata 'part' 'of' identifier ';'
   * </pre>
   * 
   * @param commentAndMetadata the metadata to be associated with the directive
   * @return the part or part-of directive that was parsed
   */
  private Directive parsePartDirective(CommentAndMetadata commentAndMetadata) {
    Token partKeyword = expect(Keyword.PART);
    if (matches(OF)) {
      Token ofKeyword = getAndAdvance();
      SimpleIdentifier libraryName = parseLibraryName(
          ParserErrorCode.MISSING_NAME_IN_PART_OF_DIRECTIVE,
          ofKeyword);
      Token semicolon = expect(TokenType.SEMICOLON);
      return new PartOfDirective(
          commentAndMetadata.getComment(),
          commentAndMetadata.getMetadata(),
          partKeyword,
          ofKeyword,
          libraryName,
          semicolon);
    }
    StringLiteral partUri = parseStringLiteral();
    Token semicolon = expect(TokenType.SEMICOLON);
    return new PartDirective(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        partKeyword,
        partUri,
        semicolon);
  }

  /**
   * Parse a postfix expression.
   * 
   * <pre>
   * postfixExpression ::=
   *     assignableExpression postfixOperator
   *   | primary selector*
   *
   * selector ::=
   *     assignableSelector
   *   | argumentList
   * </pre>
   * 
   * @return the postfix expression that was parsed
   */
  private Expression parsePostfixExpression() {
    Expression operand = parseAssignableExpression(true);
    if (matches(TokenType.OPEN_SQUARE_BRACKET) || matches(TokenType.PERIOD)
        || matches(TokenType.OPEN_PAREN)) {
      do {
        if (matches(TokenType.OPEN_PAREN)) {
          ArgumentList argumentList = parseArgumentList();
          if (operand instanceof PropertyAccess) {
            PropertyAccess access = (PropertyAccess) operand;
            operand = new MethodInvocation(
                access.getTarget(),
                access.getOperator(),
                access.getPropertyName(),
                argumentList);
          } else {
            operand = new FunctionExpressionInvocation(operand, argumentList);
          }
        } else {
          operand = parseAssignableSelector(operand, true);
        }
      } while (matches(TokenType.OPEN_SQUARE_BRACKET) || matches(TokenType.PERIOD)
          || matches(TokenType.OPEN_PAREN));
      return operand;
    }
    if (!currentToken.getType().isIncrementOperator()) {
      return operand;
    }
    if (operand instanceof FunctionExpressionInvocation) {
      reportError(ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR);
    }
    Token operator = getAndAdvance();
    return new PostfixExpression(operand, operator);
  }

  /**
   * Parse a prefixed identifier.
   * 
   * <pre>
   * prefixedIdentifier ::=
   *     identifier ('.' identifier)?
   * </pre>
   * 
   * @return the prefixed identifier that was parsed
   */
  private Identifier parsePrefixedIdentifier() {
    SimpleIdentifier qualifier = parseSimpleIdentifier();
    if (!matches(TokenType.PERIOD)) {
      return qualifier;
    }
    Token period = getAndAdvance();
    SimpleIdentifier qualified = parseSimpleIdentifier();
    return new PrefixedIdentifier(qualifier, period, qualified);
  }

  /**
   * Parse a primary expression.
   * 
   * <pre>
   * primary ::=
   *     thisExpression
   *   | 'super' assignableSelector
   *   | functionExpression
   *   | literal
   *   | identifier
   *   | newExpression
   *   | constObjectExpression
   *   | '(' expression ')'
   *   | argumentDefinitionTest
   * 
   * literal ::=
   *     nullLiteral
   *   | booleanLiteral
   *   | numericLiteral
   *   | stringLiteral
   *   | mapLiteral
   *   | listLiteral
   * </pre>
   * 
   * @return the primary expression that was parsed
   */
  private Expression parsePrimaryExpression() {
    if (matches(Keyword.THIS)) {
      return new ThisExpression(getAndAdvance());
    } else if (matches(Keyword.SUPER)) {
      return parseAssignableSelector(new SuperExpression(getAndAdvance()), false);
    } else if (matches(Keyword.NULL)) {
      return new NullLiteral(getAndAdvance());
    } else if (matches(Keyword.FALSE)) {
      return new BooleanLiteral(getAndAdvance(), false);
    } else if (matches(Keyword.TRUE)) {
      return new BooleanLiteral(getAndAdvance(), true);
    } else if (matches(TokenType.DOUBLE)) {
      Token token = getAndAdvance();
      double value = 0.0d;
      try {
        value = Double.parseDouble(token.getLexeme());
      } catch (NumberFormatException exception) {
        // The invalid format should have been reported by the scanner.
      }
      return new DoubleLiteral(token, value);
    } else if (matches(TokenType.HEXADECIMAL)) {
      Token token = getAndAdvance();
      BigInteger value = null;
      try {
        value = new BigInteger(token.getLexeme().substring(2), 16);
      } catch (NumberFormatException exception) {
        // The invalid format should have been reported by the scanner.
      }
      return new IntegerLiteral(token, value);
    } else if (matches(TokenType.INT)) {
      Token token = getAndAdvance();
      BigInteger value = null;
      try {
        value = new BigInteger(token.getLexeme());
      } catch (NumberFormatException exception) {
        // The invalid format should have been reported by the scanner.
      }
      return new IntegerLiteral(token, value);
    } else if (matches(TokenType.STRING)) {
      return parseStringLiteral();
    } else if (matches(TokenType.OPEN_CURLY_BRACKET)) {
      return parseMapLiteral(null, null);
    } else if (matches(TokenType.OPEN_SQUARE_BRACKET) || matches(TokenType.INDEX)) {
      return parseListLiteral(null, null);
    } else if (matchesIdentifier()) {
      return parsePrefixedIdentifier();
    } else if (matches(Keyword.NEW)) {
      return parseNewExpression();
    } else if (matches(Keyword.CONST)) {
      return parseConstExpression();
    } else if (matches(TokenType.OPEN_PAREN)) {
      if (isFunctionExpression(currentToken)) {
        return parseFunctionExpression();
      }
      Token leftParenthesis = getAndAdvance();
      Expression expression = parseExpression();
      Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
      return new ParenthesizedExpression(leftParenthesis, expression, rightParenthesis);
    } else if (matches(TokenType.LT)) {
      return parseListOrMapLiteral(null);
    } else if (matches(TokenType.QUESTION)) {
      return parseArgumentDefinitionTest();
    } else if (matches(Keyword.VOID)) {
      // Recover from having a return type of "void" where a return type is not expected.
      // TODO(brianwilkerson) Improve this error message.
      reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken.getLexeme());
      advance();
      return parsePrimaryExpression();
    } else {
      return createSyntheticIdentifier();
    }
  }

  /**
   * Parse a redirecting constructor invocation.
   * 
   * <pre>
   * redirectingConstructorInvocation ::=
   *     'this' ('.' identifier)? arguments
   * </pre>
   * 
   * @return the redirecting constructor invocation that was parsed
   */
  private RedirectingConstructorInvocation parseRedirectingConstructorInvocation() {
    Token keyword = expect(Keyword.THIS);
    Token period = null;
    SimpleIdentifier constructorName = null;
    if (matches(TokenType.PERIOD)) {
      period = getAndAdvance();
      constructorName = parseSimpleIdentifier();
    }
    ArgumentList argumentList = parseArgumentList();
    return new RedirectingConstructorInvocation(keyword, period, constructorName, argumentList);
  }

  /**
   * Parse a relational expression.
   * 
   * <pre>
   * relationalExpression ::=
   *     shiftExpression ('is' type | 'as' type | relationalOperator shiftExpression)?
   *   | 'super' relationalOperator shiftExpression
   * </pre>
   * 
   * @return the relational expression that was parsed
   */
  private Expression parseRelationalExpression() {
    if (matches(Keyword.SUPER) && currentToken.getNext().getType().isRelationalOperator()) {
      Expression expression = new SuperExpression(getAndAdvance());
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseShiftExpression());
      return expression;
    }
    Expression expression = parseShiftExpression();
    if (matches(Keyword.AS)) {
      Token isOperator = getAndAdvance();
      expression = new AsExpression(expression, isOperator, parseTypeName());
    } else if (matches(Keyword.IS)) {
      Token isOperator = getAndAdvance();
      Token notOperator = null;
      if (matches(TokenType.BANG)) {
        notOperator = getAndAdvance();
      }
      expression = new IsExpression(expression, isOperator, notOperator, parseTypeName());
    } else if (currentToken.getType().isRelationalOperator()) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseShiftExpression());
    }
    return expression;
  }

  /**
   * Parse a return statement.
   * 
   * <pre>
   * returnStatement ::=
   *     'return' expression? ';'
   * </pre>
   * 
   * @return the return statement that was parsed
   */
  private Statement parseReturnStatement() {
    Token returnKeyword = expect(Keyword.RETURN);
    if (matches(TokenType.SEMICOLON)) {
      return new ReturnStatement(returnKeyword, null, getAndAdvance());
    }
    Expression expression = parseExpression();
    Token semicolon = expect(TokenType.SEMICOLON);
    return new ReturnStatement(returnKeyword, expression, semicolon);
  }

  /**
   * Parse a return type.
   * 
   * <pre>
   * returnType ::=
   *     'void'
   *   | type
   * </pre>
   * 
   * @return the return type that was parsed
   */
  private TypeName parseReturnType() {
    if (matches(Keyword.VOID)) {
      return new TypeName(new SimpleIdentifier(getAndAdvance()), null);
    } else {
      return parseTypeName();
    }
  }

  /**
   * Parse a setter.
   * 
   * <pre>
   * setter ::=
   *     setterSignature functionBody?
   *
   * setterSignature ::=
   *     'external'? 'static'? returnType? 'set' identifier formalParameterList
   * </pre>
   * 
   * @param commentAndMetadata the documentation comment and metadata to be associated with the
   *          declaration
   * @param externalKeyword the 'external' token
   * @param staticKeyword the static keyword, or {@code null} if the setter is not static
   * @param the return type that has already been parsed, or {@code null} if there was no return
   *          type
   * @return the setter that was parsed
   */
  private MethodDeclaration parseSetter(CommentAndMetadata commentAndMetadata,
      Token externalKeyword, Token staticKeyword, TypeName returnType) {
    Token propertyKeyword = expect(Keyword.SET);
    SimpleIdentifier name = parseSimpleIdentifier();
    FormalParameterList parameters = parseFormalParameterList();
    validateFormalParameterList(parameters);
    FunctionBody body = parseFunctionBody(true, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      reportError(ParserErrorCode.EXTERNAL_SETTER_WITH_BODY);
    }
    return new MethodDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        externalKeyword,
        staticKeyword,
        returnType,
        propertyKeyword,
        null,
        name,
        parameters,
        body);
  }

  /**
   * Parse a shift expression.
   * 
   * <pre>
   * shiftExpression ::=
   *     additiveExpression (shiftOperator additiveExpression)*
   *   | 'super' (shiftOperator additiveExpression)+
   * </pre>
   * 
   * @return the shift expression that was parsed
   */
  private Expression parseShiftExpression() {
    Expression expression;
    if (matches(Keyword.SUPER) && currentToken.getNext().getType().isShiftOperator()) {
      expression = new SuperExpression(getAndAdvance());
    } else {
      expression = parseAdditiveExpression();
    }
    while (currentToken.getType().isShiftOperator()) {
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseAdditiveExpression());
    }
    return expression;
  }

  /**
   * Parse a simple identifier.
   * 
   * <pre>
   * identifier ::=
   *     IDENTIFIER
   * </pre>
   * 
   * @return the simple identifier that was parsed
   */
  private SimpleIdentifier parseSimpleIdentifier() {
    if (matchesIdentifier()) {
      return new SimpleIdentifier(getAndAdvance());
    }
    reportError(ParserErrorCode.MISSING_IDENTIFIER);
    return createSyntheticIdentifier();
  }

  /**
   * Parse a simple identifier and validate that it is not a built-in identifier.
   * 
   * <pre>
   * identifier ::=
   *     IDENTIFIER
   * </pre>
   * 
   * @param errorCode the error code to be used to report a built-in identifier is one is found
   * @return the simple identifier that was parsed
   */
  private SimpleIdentifier parseSimpleIdentifier(ParserErrorCode errorCode) {
    if (matchesIdentifier()) {
      Token token = getAndAdvance();
      if (token.getType() == TokenType.KEYWORD) {
        reportError(errorCode, token, token.getLexeme());
      }
      return new SimpleIdentifier(token);
    }
    reportError(ParserErrorCode.MISSING_IDENTIFIER);
    return createSyntheticIdentifier();
  }

  /**
   * Parse a statement.
   * 
   * <pre>
   * statement ::=
   *     label* nonLabeledStatement
   * </pre>
   * 
   * @return the statement that was parsed
   */
  private Statement parseStatement() {
    List<Label> labels = new ArrayList<Label>();
    while (matchesIdentifier() && matches(peek(), TokenType.COLON)) {
      SimpleIdentifier label = parseSimpleIdentifier();
      Token colon = expect(TokenType.COLON);
      labels.add(new Label(label, colon));
    }
    Statement statement = parseNonLabeledStatement();
    if (labels.isEmpty()) {
      return statement;
    }
    return new LabeledStatement(labels, statement);
  }

  /**
   * Parse a list of statements within a switch statement.
   * 
   * <pre>
   * statements ::=
   *     statement*
   * </pre>
   * 
   * @return the statements that were parsed
   */
  private List<Statement> parseStatements() {
    List<Statement> statements = new ArrayList<Statement>();
    Token statementStart = currentToken;
    while (!matches(TokenType.EOF) && !matches(TokenType.CLOSE_CURLY_BRACKET) && !isSwitchMember()) {
      statements.add(parseStatement());
      if (currentToken == statementStart) {
        reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
        advance();
      }
      statementStart = currentToken;
    }
    return statements;
  }

  /**
   * Parse a string literal that contains interpolations.
   * 
   * @return the string literal that was parsed
   */
  private StringInterpolation parseStringInterpolation(Token string) {
    List<InterpolationElement> elements = new ArrayList<InterpolationElement>();
    elements.add(new InterpolationString(string, computeStringValue(string.getLexeme())));
    while (matches(TokenType.STRING_INTERPOLATION_EXPRESSION)
        || matches(TokenType.STRING_INTERPOLATION_IDENTIFIER)) {
      if (matches(TokenType.STRING_INTERPOLATION_EXPRESSION)) {
        Token openToken = getAndAdvance();
        Expression expression = parseExpression();
        Token rightBracket = expect(TokenType.CLOSE_CURLY_BRACKET);
        elements.add(new InterpolationExpression(openToken, expression, rightBracket));
      } else {
        Token openToken = getAndAdvance();
        Expression expression = null;
        if (matches(Keyword.THIS)) {
          expression = new ThisExpression(getAndAdvance());
        } else {
          expression = parseSimpleIdentifier();
        }
        elements.add(new InterpolationExpression(openToken, expression, null));
      }
      if (matches(TokenType.STRING)) {
        string = getAndAdvance();
        elements.add(new InterpolationString(string, computeStringValue(string.getLexeme())));
      }
    }
    return new StringInterpolation(elements);
  }

  /**
   * Parse a string literal.
   * 
   * <pre>
   * stringLiteral ::=
   *     MULTI_LINE_STRING+
   *   | SINGLE_LINE_STRING+
   * </pre>
   * 
   * @return the string literal that was parsed
   */
  private StringLiteral parseStringLiteral() {
    List<StringLiteral> strings = new ArrayList<StringLiteral>();
    while (matches(TokenType.STRING)) {
      Token string = getAndAdvance();
      if (matches(TokenType.STRING_INTERPOLATION_EXPRESSION)
          || matches(TokenType.STRING_INTERPOLATION_IDENTIFIER)) {
        strings.add(parseStringInterpolation(string));
      } else {
        strings.add(new SimpleStringLiteral(string, computeStringValue(string.getLexeme())));
      }
    }
    if (strings.size() < 1) {
      reportError(ParserErrorCode.EXPECTED_STRING_LITERAL);
      return createSyntheticStringLiteral();
    } else if (strings.size() == 1) {
      return strings.get(0);
    } else {
      return new AdjacentStrings(strings);
    }
  }

  /**
   * Parse a super constructor invocation.
   * 
   * <pre>
   * superConstructorInvocation ::=
   *     'super' ('.' identifier)? arguments
   * </pre>
   * 
   * @return the super constructor invocation that was parsed
   */
  private SuperConstructorInvocation parseSuperConstructorInvocation() {
    Token keyword = expect(Keyword.SUPER);
    Token period = null;
    SimpleIdentifier constructorName = null;
    if (matches(TokenType.PERIOD)) {
      period = getAndAdvance();
      constructorName = parseSimpleIdentifier();
    }
    ArgumentList argumentList = parseArgumentList();
    return new SuperConstructorInvocation(keyword, period, constructorName, argumentList);
  }

  /**
   * Parse a switch statement.
   * 
   * <pre>
   * switchStatement ::=
   *     'switch' '(' expression ')' '{' switchCase* defaultCase? '}'
   * 
   * switchCase ::=
   *     label* ('case' expression ':') statements
   * 
   * defaultCase ::=
   *     label* 'default' ':' statements
   * </pre>
   * 
   * @return the switch statement that was parsed
   */
  private SwitchStatement parseSwitchStatement() {
    boolean wasInSwitch = inSwitch;
    inSwitch = true;
    try {
      HashSet<String> definedLabels = new HashSet<String>();
      Token keyword = expect(Keyword.SWITCH);
      Token leftParenthesis = expect(TokenType.OPEN_PAREN);
      Expression expression = parseExpression();
      Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
      Token leftBracket = expect(TokenType.OPEN_CURLY_BRACKET);
      List<SwitchMember> members = new ArrayList<SwitchMember>();
      while (!matches(TokenType.EOF) && !matches(TokenType.CLOSE_CURLY_BRACKET)) {
        List<Label> labels = new ArrayList<Label>();
        while (matchesIdentifier() && matches(peek(), TokenType.COLON)) {
          SimpleIdentifier identifier = parseSimpleIdentifier();
          String label = identifier.getToken().getLexeme();
          if (definedLabels.contains(label)) {
            reportError(
                ParserErrorCode.DUPLICATE_LABEL_IN_SWITCH_STATEMENT,
                identifier.getToken(),
                label);
          } else {
            definedLabels.add(label);
          }
          Token colon = expect(TokenType.COLON);
          labels.add(new Label(identifier, colon));
        }
        if (matches(Keyword.CASE)) {
          Token caseKeyword = getAndAdvance();
          Expression caseExpression = parseExpression();
          Token colon = expect(TokenType.COLON);
          members.add(new SwitchCase(labels, caseKeyword, caseExpression, colon, parseStatements()));
        } else if (matches(Keyword.DEFAULT)) {
          Token defaultKeyword = getAndAdvance();
          Token colon = expect(TokenType.COLON);
          members.add(new SwitchDefault(labels, defaultKeyword, colon, parseStatements()));
        } else {
          // We need to advance, otherwise we could end up in an infinite loop, but this could be a
          // lot smarter about recovering from the error.
          reportError(ParserErrorCode.EXPECTED_CASE_OR_DEFAULT);
          while (!matches(TokenType.EOF) && !matches(TokenType.CLOSE_CURLY_BRACKET)
              && !matches(Keyword.CASE) && !matches(Keyword.DEFAULT)) {
            advance();
          }
        }
      }
      Token rightBracket = expect(TokenType.CLOSE_CURLY_BRACKET);
      return new SwitchStatement(
          keyword,
          leftParenthesis,
          expression,
          rightParenthesis,
          leftBracket,
          members,
          rightBracket);
    } finally {
      inSwitch = wasInSwitch;
    }
  }

  /**
   * Parse a throw expression.
   * 
   * <pre>
   * throwExpression ::=
   *     'throw' expression? ';'
   * </pre>
   * 
   * @return the throw expression that was parsed
   */
  private Expression parseThrowExpression() {
    Token keyword = expect(Keyword.THROW);
    if (matches(TokenType.SEMICOLON) || matches(TokenType.CLOSE_PAREN)) {
      return new ThrowExpression(keyword, null);
    }
    Expression expression = parseExpression();
    return new ThrowExpression(keyword, expression);
  }

  /**
   * Parse a throw expression.
   * 
   * <pre>
   * throwExpressionWithoutCascade ::=
   *     'throw' expressionWithoutCascade? ';'
   * </pre>
   * 
   * @return the throw expression that was parsed
   */
  private Expression parseThrowExpressionWithoutCascade() {
    Token keyword = expect(Keyword.THROW);
    if (matches(TokenType.SEMICOLON) || matches(TokenType.CLOSE_PAREN)) {
      return new ThrowExpression(keyword, null);
    }
    Expression expression = parseExpressionWithoutCascade();
    return new ThrowExpression(keyword, expression);
  }

  /**
   * Parse a try statement.
   * 
   * <pre>
   * tryStatement ::=
   *     'try' block (onPart+ finallyPart? | finallyPart)
   * 
   * onPart ::=
   *     catchPart block
   *   | 'on' qualified catchPart? block
   * 
   * catchPart ::=
   *     'catch' '(' identifier (',' identifier)? ')'
   * 
   * finallyPart ::=
   *     'finally' block
   * </pre>
   * 
   * @return the try statement that was parsed
   */
  private Statement parseTryStatement() {
    Token tryKeyword = expect(Keyword.TRY);
    Block body = parseBlock();
    List<CatchClause> catchClauses = new ArrayList<CatchClause>();
    Block finallyClause = null;
    while (matches(ON) || matches(Keyword.CATCH)) {
      Token onKeyword = null;
      TypeName exceptionType = null;
      if (matches(ON)) {
        onKeyword = getAndAdvance();
        exceptionType = new TypeName(parsePrefixedIdentifier(), null);
      }
      Token catchKeyword = null;
      Token leftParenthesis = null;
      SimpleIdentifier exceptionParameter = null;
      Token comma = null;
      SimpleIdentifier stackTraceParameter = null;
      Token rightParenthesis = null;
      if (matches(Keyword.CATCH)) {
        catchKeyword = getAndAdvance();
        leftParenthesis = expect(TokenType.OPEN_PAREN);
        exceptionParameter = parseSimpleIdentifier();
        if (matches(TokenType.COMMA)) {
          comma = getAndAdvance();
          stackTraceParameter = parseSimpleIdentifier();
        }
        rightParenthesis = expect(TokenType.CLOSE_PAREN);
      }
      Block catchBody = parseBlock();
      catchClauses.add(new CatchClause(
          onKeyword,
          exceptionType,
          catchKeyword,
          leftParenthesis,
          exceptionParameter,
          comma,
          stackTraceParameter,
          rightParenthesis,
          catchBody));
    }
    Token finallyKeyword = null;
    if (matches(Keyword.FINALLY)) {
      finallyKeyword = getAndAdvance();
      finallyClause = parseBlock();
    } else {
      if (catchClauses.isEmpty()) {
        reportError(ParserErrorCode.MISSING_CATCH_OR_FINALLY);
      }
    }
    return new TryStatement(tryKeyword, body, catchClauses, finallyKeyword, finallyClause);
  }

  /**
   * Parse a type alias.
   * 
   * <pre>
   * typeAlias ::=
   *     metadata 'typedef' returnType? name typeParameterList? formalParameterList ';'
   * </pre>
   * 
   * @param commentAndMetadata the metadata to be associated with the member
   * @return the type alias that was parsed
   */
  private TypeAlias parseTypeAlias(CommentAndMetadata commentAndMetadata) {
    Token keyword = expect(Keyword.TYPEDEF);
    TypeName returnType = null;
    if (hasReturnTypeInTypeAlias()) {
      returnType = parseReturnType();
    }
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    TypeParameterList typeParameters = null;
    if (matches(TokenType.LT)) {
      typeParameters = parseTypeParameterList();
    }
    if (matches(TokenType.SEMICOLON)) {
      reportError(ParserErrorCode.MISSING_TYPEDEF_PARAMETERS);
      FormalParameterList parameters = new FormalParameterList(
          createSyntheticToken(TokenType.OPEN_PAREN),
          null,
          null,
          null,
          createSyntheticToken(TokenType.CLOSE_PAREN));
      Token semicolon = expect(TokenType.SEMICOLON);
      return new TypeAlias(
          commentAndMetadata.getComment(),
          commentAndMetadata.getMetadata(),
          keyword,
          returnType,
          name,
          typeParameters,
          parameters,
          semicolon);
    } else if (!matches(TokenType.OPEN_PAREN)) {
      // TODO(brianwilkerson) Report this error and recover.
      return null;
    }
    FormalParameterList parameters = parseFormalParameterList();
    validateFormalParameterList(parameters);
    Token semicolon = expect(TokenType.SEMICOLON);
    return new TypeAlias(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        keyword,
        returnType,
        name,
        typeParameters,
        parameters,
        semicolon);
  }

  /**
   * Parse a list of type arguments.
   * 
   * <pre>
   * typeArguments ::=
   *     '<' typeList '>'
   * 
   * typeList ::=
   *     type (',' type)*
   * </pre>
   * 
   * @return the type argument list that was parsed
   */
  private TypeArgumentList parseTypeArgumentList() {
    Token leftBracket = expect(TokenType.LT);
    List<TypeName> arguments = new ArrayList<TypeName>();
    arguments.add(parseTypeName());
    while (optional(TokenType.COMMA)) {
      arguments.add(parseTypeName());
    }
    Token rightBracket = expect(TokenType.GT);
    return new TypeArgumentList(leftBracket, arguments, rightBracket);
  }

  /**
   * Parse a type name.
   * 
   * <pre>
   * type ::=
   *     qualified typeArguments?
   * </pre>
   * 
   * @return the type name that was parsed
   */
  private TypeName parseTypeName() {
    Identifier typeName = parsePrefixedIdentifier();
    TypeArgumentList typeArguments = null;
    if (matches(TokenType.LT)) {
      typeArguments = parseTypeArgumentList();
    }
    return new TypeName(typeName, typeArguments);
  }

  /**
   * Parse a type parameter.
   * 
   * <pre>
   * typeParameter ::=
   *     metadata name ('extends' bound)?
   * </pre>
   * 
   * @return the type parameter that was parsed
   */
  private TypeParameter parseTypeParameter() {
    CommentAndMetadata commentAndMetadata = parseCommentAndMetadata();
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME);
    if (matches(Keyword.EXTENDS)) {
      Token keyword = getAndAdvance();
      TypeName bound = parseTypeName();
      return new TypeParameter(
          commentAndMetadata.getComment(),
          commentAndMetadata.getMetadata(),
          name,
          keyword,
          bound);
    }
    return new TypeParameter(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        name,
        null,
        null);
  }

/**
   * Parse a list of type parameters.
   * 
   * <pre>
   * typeParameterList ::=
   *     '<' typeParameter (',' typeParameter)* '>'
   * </pre>
   * 
   * @return the list of type parameters that were parsed
   */
  private TypeParameterList parseTypeParameterList() {
    Token leftBracket = expect(TokenType.LT);
    List<TypeParameter> typeParameters = new ArrayList<TypeParameter>();
    typeParameters.add(parseTypeParameter());
    while (optional(TokenType.COMMA)) {
      typeParameters.add(parseTypeParameter());
    }
    Token rightBracket = expect(TokenType.GT);
    return new TypeParameterList(leftBracket, typeParameters, rightBracket);
  }

  /**
   * Parse a unary expression.
   * 
   * <pre>
   * unaryExpression ::=
   *     prefixOperator unaryExpression
   *   | postfixExpression
   *   | unaryOperator 'super'
   *   | '-' 'super'
   *   | incrementOperator assignableExpression
   * </pre>
   * 
   * @return the unary expression that was parsed
   */
  private Expression parseUnaryExpression() {
    if (matches(TokenType.MINUS)) {
      Token operator = getAndAdvance();
      if (matches(Keyword.SUPER)) {
        return new PrefixExpression(operator, new SuperExpression(getAndAdvance()));
      }
      return new PrefixExpression(operator, parseUnaryExpression());
    } else if (matches(TokenType.BANG)) {
      Token operator = getAndAdvance();
      if (matches(Keyword.SUPER)) {
        return new PrefixExpression(operator, new SuperExpression(getAndAdvance()));
      }
      return new PrefixExpression(operator, parseUnaryExpression());
    } else if (matches(TokenType.TILDE)) {
      Token operator = getAndAdvance();
      if (matches(Keyword.SUPER)) {
        return new PrefixExpression(operator, new SuperExpression(getAndAdvance()));
      }
      return new PrefixExpression(operator, parseUnaryExpression());
    } else if (currentToken.getType().isIncrementOperator()) {
      Token operator = getAndAdvance();
      if (matches(Keyword.SUPER)) {
        //
        // Even though it is not valid to use an incrementing operator ('++' or '--') before 'super',
        // we can (and therefore must) interpret "--super" as semantically equivalent to "-(-super)".
        // Unfortunately, we cannot do the same for "++super" because "+super" is also not valid.
        //
        if (operator.getType() == TokenType.MINUS_MINUS) {
          int offset = operator.getOffset();
          Token firstOperator = new Token(TokenType.MINUS, offset);
          Token secondOperator = new Token(TokenType.MINUS, offset + 1);
          secondOperator.setNext(currentToken);
          firstOperator.setNext(secondOperator);
          operator.getPrevious().setNext(firstOperator);
          return new PrefixExpression(firstOperator, new PrefixExpression(
              secondOperator,
              new SuperExpression(getAndAdvance())));
        } else {
          // Invalid operator before 'super'
          reportError(ParserErrorCode.INVALID_OPERATOR_FOR_SUPER, operator.getLexeme());
          return new PrefixExpression(operator, new SuperExpression(getAndAdvance()));
        }
      }
      return new PrefixExpression(operator, parseAssignableExpression(false));
    } else if (matches(TokenType.PLUS)) {
      reportError(ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
    }
    return parsePostfixExpression();
  }

  /**
   * Parse a variable declaration.
   * 
   * <pre>
   * variableDeclaration ::=
   *     identifier ('=' expression)?
   * </pre>
   * 
   * @return the variable declaration that was parsed
   */
  private VariableDeclaration parseVariableDeclaration() {
    CommentAndMetadata commentAndMetadata = parseCommentAndMetadata();
    SimpleIdentifier name = parseSimpleIdentifier();
    Token equals = null;
    Expression initializer = null;
    if (matches(TokenType.EQ)) {
      equals = getAndAdvance();
      initializer = parseExpression();
    }
    return new VariableDeclaration(
        commentAndMetadata.getComment(),
        commentAndMetadata.getMetadata(),
        name,
        equals,
        initializer);
  }

  /**
   * Parse a variable declaration list.
   * 
   * <pre>
   * variableDeclarationList ::=
   *     finalConstVarOrType variableDeclaration (',' variableDeclaration)*
   * </pre>
   * 
   * @return the variable declaration list that was parsed
   */
  private VariableDeclarationList parseVariableDeclarationList() {
    FinalConstVarOrType holder = parseFinalConstVarOrType(false);
    return parseVariableDeclarationList(holder.getKeyword(), holder.getType());
  }

  /**
   * Parse a variable declaration list.
   * 
   * <pre>
   * variableDeclarationList ::=
   *     finalConstVarOrType variableDeclaration (',' variableDeclaration)*
   * </pre>
   * 
   * @param keyword the token representing the 'final', 'const' or 'var' keyword, or {@code null} if
   *          there is no keyword
   * @param type the type of the variables in the list
   * @return the variable declaration list that was parsed
   */
  private VariableDeclarationList parseVariableDeclarationList(Token keyword, TypeName type) {
    List<VariableDeclaration> variables = new ArrayList<VariableDeclaration>();
    variables.add(parseVariableDeclaration());
    while (matches(TokenType.COMMA)) {
      advance();
      variables.add(parseVariableDeclaration());
    }
    return new VariableDeclarationList(keyword, type, variables);
  }

  /**
   * Parse a variable declaration statement.
   * 
   * <pre>
   * variableDeclarationStatement ::=
   *     variableDeclarationList ';'
   * </pre>
   * 
   * @return the variable declaration statement that was parsed
   */
  private VariableDeclarationStatement parseVariableDeclarationStatement() {
    VariableDeclarationList variableList = parseVariableDeclarationList();
    Token semicolon = expect(TokenType.SEMICOLON);
    return new VariableDeclarationStatement(variableList, semicolon);
  }

  /**
   * Parse a while statement.
   * 
   * <pre>
   * whileStatement ::=
   *     'while' '(' expression ')' statement
   * </pre>
   * 
   * @return the while statement that was parsed
   */
  private Statement parseWhileStatement() {
    boolean wasInLoop = inLoop;
    inLoop = true;
    try {
      Token keyword = expect(Keyword.WHILE);
      Token leftParenthesis = expect(TokenType.OPEN_PAREN);
      Expression condition = parseExpression();
      Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
      Statement body = parseStatement();
      return new WhileStatement(keyword, leftParenthesis, condition, rightParenthesis, body);
    } finally {
      inLoop = wasInLoop;
    }
  }

  /**
   * Return the token that is immediately after the current token. This is equivalent to
   * {@link #peek(int) peek(1)}.
   * 
   * @return the token that is immediately after the current token
   */
  private Token peek() {
    return currentToken.getNext();
  }

  /**
   * Return the token that is the given distance after the current token.
   * 
   * @param distance the number of tokens to look ahead, where {@code 0} is the current token,
   *          {@code 1} is the next token, etc.
   * @return the token that is the given distance after the current token
   */
  private Token peek(int distance) {
    Token token = currentToken;
    for (int i = 0; i < distance; i++) {
      token = token.getNext();
    }
    return token;
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param node the node specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  private void reportError(ParserErrorCode errorCode, ASTNode node, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        node.getOffset(),
        node.getLength(),
        errorCode,
        arguments));
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param arguments the arguments to the error, used to compose the error message
   */
  private void reportError(ParserErrorCode errorCode, Object... arguments) {
    reportError(errorCode, currentToken, arguments);
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param token the token specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  private void reportError(ParserErrorCode errorCode, Token token, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        token.getOffset(),
        token.getLength(),
        errorCode,
        arguments));
  }

  /**
   * Parse the 'final', 'const', 'var' or type preceding a variable declaration, starting at the
   * given token, without actually creating a type or changing the current token. Return the token
   * following the type that was parsed, or {@code null} if the given token is not the first token
   * in a valid type.
   * 
   * <pre>
   * finalConstVarOrType ::=
   *   | 'final' type?
   *   | 'const' type?
   *   | 'var'
   *   | type
   * </pre>
   * 
   * @param startToken the token at which parsing is to begin
   * @return the token following the type that was parsed
   */
  private Token skipFinalConstVarOrType(Token startToken) {
    if (matches(startToken, Keyword.FINAL) || matches(startToken, Keyword.CONST)) {
      Token next = startToken.getNext();
      if (matchesIdentifier(next.getNext()) || matches(next.getNext(), TokenType.LT)
          || matches(next.getNext(), Keyword.THIS)) {
        return skipTypeName(next);
      }
    } else if (matches(startToken, Keyword.VAR)) {
      return startToken.getNext();
    } else if (matchesIdentifier(startToken)) {
      Token next = startToken.getNext();
      if (matchesIdentifier(next)
          || matches(next, TokenType.LT)
          || matches(next, Keyword.THIS)
          || (matches(next, TokenType.PERIOD) && matchesIdentifier(next.getNext()) && (matchesIdentifier(next.getNext().getNext())
              || matches(next.getNext().getNext(), TokenType.LT) || matches(
                next.getNext().getNext(),
                Keyword.THIS)))) {
        return skipReturnType(startToken);
      }
    }
    return null;
  }

  /**
   * Parse a list of formal parameters, starting at the given token, without actually creating a
   * formal parameter list or changing the current token. Return the token following the formal
   * parameter list that was parsed, or {@code null} if the given token is not the first token in a
   * valid list of formal parameter.
   * <p>
   * Note that unlike other skip methods, this method uses a heuristic. In the worst case, the
   * parameters could be prefixed by metadata, which would require us to be able to skip arbitrary
   * expressions. Rather than duplicate the logic of most of the parse methods we simply look for
   * something that is likely to be a list of parameters and then skip to returning the token after
   * the closing parenthesis.
   * <p>
   * This method must be kept in sync with {@link #parseFormalParameterList()}.
   * 
   * <pre>
   * formalParameterList ::=
   *     '(' ')'
   *   | '(' normalFormalParameters (',' optionalFormalParameters)? ')'
   *   | '(' optionalFormalParameters ')'
   *
   * normalFormalParameters ::=
   *     normalFormalParameter (',' normalFormalParameter)*
   *
   * optionalFormalParameters ::=
   *     optionalPositionalFormalParameters
   *   | namedFormalParameters
   *
   * optionalPositionalFormalParameters ::=
   *     '[' defaultFormalParameter (',' defaultFormalParameter)* ']'
   *
   * namedFormalParameters ::=
   *     '{' defaultNamedParameter (',' defaultNamedParameter)* '}'
   * </pre>
   * 
   * @param startToken the token at which parsing is to begin
   * @return the token following the formal parameter list that was parsed
   */
  private Token skipFormalParameterList(Token startToken) {
    if (!matches(startToken, TokenType.OPEN_PAREN)) {
      return null;
    }
    Token next = startToken.getNext();
    if (matches(next, TokenType.CLOSE_PAREN)) {
      return next.getNext();
    }
    //
    // Look to see whether the token after the open parenthesis is something that should only occur
    // at the beginning of a parameter list.
    //
    if (matchesAny(next, TokenType.AT, TokenType.OPEN_SQUARE_BRACKET, TokenType.OPEN_CURLY_BRACKET)
        || matches(next, Keyword.VOID)
        || (matchesIdentifier(next) && (matchesAny(
            next.getNext(),
            TokenType.COMMA,
            TokenType.CLOSE_PAREN)))) {
      return skipPastMatchingToken(startToken);
    }
    //
    // Look to see whether the first parameter is a function typed parameter without a return type.
    //
    if (matchesIdentifier(next) && matches(next.getNext(), TokenType.OPEN_PAREN)) {
      Token afterParameters = skipFormalParameterList(next.getNext());
      if (afterParameters != null
          && (matchesAny(afterParameters, TokenType.COMMA, TokenType.CLOSE_PAREN))) {
        return skipPastMatchingToken(startToken);
      }
    }
    //
    // Look to see whether the first parameter has a type or is a function typed parameter with a
    // return type.
    //
    Token afterType = skipFinalConstVarOrType(next);
    if (afterType == null) {
      return null;
    }
    if (skipSimpleIdentifier(afterType) == null) {
      return null;
    }
    return skipPastMatchingToken(startToken);
  }

  /**
   * If the given token is a begin token with an associated end token, then return the token
   * following the end token. Otherwise, return {@code null}.
   * 
   * @param startToken the token that is assumed to be a being token
   * @return the token following the matching end token
   */
  private Token skipPastMatchingToken(Token startToken) {
    if (!(startToken instanceof BeginToken)) {
      return null;
    }
    Token closeParen = ((BeginToken) startToken).getEndToken();
    if (closeParen == null) {
      return null;
    }
    return closeParen.getNext();
  }

  /**
   * Parse a prefixed identifier, starting at the given token, without actually creating a prefixed
   * identifier or changing the current token. Return the token following the prefixed identifier
   * that was parsed, or {@code null} if the given token is not the first token in a valid prefixed
   * identifier.
   * <p>
   * This method must be kept in sync with {@link #parsePrefixedIdentifier()}.
   * 
   * <pre>
   * prefixedIdentifier ::=
   *     identifier ('.' identifier)?
   * </pre>
   * 
   * @param startToken the token at which parsing is to begin
   * @return the token following the prefixed identifier that was parsed
   */
  private Token skipPrefixedIdentifier(Token startToken) {
    Token token = skipSimpleIdentifier(startToken);
    if (token == null) {
      return null;
    } else if (!matches(token, TokenType.PERIOD)) {
      return token;
    }
    return skipSimpleIdentifier(token.getNext());
  }

  /**
   * Parse a return type, starting at the given token, without actually creating a return type or
   * changing the current token. Return the token following the return type that was parsed, or
   * {@code null} if the given token is not the first token in a valid return type.
   * <p>
   * This method must be kept in sync with {@link #parseReturnType()}.
   * 
   * <pre>
   * returnType ::=
   *     'void'
   *   | type
   * </pre>
   * 
   * @param startToken the token at which parsing is to begin
   * @return the token following the return type that was parsed
   */
  private Token skipReturnType(Token startToken) {
    if (matches(startToken, Keyword.VOID)) {
      return startToken.getNext();
    } else {
      return skipTypeName(startToken);
    }
  }

  /**
   * Parse a simple identifier, starting at the given token, without actually creating a simple
   * identifier or changing the current token. Return the token following the simple identifier that
   * was parsed, or {@code null} if the given token is not the first token in a valid simple
   * identifier.
   * <p>
   * This method must be kept in sync with {@link #parseSimpleIdentifier()}.
   * 
   * <pre>
   * identifier ::=
   *     IDENTIFIER
   * </pre>
   * 
   * @param startToken the token at which parsing is to begin
   * @return the token following the simple identifier that was parsed
   */
  private Token skipSimpleIdentifier(Token startToken) {
    if (matches(startToken, TokenType.IDENTIFIER)
        || (matches(startToken, TokenType.KEYWORD) && ((KeywordToken) startToken).getKeyword().isPseudoKeyword())) {
      return startToken.getNext();
    }
    return null;
  }

  /**
   * Parse a string literal that contains interpolations, starting at the given token, without
   * actually creating a string literal or changing the current token. Return the token following
   * the string literal that was parsed, or {@code null} if the given token is not the first token
   * in a valid string literal.
   * <p>
   * This method must be kept in sync with {@link #parseStringInterpolation(Token)}.
   * 
   * @param startToken the token at which parsing is to begin
   * @return the string literal that was parsed
   */
  private Token skipStringInterpolation(Token startToken) {
    Token token = startToken;
    TokenType type = token.getType();
    while (type == TokenType.STRING_INTERPOLATION_EXPRESSION
        || type == TokenType.STRING_INTERPOLATION_IDENTIFIER) {
      if (type == TokenType.STRING_INTERPOLATION_EXPRESSION) {
        token = token.getNext();
        type = token.getType();
        //
        // Rather than verify that the following tokens represent a valid expression, we simply skip
        // tokens until we reach the end of the interpolation, being careful to handle nested string
        // literals.
        //
        int bracketNestingLevel = 1;
        while (bracketNestingLevel > 0) {
          if (type == TokenType.EOF) {
            return null;
          } else if (type == TokenType.OPEN_CURLY_BRACKET) {
            bracketNestingLevel++;
          } else if (type == TokenType.CLOSE_CURLY_BRACKET) {
            bracketNestingLevel--;
          } else if (type == TokenType.STRING) {
            token = skipStringLiteral(token);
            if (token == null) {
              return null;
            }
          } else {
            token = token.getNext();
          }
          type = token.getType();
        }
        token = token.getNext();
        type = token.getType();
      } else {
        token = token.getNext();
        if (token.getType() != TokenType.IDENTIFIER) {
          return null;
        }
        token = token.getNext();
      }
      type = token.getType();
      if (type == TokenType.STRING) {
        token = token.getNext();
        type = token.getType();
      }
    }
    return token;
  }

  /**
   * Parse a string literal, starting at the given token, without actually creating a string literal
   * or changing the current token. Return the token following the string literal that was parsed,
   * or {@code null} if the given token is not the first token in a valid string literal.
   * <p>
   * This method must be kept in sync with {@link #parseStringLiteral()}.
   * 
   * <pre>
 * stringLiteral ::=
 *     MULTI_LINE_STRING+
 *   | SINGLE_LINE_STRING+
 * </pre>
   * 
   * @param startToken the token at which parsing is to begin
   * @return the token following the string literal that was parsed
   */
  private Token skipStringLiteral(Token startToken) {
    Token token = startToken;
    while (token != null && matches(token, TokenType.STRING)) {
      token = token.getNext();
      TokenType type = token.getType();
      if (type == TokenType.STRING_INTERPOLATION_EXPRESSION
          || type == TokenType.STRING_INTERPOLATION_IDENTIFIER) {
        token = skipStringInterpolation(token);
      }
    }
    if (token == startToken) {
      return null;
    }
    return token;
  }

  /**
   * Parse a list of type arguments, starting at the given token, without actually creating a type argument list
   * or changing the current token. Return the token following the type argument list that was parsed,
   * or {@code null} if the given token is not the first token in a valid type argument list.
   * <p>
   * This method must be kept in sync with {@link #parseTypeArgumentList()}.
   * 
   * <pre>
   * typeArguments ::=
   *     '<' typeList '>'
   * 
   * typeList ::=
   *     type (',' type)*
   * </pre>
   * 
   * @param startToken the token at which parsing is to begin
   * @return the token following the type argument list that was parsed
   */
  private Token skipTypeArgumentList(Token startToken) {
    Token token = startToken;
    if (!matches(token, TokenType.LT)) {
      return null;
    }
    token = skipTypeName(token.getNext());
    if (token == null) {
      return null;
    }
    while (matches(token, TokenType.COMMA)) {
      token = skipTypeName(token.getNext());
      if (token == null) {
        return null;
      }
    }
    if (token.getType() == TokenType.GT) {
      return token.getNext();
    } else if (token.getType() == TokenType.GT_GT) {
      Token second = new Token(TokenType.GT, token.getOffset() + 1);
      second.setNextWithoutSettingPrevious(token.getNext());
      return second;
    } else if (token.getType() == TokenType.GT_GT_GT) {
      Token second = new Token(TokenType.GT, token.getOffset() + 1);
      Token third = new Token(TokenType.GT, token.getOffset() + 2);
      third.setNextWithoutSettingPrevious(token.getNext());
      second.setNextWithoutSettingPrevious(third);
      return second;
    }
    return null;
  }

  /**
   * Parse a type name, starting at the given token, without actually creating a type name or
   * changing the current token. Return the token following the type name that was parsed, or
   * {@code null} if the given token is not the first token in a valid type name.
   * <p>
   * This method must be kept in sync with {@link #parseTypeName()}.
   * 
   * <pre>
   * type ::=
   *     qualified typeArguments?
   * </pre>
   * 
   * @param startToken the token at which parsing is to begin
   * @return the token following the type name that was parsed
   */
  private Token skipTypeName(Token startToken) {
    Token token = skipPrefixedIdentifier(startToken);
    if (token == null) {
      return null;
    }
    if (matches(token, TokenType.LT)) {
      token = skipTypeArgumentList(token);
    }
    return token;
  }

  /**
   * Translate the characters at the given index in the given string, appending the translated
   * character to the given builder. The index is assumed to be valid.
   * 
   * @param builder the builder to which the translated character is to be appended
   * @param lexeme the string containing the character(s) to be translated
   * @param index the index of the character to be translated
   * @return the index of the next character to be translated
   */
  private int translateCharacter(StringBuilder builder, String lexeme, int index) {
    char currentChar = lexeme.charAt(index);
    if (currentChar != '\\') {
      builder.append(currentChar);
      return index + 1;
    }
    //
    // We have found an escape sequence, so we parse the string to determine what kind of escape
    // sequence and what character to add to the builder.
    //
    int length = lexeme.length();
    int currentIndex = index + 1;
    if (currentIndex >= length) {
      // Illegal escape sequence: no char after escape
      // This cannot actually happen because it would require the escape character to be the last
      // character in the string, but if it were it would escape the closing quote, leaving the
      // string unclosed.
      // reportError(ParserErrorCode.MISSING_CHAR_IN_ESCAPE_SEQUENCE);
      return length;
    }
    currentChar = lexeme.charAt(currentIndex);
    if (currentChar == 'n') {
      builder.append('\n'); // newline
    } else if (currentChar == 'r') {
      builder.append('\r'); // carriage return
    } else if (currentChar == 'f') {
      builder.append('\f'); // form feed
    } else if (currentChar == 'b') {
      builder.append('\b'); // backspace
    } else if (currentChar == 't') {
      builder.append('\t'); // tab
    } else if (currentChar == 'v') {
      builder.append('\u000B'); // vertical tab
    } else if (currentChar == 'x') {
      if (currentIndex + 2 >= length) {
        // Illegal escape sequence: not enough hex digits
        reportError(ParserErrorCode.INVALID_HEX_ESCAPE);
        return length;
      }
      char firstDigit = lexeme.charAt(currentIndex + 1);
      char secondDigit = lexeme.charAt(currentIndex + 2);
      if (!isHexDigit(firstDigit) || !isHexDigit(secondDigit)) {
        // Illegal escape sequence: invalid hex digit
        reportError(ParserErrorCode.INVALID_HEX_ESCAPE);
      } else {
        builder.append((char) ((Character.digit(firstDigit, 16) << 4) + Character.digit(
            secondDigit,
            16)));
      }
      return currentIndex + 3;
    } else if (currentChar == 'u') {
      currentIndex++;
      if (currentIndex >= length) {
        // Illegal escape sequence: not enough hex digits
        reportError(ParserErrorCode.INVALID_UNICODE_ESCAPE);
        return length;
      }
      currentChar = lexeme.charAt(currentIndex);
      if (currentChar == '{') {
        currentIndex++;
        if (currentIndex >= length) {
          // Illegal escape sequence: incomplete escape
          reportError(ParserErrorCode.INVALID_UNICODE_ESCAPE);
          return length;
        }
        currentChar = lexeme.charAt(currentIndex);
        int digitCount = 0;
        int value = 0;
        while (currentChar != '}') {
          if (!isHexDigit(currentChar)) {
            // Illegal escape sequence: invalid hex digit
            reportError(ParserErrorCode.INVALID_UNICODE_ESCAPE);
            currentIndex++;
            while (currentIndex < length && lexeme.charAt(currentIndex) != '}') {
              currentIndex++;
            }
            return currentIndex + 1;
          }
          digitCount++;
          value = (value << 4) + Character.digit(currentChar, 16);
          currentIndex++;
          if (currentIndex >= length) {
            // Illegal escape sequence: incomplete escape
            reportError(ParserErrorCode.INVALID_UNICODE_ESCAPE);
            return length;
          }
          currentChar = lexeme.charAt(currentIndex);
        }
        if (digitCount < 1 || digitCount > 6) {
          // Illegal escape sequence: not enough or too many hex digits
          reportError(ParserErrorCode.INVALID_UNICODE_ESCAPE);
        }
        appendScalarValue(
            builder,
            lexeme.substring(index, currentIndex + 1),
            value,
            index,
            currentIndex);
        return currentIndex + 1;
      } else {
        if (currentIndex + 3 >= length) {
          // Illegal escape sequence: not enough hex digits
          reportError(ParserErrorCode.INVALID_UNICODE_ESCAPE);
          return length;
        }
        char firstDigit = currentChar;
        char secondDigit = lexeme.charAt(currentIndex + 1);
        char thirdDigit = lexeme.charAt(currentIndex + 2);
        char fourthDigit = lexeme.charAt(currentIndex + 3);
        if (!isHexDigit(firstDigit) || !isHexDigit(secondDigit) || !isHexDigit(thirdDigit)
            || !isHexDigit(fourthDigit)) {
          // Illegal escape sequence: invalid hex digits
          reportError(ParserErrorCode.INVALID_UNICODE_ESCAPE);
        } else {
          appendScalarValue(
              builder,
              lexeme.substring(index, currentIndex + 1),
              ((((((Character.digit(firstDigit, 16) << 4) + Character.digit(secondDigit, 16)) << 4) + Character.digit(
                  thirdDigit,
                  16)) << 4) + Character.digit(fourthDigit, 16)),
              index,
              currentIndex + 3);
        }
        return currentIndex + 4;
      }
    } else {
      builder.append(currentChar);
    }
    return currentIndex + 1;
  }

  /**
   * Validate that the given parameter list does not contain any field initializers.
   * 
   * @param parameterList the parameter list to be validated
   */
  private void validateFormalParameterList(FormalParameterList parameterList) {
    for (FormalParameter parameter : parameterList.getParameters()) {
      if (parameter instanceof FieldFormalParameter) {
        reportError(
            ParserErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR,
            ((FieldFormalParameter) parameter).getIdentifier());
      }
    }
  }

  /**
   * Validate that the given set of modifiers is appropriate for a class and return the 'abstract'
   * keyword if there is one.
   * 
   * @param modifiers the modifiers being validated
   */
  private Token validateModifiersForClass(Modifiers modifiers) {
    validateModifiersForTopLevelDeclaration(modifiers);
    if (modifiers.getConstKeyword() != null) {
      reportError(ParserErrorCode.CONST_CLASS, modifiers.getConstKeyword());
    }
    if (modifiers.getExternalKeyword() != null) {
      reportError(ParserErrorCode.EXTERNAL_CLASS, modifiers.getExternalKeyword());
    }
    if (modifiers.getFinalKeyword() != null) {
      reportError(ParserErrorCode.FINAL_CLASS, modifiers.getFinalKeyword());
    }
    if (modifiers.getVarKeyword() != null) {
      reportError(ParserErrorCode.VAR_CLASS, modifiers.getVarKeyword());
    }
    return modifiers.getAbstractKeyword();
  }

  /**
   * Validate that the given set of modifiers is appropriate for a constructor and return the
   * 'const' keyword if there is one.
   * 
   * @param modifiers the modifiers being validated
   * @return the 'const' or 'final' keyword associated with the constructor
   */
  private Token validateModifiersForConstructor(Modifiers modifiers) {
    if (modifiers.getAbstractKeyword() != null) {
      reportError(ParserErrorCode.ABSTRACT_CLASS_MEMBER);
    }
    if (modifiers.getFinalKeyword() != null) {
      reportError(ParserErrorCode.FINAL_CONSTRUCTOR, modifiers.getFinalKeyword());
    }
    if (modifiers.getStaticKeyword() != null) {
      reportError(ParserErrorCode.STATIC_CONSTRUCTOR, modifiers.getStaticKeyword());
    }
    if (modifiers.getVarKeyword() != null) {
      reportError(ParserErrorCode.CONSTRUCTOR_WITH_RETURN_TYPE, modifiers.getVarKeyword());
    }
    Token externalKeyword = modifiers.getExternalKeyword();
    Token constKeyword = modifiers.getConstKeyword();
    Token factoryKeyword = modifiers.getFactoryKeyword();
    if (externalKeyword != null && constKeyword != null
        && constKeyword.getOffset() < externalKeyword.getOffset()) {
      reportError(ParserErrorCode.EXTERNAL_AFTER_CONST, externalKeyword);
    }
    if (externalKeyword != null && factoryKeyword != null
        && factoryKeyword.getOffset() < externalKeyword.getOffset()) {
      reportError(ParserErrorCode.EXTERNAL_AFTER_FACTORY, externalKeyword);
    }
    return constKeyword;
  }

  /**
   * Validate that the given set of modifiers is appropriate for a field and return the 'final',
   * 'const' or 'var' keyword if there is one.
   * 
   * @param modifiers the modifiers being validated
   * @return the 'final', 'const' or 'var' keyword associated with the field
   */
  private Token validateModifiersForField(Modifiers modifiers) {
    if (modifiers.getAbstractKeyword() != null) {
      reportError(ParserErrorCode.ABSTRACT_CLASS_MEMBER);
    }
    if (modifiers.getExternalKeyword() != null) {
      reportError(ParserErrorCode.EXTERNAL_FIELD, modifiers.getExternalKeyword());
    }
    if (modifiers.getFactoryKeyword() != null) {
      reportError(ParserErrorCode.NON_CONSTRUCTOR_FACTORY, modifiers.getFactoryKeyword());
    }
    Token staticKeyword = modifiers.getStaticKeyword();
    Token constKeyword = modifiers.getConstKeyword();
    Token finalKeyword = modifiers.getFinalKeyword();
    Token varKeyword = modifiers.getVarKeyword();
    if (constKeyword != null) {
      if (finalKeyword != null) {
        reportError(ParserErrorCode.CONST_AND_FINAL, finalKeyword);
      }
      if (varKeyword != null) {
        reportError(ParserErrorCode.CONST_AND_VAR, varKeyword);
      }
      if (staticKeyword != null && constKeyword.getOffset() < staticKeyword.getOffset()) {
        reportError(ParserErrorCode.STATIC_AFTER_CONST, staticKeyword);
      }
    } else if (finalKeyword != null) {
      if (varKeyword != null) {
        reportError(ParserErrorCode.FINAL_AND_VAR, varKeyword);
      }
      if (staticKeyword != null && finalKeyword.getOffset() < staticKeyword.getOffset()) {
        reportError(ParserErrorCode.STATIC_AFTER_FINAL, staticKeyword);
      }
    } else if (varKeyword != null && staticKeyword != null
        && varKeyword.getOffset() < staticKeyword.getOffset()) {
      reportError(ParserErrorCode.STATIC_AFTER_VAR, staticKeyword);
    }
    return lexicallyFirst(constKeyword, finalKeyword, varKeyword);
  }

  /**
   * Validate that the given set of modifiers is appropriate for a getter, setter, or method.
   * 
   * @param modifiers the modifiers being validated
   */
  private void validateModifiersForGetterOrSetterOrMethod(Modifiers modifiers) {
    if (modifiers.getAbstractKeyword() != null) {
      reportError(ParserErrorCode.ABSTRACT_CLASS_MEMBER);
    }
    if (modifiers.getConstKeyword() != null) {
      reportError(ParserErrorCode.CONST_METHOD, modifiers.getConstKeyword());
    }
    if (modifiers.getFactoryKeyword() != null) {
      reportError(ParserErrorCode.NON_CONSTRUCTOR_FACTORY, modifiers.getFactoryKeyword());
    }
    if (modifiers.getFinalKeyword() != null) {
      reportError(ParserErrorCode.FINAL_METHOD, modifiers.getFinalKeyword());
    }
    if (modifiers.getVarKeyword() != null) {
      reportError(ParserErrorCode.VAR_RETURN_TYPE, modifiers.getVarKeyword());
    }
    Token externalKeyword = modifiers.getExternalKeyword();
    Token staticKeyword = modifiers.getStaticKeyword();
    if (externalKeyword != null && staticKeyword != null
        && staticKeyword.getOffset() < externalKeyword.getOffset()) {
      reportError(ParserErrorCode.EXTERNAL_AFTER_STATIC, externalKeyword);
    }
  }

  /**
   * Validate that the given set of modifiers is appropriate for a getter, setter, or method.
   * 
   * @param modifiers the modifiers being validated
   */
  private void validateModifiersForOperator(Modifiers modifiers) {
    if (modifiers.getAbstractKeyword() != null) {
      reportError(ParserErrorCode.ABSTRACT_CLASS_MEMBER);
    }
    if (modifiers.getConstKeyword() != null) {
      reportError(ParserErrorCode.CONST_METHOD, modifiers.getConstKeyword());
    }
    if (modifiers.getFactoryKeyword() != null) {
      reportError(ParserErrorCode.NON_CONSTRUCTOR_FACTORY, modifiers.getFactoryKeyword());
    }
    if (modifiers.getFinalKeyword() != null) {
      reportError(ParserErrorCode.FINAL_METHOD, modifiers.getFinalKeyword());
    }
    if (modifiers.getStaticKeyword() != null) {
      reportError(ParserErrorCode.STATIC_OPERATOR, modifiers.getStaticKeyword());
    }
    if (modifiers.getVarKeyword() != null) {
      reportError(ParserErrorCode.VAR_RETURN_TYPE, modifiers.getVarKeyword());
    }
  }

  /**
   * Validate that the given set of modifiers is appropriate for a top-level declaration.
   * 
   * @param modifiers the modifiers being validated
   */
  private void validateModifiersForTopLevelDeclaration(Modifiers modifiers) {
    if (modifiers.getFactoryKeyword() != null) {
      reportError(ParserErrorCode.FACTORY_TOP_LEVEL_DECLARATION, modifiers.getFactoryKeyword());
    }
    if (modifiers.getStaticKeyword() != null) {
      reportError(ParserErrorCode.STATIC_TOP_LEVEL_DECLARATION, modifiers.getStaticKeyword());
    }
  }

  /**
   * Validate that the given set of modifiers is appropriate for a top-level function.
   * 
   * @param modifiers the modifiers being validated
   */
  private void validateModifiersForTopLevelFunction(Modifiers modifiers) {
    validateModifiersForTopLevelDeclaration(modifiers);
    if (modifiers.getAbstractKeyword() != null) {
      reportError(ParserErrorCode.ABSTRACT_TOP_LEVEL_FUNCTION);
    }
    if (modifiers.getConstKeyword() != null) {
      reportError(ParserErrorCode.CONST_CLASS, modifiers.getConstKeyword());
    }
    if (modifiers.getFinalKeyword() != null) {
      reportError(ParserErrorCode.FINAL_CLASS, modifiers.getFinalKeyword());
    }
    if (modifiers.getVarKeyword() != null) {
      reportError(ParserErrorCode.VAR_RETURN_TYPE, modifiers.getVarKeyword());
    }
  }

  /**
   * Validate that the given set of modifiers is appropriate for a field and return the 'final',
   * 'const' or 'var' keyword if there is one.
   * 
   * @param modifiers the modifiers being validated
   * @return the 'final', 'const' or 'var' keyword associated with the field
   */
  private Token validateModifiersForTopLevelVariable(Modifiers modifiers) {
    validateModifiersForTopLevelDeclaration(modifiers);
    if (modifiers.getAbstractKeyword() != null) {
      reportError(ParserErrorCode.ABSTRACT_TOP_LEVEL_VARIABLE);
    }
    if (modifiers.getExternalKeyword() != null) {
      reportError(ParserErrorCode.EXTERNAL_FIELD, modifiers.getExternalKeyword());
    }
    Token constKeyword = modifiers.getConstKeyword();
    Token finalKeyword = modifiers.getFinalKeyword();
    Token varKeyword = modifiers.getVarKeyword();
    if (constKeyword != null) {
      if (finalKeyword != null) {
        reportError(ParserErrorCode.CONST_AND_FINAL, finalKeyword);
      }
      if (varKeyword != null) {
        reportError(ParserErrorCode.CONST_AND_VAR, varKeyword);
      }
    } else if (finalKeyword != null) {
      if (varKeyword != null) {
        reportError(ParserErrorCode.FINAL_AND_VAR, varKeyword);
      }
    }
    return lexicallyFirst(constKeyword, finalKeyword, varKeyword);
  }

  /**
   * Validate that the given set of modifiers is appropriate for a class and return the 'abstract'
   * keyword if there is one.
   * 
   * @param modifiers the modifiers being validated
   */
  private void validateModifiersForTypedef(Modifiers modifiers) {
    validateModifiersForTopLevelDeclaration(modifiers);
    if (modifiers.getAbstractKeyword() != null) {
      reportError(ParserErrorCode.ABSTRACT_TYPEDEF, modifiers.getAbstractKeyword());
    }
    if (modifiers.getConstKeyword() != null) {
      reportError(ParserErrorCode.CONST_TYPEDEF, modifiers.getConstKeyword());
    }
    if (modifiers.getExternalKeyword() != null) {
      reportError(ParserErrorCode.EXTERNAL_TYPEDEF, modifiers.getExternalKeyword());
    }
    if (modifiers.getFinalKeyword() != null) {
      reportError(ParserErrorCode.FINAL_TYPEDEF, modifiers.getFinalKeyword());
    }
    if (modifiers.getVarKeyword() != null) {
      reportError(ParserErrorCode.VAR_TYPEDEF, modifiers.getVarKeyword());
    }
  }
}
