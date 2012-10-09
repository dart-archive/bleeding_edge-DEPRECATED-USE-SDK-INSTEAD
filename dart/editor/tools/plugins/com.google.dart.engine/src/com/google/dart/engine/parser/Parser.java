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
import com.google.dart.engine.scanner.BeginToken;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Instances of the class {@code Parser} are used to parse tokens into an AST structure.
 */
public class Parser {
  /*
   * TODO(brianwilkerson) Find commented out references to the method reportError and uncomment
   * them.
   */

  /**
   * Instances of the class {@code FinalConstVarOrType} implement a simple data-holder for a method
   * that needs to return multiple values.
   */
  static class FinalConstVarOrType {
    /**
     * The 'final', 'const' or 'var' keyword, or {@code null} if none was given.
     */
    private Token keyword;

    /**
     * The type, of {@code null} if no type was specified.
     */
    private TypeName type;

    /**
     * Initialize a newly created holder with the given data.
     * 
     * @param keyword the 'final', 'const' or 'var' keyword
     * @param type the type
     */
    public FinalConstVarOrType(Token keyword, TypeName type) {
      this.keyword = keyword;
      this.type = type;
    }

    /**
     * Return the 'final', 'const' or 'var' keyword, or {@code null} if none was given.
     * 
     * @return the 'final', 'const' or 'var' keyword
     */
    public Token getKeyword() {
      return keyword;
    }

    /**
     * Return the type, of {@code null} if no type was specified.
     * 
     * @return the type
     */
    public TypeName getType() {
      return type;
    }
  }

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
   * <code>true</code> if the parser is currently in a loop.
   */
  private boolean inLoop = false;

  /**
   * <code>true</code> if the parser is currently in a switch statement.
   */
  private boolean inSwitch = false;

  private static final String HIDE = "hide"; //$NON-NLS-1$
  private static final String OF = "of"; //$NON-NLS-1$
  private static final String ON = "on"; //$NON-NLS-1$
  private static final String SHOW = "show"; //$NON-NLS-1$

  private static final ArrayList<Expression> EMPTY_EXPRESSION_LIST = new ArrayList<Expression>(0);

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
  public ArrayList<Statement> parseStatements(Token token) {
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
   * @param scalarValue the value to be appended
   * @param startIndex the index of the first character representing the scalar value
   * @param endIndex the index of the last character representing the scalar value
   */
  private void appendScalarValue(StringBuilder builder, int scalarValue, int startIndex,
      int endIndex) {
    if (scalarValue < 0 || scalarValue > Character.MAX_CODE_POINT
        || (scalarValue >= 0xD800 && scalarValue <= 0xDFFF)) {
      // Illegal escape sequence: invalid code point
      // reportError(ParserErrorCode.INVALID_CODE_POINT));
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
    if (lexeme.startsWith("@\"\"\"") || lexeme.startsWith("@'''")) { //$NON-NLS-1$ //$NON-NLS-2$
      return lexeme.substring(4, lexeme.length() - 3);
    } else if (lexeme.startsWith("@\"") || lexeme.startsWith("@'")) { //$NON-NLS-1$ //$NON-NLS-2$
      return lexeme.substring(2, lexeme.length() - 1);
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
   * Create and return a new synthetic SimpleIdentifier.
   * 
   * @return a new synthetic SimpleIdentifier
   */
  private SimpleIdentifier createSyntheticSimpleIdentifier() {
    return new SimpleIdentifier(new StringToken(TokenType.IDENTIFIER, "", currentToken.getOffset()));
  }

  /**
   * Create and return a new synthetic SimpleStringLiteral.
   * 
   * @return a new synthetic SimpleStringLiteral
   */
  private SimpleStringLiteral createSyntheticSimpleStringLiteral() {
    return new SimpleStringLiteral(
        new StringToken(TokenType.STRING, "", currentToken.getOffset()),
        "");
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
   * Return {@code true} if the current token appears to be the beginning of a function expression.
   * 
   * @return {@code true} if the current token appears to be the beginning of a function expression
   */
  private boolean isFunctionExpression() {
    if (matches(Keyword.VOID)) {
      return true;
    }
    Token afterReturnType = skipReturnType(currentToken);
    if (afterReturnType == null) {
      // There was no return type, but it is optional, so go back to where we started.
      afterReturnType = currentToken;
    }
    Token afterIdentifier = skipSimpleIdentifier(afterReturnType);
    if (afterIdentifier == null) {
      // There was no name, so go back to the end of the return type
      afterIdentifier = afterReturnType;
    }
    if (afterIdentifier.getType() == TokenType.OPEN_PAREN) {
      Token openParen = afterIdentifier;
      if (openParen instanceof BeginToken) {
        Token closeParen = ((BeginToken) openParen).getEndToken();
        if (closeParen != null) {
          Token next = closeParen.getNext();
          return next.getType() == TokenType.OPEN_CURLY_BRACKET
              || next.getType() == TokenType.FUNCTION;
        }
      }
    }
    return false;
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
   * declaration rather than an expression.
   * 
   * <pre>
   * initializedVariableDeclaration ::=
   *     declaredIdentifier ('=' expression)? (',' initializedIdentifier)*
   * 
   * declaredIdentifier ::=
   *     finalConstVarOrType identifier
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
      // An expression cannot start with a keyword.
      return true;
    }
    // We know that we have an identifier, and need to see whether it might be a type name.
    Token token = skipTypeName(currentToken);
    if (token == null) {
      // There was no type name, so this can't be a declaration.
      return false;
    }
    token = skipSimpleIdentifier(token);
    return token != null && token.getType() != TokenType.OPEN_PAREN;
  }

  /**
   * Return {@code true} if the current token appears to be the beginning of a switch member.
   * 
   * @return {@code true} if the current token appears to be the beginning of a switch member
   */
  private boolean isSwitchMember() {
    Token token = currentToken;
    while (token.getType() == TokenType.IDENTIFIER && token.getNext().getType() == TokenType.COLON) {
      token = token.getNext().getNext();
    }
    if (token.getType() == TokenType.KEYWORD) {
      Keyword keyword = ((KeywordToken) token).getKeyword();
      return keyword == Keyword.CASE || keyword == Keyword.DEFAULT;
    }
    return false;
  }

  /**
   * Return {@code true} if the current token matches the given keyword.
   * 
   * @param keyword the keyword that can optionally appear in the current location
   * @return {@code true} if the current token matches the given keyword
   */
  private boolean matches(Keyword keyword) {
    return currentToken.getType() == TokenType.KEYWORD
        && ((KeywordToken) currentToken).getKeyword() == keyword;
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
   * Return {@code true} if the current token has the given type.
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
   * Return {@code true} if the current token is a valid identifier. Valid identifiers include
   * built-in identifiers (pseudo-keywords).
   * 
   * @return {@code true} if the current token is a valid identifier
   */
  private boolean matchesIdentifier() {
    return matches(TokenType.IDENTIFIER)
        || (matches(TokenType.KEYWORD) && ((KeywordToken) currentToken).getKeyword().isPseudoKeyword());
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
   * metadata ::=
   *     annotation*
   * 
   * annotation ::=
   *     '@' {@link Identifier qualified} (‘.’ {@link SimpleIdentifier identifier})? {@link ArgumentList arguments}?
   * </pre>
   * 
   * @return the annotation that was parsed
   */
  // TODO (jwren) have this method called by the parser and remove the SuppressWarnings annotation
  @SuppressWarnings("unused")
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
    if (matchesIdentifier() && peekMatches(TokenType.COLON)) {
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
    Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
    return new ArgumentList(leftParenthesis, arguments, rightParenthesis);
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
   * @return the assignable expression that was parsed
   */
  private Expression parseAssignableExpression(boolean orPrimaryWithSelectors) {
    if (matches(Keyword.SUPER)) {
      return parseAssignableSelector(new SuperExpression(getAndAdvance()), false);
    }
    //
    // A primary expression can start with an identifier. We resolve the ambiguity by determining
    // whether the primary consists of anything other than an identifier and/or is followed by an
    // assignableSelector.
    //
    Expression expression = parsePrimaryExpression();
    boolean isOptional = orPrimaryWithSelectors || expression instanceof SimpleIdentifier;
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
        if (!orPrimaryWithSelectors) {
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
      return new ArrayAccess(prefix, leftBracket, index, rightBracket);
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
    if (matches(Keyword.SUPER) && peekMatches(TokenType.AMPERSAND)) {
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
    if (matches(Keyword.SUPER) && peekMatches(TokenType.BAR)) {
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
    if (matches(Keyword.SUPER) && peekMatches(TokenType.CARET)) {
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
    if (currentToken.getType() == TokenType.IDENTIFIER) {
      functionName = parseSimpleIdentifier();
    } else if (currentToken.getType() == TokenType.OPEN_SQUARE_BRACKET) {
      Token leftBracket = getAndAdvance();
      Expression index = parseExpression();
      Token rightBracket = expect(TokenType.CLOSE_SQUARE_BRACKET);
      expression = new ArrayAccess(period, leftBracket, index, rightBracket);
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
   *     'abstract'? 'class' name typeParameterList? extendsClause? implementsClause? '{' memberDefinition* '}'
   * </pre>
   * 
   * @return the class declaration that was parsed
   */
  private ClassDeclaration parseClassDeclaration() {
    Comment comment = parseDocumentationComment();
    Token abstractKeyword = null;
    if (matches(Keyword.ABSTRACT)) {
      abstractKeyword = getAndAdvance();
    }
    Token keyword = expect(Keyword.CLASS);
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
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
      members.add(parseClassMember());
      if (currentToken == memberStart) {
        reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
        advance();
      }
      memberStart = currentToken;
    }
    Token rightBracket = expect(TokenType.CLOSE_CURLY_BRACKET);
    return new ClassDeclaration(
        comment,
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
   * @return the class member that was parsed
   */
  private ClassMember parseClassMember() {
    Comment comment = parseDocumentationComment();
    // TODO(brianwilkerson) The following condition exists for backward compatibility and might want
    // to be removed before shipping.
    if (matches(Keyword.ABSTRACT)) {
      // reportError(ParserErrorCode.?);
      advance();
    }
    Token externalKeyword = null;
    if (matches(Keyword.EXTERNAL)) {
      externalKeyword = getAndAdvance();
    }
    if (matches(Keyword.CONST)) {
      if (peekMatchesIdentifier()) {
        if (peekMatches(2, TokenType.OPEN_PAREN)) {
          return parseConstantConstructor(comment, externalKeyword);
        } else if (peekMatches(2, TokenType.PERIOD) && peekMatches(4, TokenType.OPEN_PAREN)) {
          return parseConstantConstructor(comment, externalKeyword);
        }
      }
      if (externalKeyword != null) {
        // reportError(ParserErrorCode.?);
      }
      return new FieldDeclaration(
          comment,
          null,
          parseVariableDeclarationList(),
          expect(TokenType.SEMICOLON));
    } else if (matches(Keyword.FACTORY)) {
      return parseFactoryConstructor(comment, externalKeyword);
    } else if (matches(Keyword.FINAL)) {
      if (externalKeyword != null) {
        // reportError(ParserErrorCode.?);
      }
      return new FieldDeclaration(
          comment,
          null,
          parseVariableDeclarationList(),
          expect(TokenType.SEMICOLON));
    }
    Token staticKeyword = null;
    if (matches(Keyword.STATIC)) {
      if (peekMatches(Keyword.FINAL) || peekMatches(Keyword.CONST)) {
        if (externalKeyword != null) {
          // reportError(ParserErrorCode.?);
        }
        return new FieldDeclaration(
            comment,
            getAndAdvance(),
            parseVariableDeclarationList(),
            expect(TokenType.SEMICOLON));
      }
      staticKeyword = getAndAdvance();
    }
    if (matches(Keyword.VAR)) {
      if (externalKeyword != null) {
        // reportError(ParserErrorCode.?);
      }
      return parseInitializedIdentifierList(comment, staticKeyword, getAndAdvance(), null);
    } else if (matches(Keyword.GET)) {
      return parseGetter(comment, externalKeyword, staticKeyword, null);
    } else if (matches(Keyword.SET)) {
      return parseSetter(comment, externalKeyword, staticKeyword, null);
    } else if (matches(Keyword.OPERATOR)) {
      if (staticKeyword != null) {
        reportError(ParserErrorCode.STATIC_OPERATOR, staticKeyword);
      }
      return parseOperator(comment, externalKeyword, null);
    } else if (matchesIdentifier()) {
      if (peekMatches(TokenType.OPEN_PAREN)) {
        if (staticKeyword != null) {
          return parseMethodDeclaration(comment, externalKeyword, staticKeyword);
        }
        return parseMethodOrConstructor(comment, externalKeyword, staticKeyword, null);
      } else if (peekMatches(TokenType.PERIOD) && peekMatches(3, TokenType.OPEN_PAREN)) {
        SimpleIdentifier returnType = parseSimpleIdentifier();
        Token period = getAndAdvance();
        SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
        return parseConstructor(
            comment,
            externalKeyword,
            staticKeyword,
            returnType,
            period,
            name,
            parseFormalParameterList());
      }
    }
    TypeName returnType = parseReturnType();
    if (matches(Keyword.GET)) {
      return parseGetter(comment, externalKeyword, staticKeyword, returnType);
    } else if (matches(Keyword.SET)) {
      return parseSetter(comment, externalKeyword, staticKeyword, returnType);
    } else if (matches(Keyword.OPERATOR)) {
      if (staticKeyword != null) {
        reportError(ParserErrorCode.STATIC_OPERATOR, staticKeyword);
      }
      return parseOperator(comment, externalKeyword, returnType);
    }
    if (peekMatches(TokenType.PERIOD) || peekMatches(TokenType.OPEN_PAREN)) {
      return parseMethodOrConstructor(comment, externalKeyword, staticKeyword, returnType);
    }
    if (externalKeyword != null) {
      // reportError(ParserErrorCode.?);
    }
    return parseInitializedIdentifierList(comment, staticKeyword, null, returnType);
  }

  /**
   * Parse a comment reference from the source between square brackets.
   * 
   * <pre>
   * commentReference ::=
   *     prefixedIdentifier
   * </pre>
   * 
   * @param referenceSource the source occurring between the square brackets within a documentation
   *          comment
   * @param sourceOffset the offset of the first character of the reference source
   * @return the comment reference that was parsed
   */
  private CommentReference parseCommentReference(String referenceSource, int sourceOffset) {
    try {
      final boolean[] errorFound = {false};
      AnalysisErrorListener listener = new AnalysisErrorListener() {
        @Override
        public void onError(AnalysisError error) {
          errorFound[0] = true;
        }
      };
      StringScanner scanner = new StringScanner(null, referenceSource, listener);
      Token firstToken = scanner.tokenize();
      if (!errorFound[0] && firstToken.getType() == TokenType.IDENTIFIER) {
        firstToken.setOffset(firstToken.getOffset() + sourceOffset);
        Token secondToken = firstToken.getNext();
        Token thirdToken = secondToken.getNext();
        Token nextToken;
        Identifier identifier;
        if (secondToken.getType() == TokenType.PERIOD
            && thirdToken.getType() == TokenType.IDENTIFIER) {
          secondToken.setOffset(secondToken.getOffset() + sourceOffset);
          thirdToken.setOffset(thirdToken.getOffset() + sourceOffset);
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
          // reportError(ParserErrorCode.?);
        }
        return new CommentReference(identifier);
      }
    } catch (Exception exception) {
      // reportError(ParserErrorCode.?);
    }
    return null;
  }

  /**
   * Parse all of the comment references occurring in the given array of documentation comments.
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
          CommentReference reference = parseCommentReference(
              comment.substring(leftIndex + 1, rightIndex),
              token.getOffset() + leftIndex + 1);
          if (reference != null) {
            references.add(reference);
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
   * 
   * <pre>
   * compilationUnit ::=
   *     scriptTag? directives* topLevelDeclaration*
   * </pre>
   * 
   * @return the compilation unit that was parsed
   */
  private CompilationUnit parseCompilationUnit() {
    ScriptTag scriptTag = null;
    if (matches(TokenType.SCRIPT_TAG)) {
      scriptTag = new ScriptTag(getAndAdvance());
    }
    boolean libraryDirectiveFound = false;
    boolean declarationFound = false;
    boolean errorGenerated = false;
    List<Directive> directives = new ArrayList<Directive>();
    List<CompilationUnitMember> declarations = new ArrayList<CompilationUnitMember>();
    Token memberStart = currentToken;
    while (!matches(TokenType.EOF)) {
      if (matches(Keyword.IMPORT) || matches(Keyword.EXPORT) || matches(Keyword.LIBRARY)
          || matches(Keyword.PART)) {
        if (declarationFound && !errorGenerated) {
          // reportError(ParserErrorCode.?);
          errorGenerated = true;
        }
        Directive directive = parseDirective();
        if (directive instanceof LibraryDirective) {
          if (libraryDirectiveFound) {
            reportError(ParserErrorCode.MULTIPLE_LIBRARY_DIRECTIVES);
          } else if (directives.size() > 0 || declarations.size() > 0) {
            reportError(ParserErrorCode.LIBRARY_DIRECTIVE_FIRST);
          } else {
            libraryDirectiveFound = true;
          }
        } else if (declarations.size() > 0) {
          reportError(ParserErrorCode.DIRECTIVE_AFTER_DECLARATION);
        }
        directives.add(directive);
      } else {
        CompilationUnitMember member = parseCompilationUnitMember();
        if (member != null) {
          declarations.add(member);
          declarationFound = true;
        }
      }
      if (currentToken == memberStart) {
        reportError(ParserErrorCode.UNEXPECTED_TOKEN, currentToken, currentToken.getLexeme());
        advance();
      }
      memberStart = currentToken;
    }
    // Check the order of the directives.
    // reportError(ParserErrorCode.?);
    return new CompilationUnit(scriptTag, directives, declarations);
  }

  /**
   * Parse a compilation unit member.
   * 
   * <pre>
   * compilationUnitMember ::=
   *     classDefinition
   *   | functionTypeAlias
   *   | functionSignature functionBody
   *   | returnType? getOrSet identifier formalParameterList functionBody
   *   | (final | const) type? staticFinalDeclarationList ';'
   *   | variableDeclaration ';'
   * </pre>
   * 
   * @return the compilation unit member that was parsed
   */
  private CompilationUnitMember parseCompilationUnitMember() {
    if (matches(Keyword.STATIC)) {
      reportError(ParserErrorCode.STATIC_TOP_LEVEL_DECLARATION);
      advance();
    }
    if (matches(Keyword.ABSTRACT) || matches(Keyword.CLASS)) {
      return parseClassDeclaration();
    } else if (matches(Keyword.TYPEDEF)) {
      return parseTypeAlias();
    }
    Comment comment = parseDocumentationComment();
    if (matches(Keyword.CONST) || matches(Keyword.FINAL) || matches(Keyword.VAR)) {
      return new TopLevelVariableDeclaration(
          comment,
          parseVariableDeclarationList(),
          expect(TokenType.SEMICOLON));
    } else if (matches(Keyword.GET) || matches(Keyword.SET)) {
      return parseFunctionDeclaration(comment, null);
    } else if (matchesIdentifier() && peekMatches(TokenType.OPEN_PAREN)) {
      return parseFunctionDeclaration(comment, null);
    }
    TypeName returnType = parseReturnType();
    if (matches(Keyword.GET) || matches(Keyword.SET)) {
      return parseFunctionDeclaration(comment, returnType);
    } else if (matchesIdentifier() && peekMatches(TokenType.OPEN_PAREN)) {
      return parseFunctionDeclaration(comment, returnType);
    }
    return new TopLevelVariableDeclaration(
        comment,
        parseVariableDeclarationList(returnType),
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
   * Parse a constant constructor.
   * 
   * <pre>
   * constantConstructor ::=
   *     constantConstructorSignature (redirection | initializers)? ';'
   * 
   * constantConstructorSignature ::=
   *     'external'? 'const' qualified formalParameterList
   * 
   * initializers ::=
   *     ':' superCallOrFieldInitializer (',' superCallOrFieldInitializer)*
   * 
   * superCallOrFieldInitializer ::=
   *     superConstructorInvocation
   *   | fieldInitializer
   * 
   * redirection ::=
   *     ':' redirectingConstructorInvocation
   * </pre>
   * 
   * @param comment the documentation comment to be associated with the declaration
   * @param externalKeyword the 'external' token
   * @return the constant constructor that was parsed
   */
  private ConstructorDeclaration parseConstantConstructor(Comment comment, Token externalKeyword) {
    Token keyword = expect(Keyword.CONST);
    SimpleIdentifier returnType = parseSimpleIdentifier();
    // TODO(brianwilkerson) Validate that the return type is the same name as the class in which
    // the constructor is being declared.
    Token period = null;
    SimpleIdentifier name = null;
    if (matches(TokenType.PERIOD)) {
      period = getAndAdvance();
      name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
    }
    return parseConstructor(
        comment,
        externalKeyword,
        keyword,
        returnType,
        period,
        name,
        parseFormalParameterList());
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

  private ConstructorDeclaration parseConstructor(Comment comment, Token externalKeyword,
      Token keyword, SimpleIdentifier returnType, Token period, SimpleIdentifier name,
      FormalParameterList parameters) {
    boolean bodyAllowed = externalKeyword == null;
    Token colon = null;
    List<ConstructorInitializer> initializers = new ArrayList<ConstructorInitializer>();
    if (matches(TokenType.COLON)) {
      colon = getAndAdvance();
      do {
        if (matches(Keyword.THIS)) {
          if (peekMatches(TokenType.OPEN_PAREN)) {
            bodyAllowed = false;
            initializers.add(parseRedirectingConstructorInvocation());
          } else if (peekMatches(TokenType.PERIOD) && peekMatches(3, TokenType.OPEN_PAREN)) {
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
    FunctionBody body = parseFunctionBody(true, false);
    if (!bodyAllowed && !(body instanceof EmptyFunctionBody)) {
      // reportError(ParserErrorCode.?);
    }
    return new ConstructorDeclaration(
        comment,
        externalKeyword,
        keyword,
        returnType,
        period,
        name,
        parameters,
        colon,
        initializers,
        body);
  }

  /**
   * Parse a field initializer within a constructor.
   * 
   * <pre>
   * fieldInitializer:
   *     ('this' '.')? identifier '=' conditionalExpression
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
    return new ConstructorFieldInitializer(keyword, period, fieldName, equals, expression);
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
   * @return the directive that was parsed
   */
  private Directive parseDirective() {
    if (matches(Keyword.IMPORT)) {
      return parseImportDirective();
    } else if (matches(Keyword.EXPORT)) {
      return parseExportDirective();
    } else if (matches(Keyword.LIBRARY)) {
      return parseLibraryDirective();
    } else if (matches(Keyword.PART)) {
      return parsePartDirective();
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
   *     'export' stringLiteral combinator*';'
   * 
   * combinator ::=
   *     'show' identifier (',' identifier)*
   *   | 'hide' identifier (',' identifier)*
   * </pre>
   * 
   * @return the export directive that was parsed
   */
  private ExportDirective parseExportDirective() {
    Token exportKeyword = expect(Keyword.EXPORT);
    StringLiteral libraryUri = parseStringLiteral();
    List<ImportCombinator> combinators = new ArrayList<ImportCombinator>();
    while (matches(SHOW) || matches(HIDE)) {
      Token kind = expect(TokenType.IDENTIFIER);
      if (kind.getLexeme().equals(SHOW)) {
        List<Identifier> shownNames = parseIdentifierList();
        combinators.add(new ImportShowCombinator(kind, shownNames));
      } else {
        List<Identifier> hiddenNames = parseIdentifierList();
        combinators.add(new ImportHideCombinator(kind, hiddenNames));
      }
    }
    Token semicolon = expect(TokenType.SEMICOLON);
    return new ExportDirective(exportKeyword, libraryUri, combinators, semicolon);
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
   * Parse a factory constructor.
   * 
   * <pre>
   * factoryConstructor ::=
   *     factoryConstructorSignature functionBody
   * 
   * factoryConstructorSignature ::=
   *     'external'? 'factory' qualified  ('.' identifier)? formalParameterList
   * </pre>
   * 
   * @param comment the documentation comment to be associated with the declaration
   * @param externalKeyword the 'external' token
   * @return the factory constructor that was parsed
   */
  private ConstructorDeclaration parseFactoryConstructor(Comment comment, Token externalKeyword) {
    Token keyword = expect(Keyword.FACTORY);
    Identifier returnType = parseSimpleIdentifier();
    Token period = null;
    SimpleIdentifier name = null;
    if (matches(TokenType.PERIOD)) {
      period = getAndAdvance();
      name = parseSimpleIdentifier();
      if (matches(TokenType.PERIOD)) {
        returnType = new PrefixedIdentifier((SimpleIdentifier) returnType, period, name);
        period = getAndAdvance();
        name = parseSimpleIdentifier();
      }
    }
    FormalParameterList parameters = parseFormalParameterList();
    Token colon = null;
    List<ConstructorInitializer> initializers = new ArrayList<ConstructorInitializer>();
    FunctionBody body = parseFunctionBody(true, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      // reportError(ParserErrorCode.?);
    }
    return new ConstructorDeclaration(
        comment,
        externalKeyword,
        keyword,
        returnType,
        period,
        name,
        parameters,
        colon,
        initializers,
        body);
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
      if (peekMatchesIdentifier() || peekMatches(TokenType.LT) || peekMatches(Keyword.THIS)) {
        type = parseTypeName();
      }
    } else if (matches(Keyword.VAR)) {
      keyword = getAndAdvance();
    } else {
      if (peekMatchesIdentifier() || peekMatches(TokenType.LT) || peekMatches(Keyword.THIS)) {
        type = parseReturnType();
      } else if (!optional) {
        // reportError(ParserErrorCode.?);
      }
    }
    return new FinalConstVarOrType(keyword, type);
  }

  /**
   * Parse a formal parameter.
   * 
   * <pre>
   * defaultFormalParameter:
   *     normalFormalParameter ('=' expression)?
   * </pre>
   * 
   * @return the formal parameter that was parsed
   */
  private FormalParameter parseFormalParameter() {
    NormalFormalParameter parameter = parseNormalFormalParameter();
    if (matches(TokenType.EQ)) {
      // Validate that these are only used for optional parameters.
      // reportError(ParserErrorCode.?);
      Token equals = getAndAdvance();
      Expression defaultValue = parseExpression();
      return new NamedFormalParameter(parameter, equals, defaultValue);
    }
    return parameter;
  }

  /**
   * Parse a list of formal parameters.
   * 
   * <pre>
   * formalParameterList ::=
   *     '(' ')'
   *   | '(' normalFormalParameters (',' namedFormalParameters)? ')'
   *   | '(' namedFormalParameters ')'
   *
   * normalFormalParameters ::=
   *     normalFormalParameter (',' normalFormalParameter)*
   *
   * namedFormalParameters ::=
   *     '[' defaultFormalParameter (',' defaultFormalParameter)* ']'
   * </pre>
   * 
   * @return the formal parameters that were parsed
   */
  private FormalParameterList parseFormalParameterList() {
    Token leftParenthesis = expect(TokenType.OPEN_PAREN);
    List<FormalParameter> parameters = new ArrayList<FormalParameter>();
    if (matches(TokenType.CLOSE_PAREN)) {
      return new FormalParameterList(leftParenthesis, parameters, null, null, getAndAdvance());
    }
    //
    // Even though it is invalid to have named parameters outside the square brackets, or unnamed
    // parameters inside the square brackets, we allow both in order to recover better.
    //
    Token leftBracket = null;
    do {
      if (matches(TokenType.OPEN_SQUARE_BRACKET)) {
        if (leftBracket != null) {
          // reportError(ParserErrorCode.?);
        }
        leftBracket = getAndAdvance();
      }
      FormalParameter parameter = parseFormalParameter();
      if (leftBracket == null) {
        if (parameter instanceof NamedFormalParameter) {
          // reportError(ParserErrorCode.?);
        }
      } else {
        if (!(parameter instanceof NamedFormalParameter)) {
          // reportError(ParserErrorCode.?);
        }
      }
      parameters.add(parameter);
    } while (optional(TokenType.COMMA));
    Token rightBracket = leftBracket == null ? null : expect(TokenType.CLOSE_SQUARE_BRACKET);
    Token rightParenthesis = expect(TokenType.CLOSE_PAREN);
    return new FormalParameterList(
        leftParenthesis,
        parameters,
        leftBracket,
        rightBracket,
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
        if (matchesIdentifier() && peekMatches(Keyword.IN)) {
          List<VariableDeclaration> variables = new ArrayList<VariableDeclaration>();
          SimpleIdentifier variableName = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_VARIABLE_NAME);
          variables.add(new VariableDeclaration(null, variableName, null, null));
          variableList = new VariableDeclarationList(null, null, variables);
        } else if (isInitializedVariableDeclaration()) {
          variableList = parseVariableDeclarationList();
        } else {
          initialization = parseExpression();
        }
        if (matches(Keyword.IN)) {
          SimpleFormalParameter loopParameter = null;
          if (variableList == null) {
            // We found: expression 'in'
            // reportError(ParserErrorCode.?);
          } else {
            NodeList<VariableDeclaration> variables = variableList.getVariables();
            if (variables.size() > 1) {
              // reportError(ParserErrorCode.?);
            }
            VariableDeclaration variable = variables.get(0);
            if (variable.getInitializer() != null) {
              // reportError(ParserErrorCode.?);
            }
            loopParameter = new SimpleFormalParameter(
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
        // Ensure that the loop parameter is not final.
        // reportError(ParserErrorCode.?);
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
   * </pre>
   * 
   * @param mayBeEmpty {@code true} if the function body is allowed to be empty
   * @param inExpression {@code true} if the function body is being parsed as part of an expression
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
          // reportError(ParserErrorCode.?);
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
        // reportError(ParserErrorCode.?);
        return null;
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
   * @param comment the documentation comment to be associated with the declaration
   * @param returnType the return type, or {@code null} if there is no return type
   * @return the function declaration that was parsed
   */
  private FunctionDeclaration parseFunctionDeclaration(Comment comment, TypeName returnType) {
    Token keyword = null;
    boolean isGetter = false;
    if (matches(Keyword.GET)) {
      keyword = getAndAdvance();
      isGetter = true;
    } else if (matches(Keyword.SET)) {
      keyword = getAndAdvance();
    }
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
    FormalParameterList parameters = null;
    if (!isGetter) {
      parameters = parseFormalParameterList();
    }
    FunctionBody body = parseFunctionBody(false, false);
    return new FunctionDeclaration(comment, keyword, new FunctionExpression(
        returnType,
        name,
        parameters,
        body));
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
    return new FunctionDeclarationStatement(parseFunctionDeclaration(
        parseDocumentationComment(),
        parseReturnType()));
  }

  /**
   * Parse a function expression.
   * 
   * <pre>
   * functionExpression ::=
   *     (returnType? identifier)? formalParameterList functionExpressionBody
   * 
   * functionExpressionBody ::=
   *     '=>' expression
   *   | block
   * </pre>
   * 
   * @return the function expression that was parsed
   */
  private FunctionExpression parseFunctionExpression() {
    TypeName returnType = null;
    if (matches(Keyword.VOID)
        || (matchesIdentifier() && (peekMatchesIdentifier() || peekMatches(TokenType.LT)))) {
      returnType = parseReturnType();
    }
    SimpleIdentifier name = null;
    if (matchesIdentifier()) {
      name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
    }
    FormalParameterList parameters = parseFormalParameterList();
    FunctionBody body = parseFunctionBody(false, true);
    return new FunctionExpression(returnType, name, parameters, body);
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
   * @param comment the documentation comment to be associated with the declaration
   * @param externalKeyword the 'external' token
   * @param staticKeyword the static keyword, or {@code null} if the getter is not static
   * @param the return type that has already been parsed, or {@code null} if there was no return
   *          type
   * @return the getter that was parsed
   */
  private MethodDeclaration parseGetter(Comment comment, Token externalKeyword,
      Token staticKeyword, TypeName returnType) {
    Token propertyKeyword = expect(Keyword.GET);
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
    if (matches(TokenType.OPEN_PAREN) && peekMatches(TokenType.CLOSE_PAREN)) {
      // reportError(ParserErrorCode.GETTER_WITH_PARAMETERS);
      advance();
      advance();
    }
    FunctionBody body = parseFunctionBody(true, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      // reportError(ParserErrorCode.?);
    }
    return new MethodDeclaration(
        comment,
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
  private List<Identifier> parseIdentifierList() {
    List<Identifier> identifiers = new ArrayList<Identifier>();
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
   *     'import' stringLiteral ('as' identifier)? combinator*';'
   * 
   * combinator ::=
   *     'show' identifier (',' identifier)*
   *   | 'hide' identifier (',' identifier)*
   * </pre>
   * 
   * @return the import directive that was parsed
   */
  private ImportDirective parseImportDirective() {
    Token importKeyword = expect(Keyword.IMPORT);
    StringLiteral libraryUri = parseStringLiteral();
    Token asToken = null;
    SimpleIdentifier prefix = null;
    if (matches(Keyword.AS)) {
      asToken = getAndAdvance();
      prefix = parseSimpleIdentifier();
    }
    List<ImportCombinator> combinators = new ArrayList<ImportCombinator>();
    while (matches(SHOW) || matches(HIDE)) {
      Token kind = expect(TokenType.IDENTIFIER);
      if (kind.getLexeme().equals(SHOW)) {
        List<Identifier> shownNames = parseIdentifierList();
        combinators.add(new ImportShowCombinator(kind, shownNames));
      } else {
        List<Identifier> hiddenNames = parseIdentifierList();
        combinators.add(new ImportHideCombinator(kind, hiddenNames));
      }
    }
    Token semicolon = expect(TokenType.SEMICOLON);
    return new ImportDirective(importKeyword, libraryUri, asToken, prefix, combinators, semicolon);
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
   * @param comment the documentation comment to be associated with the declaration
   * @param staticKeyword the static keyword, or {@code null} if the getter is not static
   * @param the 'var' keyword, or {@code null} if a type was provided
   * @param type the type that has already been parsed, or {@code null} if 'var' was provided
   * @return the getter that was parsed
   */
  private FieldDeclaration parseInitializedIdentifierList(Comment comment, Token staticKeyword,
      Token varKeyword, TypeName type) {
    VariableDeclarationList fieldList = parseVariableDeclarationList(varKeyword, type);
    return new FieldDeclaration(comment, staticKeyword, fieldList, expect(TokenType.SEMICOLON));
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
    TypeName type = parseTypeName();
    Token period = null;
    SimpleIdentifier identifier = null;
    if (matches(TokenType.PERIOD)) {
      period = getAndAdvance();
      identifier = parseSimpleIdentifier();
    }
    ArgumentList argumentList = parseArgumentList();
    return new InstanceCreationExpression(keyword, type, period, identifier, argumentList);
  }

  /**
   * Parse a library directive.
   * 
   * <pre>
   * libraryDirective ::=
   *     'library' qualified ';'
   * </pre>
   * 
   * @return the library directive that was parsed
   */
  private LibraryDirective parseLibraryDirective() {
    Token keyword = expect(Keyword.LIBRARY);
    Identifier libraryName = parsePrefixedIdentifier();
    Token semicolon = expect(TokenType.SEMICOLON);
    return new LibraryDirective(keyword, libraryName, semicolon);
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
      Token leftBracket = new Token(TokenType.OPEN_SQUARE_BRACKET, currentToken.getOffset());
      Token rightBracket = new Token(TokenType.CLOSE_SQUARE_BRACKET, currentToken.getOffset() + 1);
      rightBracket.setNext(currentToken.getNext());
      leftBracket.setNext(rightBracket);
      currentToken.getPrevious().setNext(leftBracket);
      currentToken = currentToken.getNext();
      return new ListLiteral(
          modifier,
          typeArguments,
          leftBracket,
          EMPTY_EXPRESSION_LIST,
          rightBracket);
    }
    Token leftBracket = expect(TokenType.OPEN_SQUARE_BRACKET);
    if (matches(TokenType.CLOSE_SQUARE_BRACKET)) {
      return new ListLiteral(
          modifier,
          typeArguments,
          leftBracket,
          EMPTY_EXPRESSION_LIST,
          getAndAdvance());
    }
    ArrayList<Expression> elements = new ArrayList<Expression>();
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
    // TODO (jwren) return a synthetic node?
    return null;
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
    ArrayList<MapLiteralEntry> entries = new ArrayList<MapLiteralEntry>();
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
   * @param comment the documentation comment to be associated with the declaration
   * @param externalKeyword the 'external' token
   * @param staticKeyword the static keyword, or {@code null} if the getter is not static
   * @return the method declaration that was parsed
   */
  private MethodDeclaration parseMethodDeclaration(Comment comment, Token externalKeyword,
      Token staticKeyword) {
    TypeName returnType = null;
    if (!peekMatches(TokenType.OPEN_PAREN)) {
      returnType = parseReturnType();
    }
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
    FormalParameterList parameters = parseFormalParameterList();
    FunctionBody body = parseFunctionBody(staticKeyword == null, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      // reportError(ParserErrorCode.?);
    }
    return new MethodDeclaration(
        comment,
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
   * Parse either a method or a constructor declaration.
   * 
   * <pre>
   * methodOrConstructor ::=
   *     'static'? functionSignature functionBody
   *   | functionSignature ';'
   *   | constructorSignature (redirection | initializers)? ';'
   *   | constructorSignature initializers? functionBody
   * </pre>
   * 
   * @param comment the documentation comment to be associated with the declaration
   * @param externalKeyword the 'external' token
   * @param staticKeyword the static keyword, or {@code null} if the getter is not static
   * @param returnType the return type that was declared, or {@code null} if no return type was
   *          declared
   * @return the method or constructor declaration that was parsed
   */
  private ClassMember parseMethodOrConstructor(Comment comment, Token externalKeyword,
      Token staticKeyword, TypeName returnType) {
    if (matches(TokenType.PERIOD)) {
      if (staticKeyword != null) {
        // Constructors cannot be static
        // reportError(ParserErrorCode.?);
      }
      SimpleIdentifier realReturnType = null;
      if (returnType != null && returnType.getName() instanceof SimpleIdentifier) {
        realReturnType = (SimpleIdentifier) returnType.getName();
      } else {
        // reportError(ParserErrorCode.?);
      }
      Token period = getAndAdvance();
      SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
      FormalParameterList parameters = parseFormalParameterList();
      return parseConstructor(
          comment,
          externalKeyword,
          null,
          realReturnType,
          period,
          name,
          parameters);
    }
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
    FormalParameterList parameters = parseFormalParameterList();
    if (matches(TokenType.COLON)) {
      if (staticKeyword != null) {
        // Constructors cannot be static
        // reportError(ParserErrorCode.?);
      }
      SimpleIdentifier realReturnType = null;
      if (returnType == null) {
        realReturnType = name;
        name = null;
      } else if (returnType.getName() instanceof SimpleIdentifier) {
        realReturnType = (SimpleIdentifier) returnType.getName();
      } else {
        // reportError(ParserErrorCode.?);
      }
      return parseConstructor(
          comment,
          externalKeyword,
          null,
          realReturnType,
          null,
          name,
          parameters);
    }
    FunctionBody body = parseFunctionBody(staticKeyword == null, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      // reportError(ParserErrorCode.?);
    }
    return new MethodDeclaration(
        comment,
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
   *   | throwStatement
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
    if (matches(TokenType.OPEN_CURLY_BRACKET)) {
      if (peekMatches(TokenType.STRING)) {
        Token afterString = skipStringLiteral(currentToken.getNext());
        if (afterString != null && afterString.getType() == TokenType.COLON) {
          return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
        }
      }
      return parseBlock();
    } else if (matches(TokenType.KEYWORD)) {
      Keyword keyword = ((KeywordToken) currentToken).getKeyword();
      if (keyword == Keyword.BREAK) {
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
        return parseFunctionDeclarationStatement();
      } else if (keyword == Keyword.CONST) {
        if (peekMatches(TokenType.LT) || peekMatches(TokenType.OPEN_CURLY_BRACKET)
            || peekMatches(TokenType.OPEN_SQUARE_BRACKET)) {
          return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
        } else if (peekMatches(1, TokenType.IDENTIFIER)
            && (peekMatches(2, TokenType.OPEN_PAREN) || (peekMatches(2, TokenType.PERIOD)
                && peekMatches(3, TokenType.IDENTIFIER) && peekMatches(4, TokenType.OPEN_PAREN)))) {
          return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
        }
        return parseVariableDeclarationStatement();
      } else if (keyword == Keyword.NEW || keyword == Keyword.TRUE || keyword == Keyword.FALSE
          || keyword == Keyword.NULL) {
        return new ExpressionStatement(parseExpression(), expect(TokenType.SEMICOLON));
      } else {
        // Expected a statement
        // reportError(ParserErrorCode.?);
        return null;
      }
    } else if (matches(TokenType.SEMICOLON)) {
      return parseEmptyStatement();
    } else if (isInitializedVariableDeclaration()) {
      return parseVariableDeclarationStatement();
    } else if (isFunctionExpression()) {
      return new ExpressionStatement(parseFunctionExpression(), expect(TokenType.SEMICOLON));
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
   *     returnType? identifier formalParameterList
   * 
   * fieldFormalParameter ::=
   *     finalConstVarOrType? 'this' '.' identifier
   * </pre>
   * 
   * @return the normal formal parameter that was parsed
   */
  private NormalFormalParameter parseNormalFormalParameter() {
    FinalConstVarOrType holder = parseFinalConstVarOrType(true);
    Token thisKeyword = null;
    Token period = null;
    if (matches(Keyword.THIS)) {
      // Validate that field initializers are only used in constructors.
      // reportError(ParserErrorCode.?);
      thisKeyword = getAndAdvance();
      period = expect(TokenType.PERIOD);
    }
    SimpleIdentifier identifier = parseSimpleIdentifier();
    if (matches(TokenType.OPEN_PAREN)) {
      if (thisKeyword != null) {
        // Decide how to recover from this error.
        // reportError(ParserErrorCode.?);
      }
      FormalParameterList parameters = parseFormalParameterList();
      return new FunctionTypedFormalParameter(holder.getType(), identifier, parameters);
    }
    // Validate that the type is not void because this is not a function signature.
    // reportError(ParserErrorCode.?);
    if (thisKeyword != null) {
      return new FieldFormalParameter(
          holder.getKeyword(),
          holder.getType(),
          thisKeyword,
          period,
          identifier);
    }
    return new SimpleFormalParameter(holder.getKeyword(), holder.getType(), identifier);
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
   * @param comment the documentation comment to be associated with the declaration
   * @param externalKeyword the 'external' token
   * @param the return type that has already been parsed, or {@code null} if there was no return
   *          type
   * @return the operator declaration that was parsed
   */
  private MethodDeclaration parseOperator(Comment comment, Token externalKeyword,
      TypeName returnType) {
    Token operatorKeyword = expect(Keyword.OPERATOR);
    if (!currentToken.isUserDefinableOperator()) {
      reportError(ParserErrorCode.NON_USER_DEFINABLE_OPERATOR, currentToken.getLexeme());
    }
    SimpleIdentifier name = new SimpleIdentifier(getAndAdvance());
    FormalParameterList parameters = parseFormalParameterList();
    FunctionBody body = parseFunctionBody(true, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      // reportError(ParserErrorCode.?);
    }
    return new MethodDeclaration(
        comment,
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
   * Parse a part or part-of directive.
   * 
   * <pre>
   * partDirective ::=
   *     'part' stringLiteral ';'
   * 
   * partOfDirective ::=
   *     'part' 'of' qualified ';'
   * </pre>
   * 
   * @return the part or part-of directive that was parsed
   */
  private Directive parsePartDirective() {
    Token partKeyword = expect(Keyword.PART);
    if (matches(OF)) {
      Token ofKeyword = getAndAdvance();
      Identifier libraryName = parsePrefixedIdentifier();
      Token semicolon = expect(TokenType.SEMICOLON);
      return new PartOfDirective(partKeyword, ofKeyword, libraryName, semicolon);
    }
    StringLiteral partUri = parseStringLiteral();
    Token semicolon = expect(TokenType.SEMICOLON);
    return new PartDirective(partKeyword, partUri, semicolon);
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
      if (isFunctionExpression()) {
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
    } else {
      return createSyntheticSimpleIdentifier();
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
    Expression expression;
    if (matches(Keyword.SUPER) && currentToken.getNext().getType().isRelationalOperator()) {
      expression = new SuperExpression(getAndAdvance());
      Token operator = getAndAdvance();
      expression = new BinaryExpression(expression, operator, parseShiftExpression());
      return expression;
    }
    expression = parseShiftExpression();
    while (matches(Keyword.AS) || matches(Keyword.IS)
        || currentToken.getType().isRelationalOperator()) {
      if (currentToken.getType().isRelationalOperator()) {
        Token operator = getAndAdvance();
        expression = new BinaryExpression(expression, operator, parseShiftExpression());
      } else if (matches(Keyword.IS)) {
        Token isOperator = getAndAdvance();
        Token notOperator = null;
        if (matches(TokenType.BANG)) {
          notOperator = getAndAdvance();
        }
        expression = new IsExpression(expression, isOperator, notOperator, parseTypeName());
      } else {
        Token isOperator = getAndAdvance();
        expression = new IsExpression(expression, isOperator, null, parseTypeName());
      }
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
   *     'external'? 'static'? returnType? 'set' identifier '=' formalParameterList
   * </pre>
   * 
   * @param comment the documentation comment to be associated with the declaration
   * @param externalKeyword the 'external' token
   * @param staticKeyword the static keyword, or {@code null} if the setter is not static
   * @param the return type that has already been parsed, or {@code null} if there was no return
   *          type
   * @return the setter that was parsed
   */
  private MethodDeclaration parseSetter(Comment comment, Token externalKeyword,
      Token staticKeyword, TypeName returnType) {
    Token propertyKeyword = expect(Keyword.SET);
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
    FormalParameterList parameters = parseFormalParameterList();
    FunctionBody body = parseFunctionBody(true, false);
    if (externalKeyword != null && !(body instanceof EmptyFunctionBody)) {
      // reportError(ParserErrorCode.?);
    }
    return new MethodDeclaration(
        comment,
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
    reportError(ParserErrorCode.EXPECTED_IDENTIFIER);
    return createSyntheticSimpleIdentifier();
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
    reportError(ParserErrorCode.EXPECTED_IDENTIFIER);
    return createSyntheticSimpleIdentifier();
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
    while (matchesIdentifier() && peekMatches(TokenType.COLON)) {
      SimpleIdentifier label = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_LABEL);
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
  private ArrayList<Statement> parseStatements() {
    ArrayList<Statement> statements = new ArrayList<Statement>();
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
        Expression expression = parseSimpleIdentifier();
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
      return createSyntheticSimpleStringLiteral();
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
      ArrayList<SwitchMember> members = new ArrayList<SwitchMember>();
      while (!matches(TokenType.EOF) && !matches(TokenType.CLOSE_CURLY_BRACKET)) {
        List<Label> labels = new ArrayList<Label>();
        while (matchesIdentifier() && peekMatches(TokenType.COLON)) {
          SimpleIdentifier identifier = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_LABEL);
          String label = identifier.getToken().getLexeme();
          if (definedLabels.contains(label)) {
            reportError(ParserErrorCode.DUPLICATE_LABEL_IN_SWITCH_STATEMENT, identifier.getToken());
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
            getAndAdvance();
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
    ArrayList<CatchClause> catchClauses = new ArrayList<CatchClause>();
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
   *     'typedef' returnType? name typeParameterList? formalParameterList ';'
   * </pre>
   * 
   * @return the type alias that was parsed
   */
  private TypeAlias parseTypeAlias() {
    Comment comment = parseDocumentationComment();
    Token keyword = expect(Keyword.TYPEDEF);
    TypeName returnType = null;
    if (!peekMatches(TokenType.OPEN_PAREN) && !peekMatches(TokenType.LT)) {
      returnType = parseReturnType();
    }
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    TypeParameterList typeParameters = null;
    if (matches(TokenType.LT)) {
      typeParameters = parseTypeParameterList();
    }
    FormalParameterList parameters = parseFormalParameterList();
    Token semicolon = expect(TokenType.SEMICOLON);
    return new TypeAlias(comment, keyword, returnType, name, typeParameters, parameters, semicolon);
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
    ArrayList<TypeName> arguments = new ArrayList<TypeName>();
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
   *     name ('extends' bound)?
   * </pre>
   * 
   * @return the type parameter that was parsed
   */
  private TypeParameter parseTypeParameter() {
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME);
    if (matches(Keyword.EXTENDS)) {
      Token keyword = getAndAdvance();
      TypeName bound = parseTypeName();
      return new TypeParameter(name, keyword, bound);
    }
    return new TypeParameter(name, null, null);
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
          // reportError(ParserErrorCode.?);
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
    Comment localComment = parseDocumentationComment();
    SimpleIdentifier name = parseSimpleIdentifier(ParserErrorCode.BUILT_IN_IDENTIFIER_AS_VARIABLE_NAME);
    Token equals = null;
    Expression initializer = null;
    if (matches(TokenType.EQ)) {
      equals = getAndAdvance();
      initializer = parseExpression();
    }
    return new VariableDeclaration(localComment, name, equals, initializer);
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
    List<VariableDeclaration> variables = new ArrayList<VariableDeclaration>();
    variables.add(parseVariableDeclaration());
    while (matches(TokenType.COMMA)) {
      getAndAdvance();
      variables.add(parseVariableDeclaration());
    }
    return new VariableDeclarationList(holder.getKeyword(), holder.getType(), variables);
  }

  /**
   * Parse a variable declaration list.
   * 
   * <pre>
   * variableDeclarationList ::=
   *     finalConstVarOrType variableDeclaration (',' variableDeclaration)*
   * </pre>
   * 
   * @param keyword the token representing the 'var' keyword, or {@code null} if there is no keyword
   * @param type the type of the variables in the list
   * @return the variable declaration list that was parsed
   */
  private VariableDeclarationList parseVariableDeclarationList(Token keyword, TypeName type) {
    List<VariableDeclaration> variables = new ArrayList<VariableDeclaration>();
    variables.add(parseVariableDeclaration());
    while (matches(TokenType.COMMA)) {
      getAndAdvance();
      variables.add(parseVariableDeclaration());
    }
    return new VariableDeclarationList(keyword, type, variables);
  }

  /**
   * Parse a variable declaration list.
   * 
   * <pre>
   * variableDeclarationList ::=
   *     finalConstVarOrType variableDeclaration (',' variableDeclaration)*
   * </pre>
   * 
   * @param type the type of the variables
   * @return the variable declaration list that was parsed
   */
  private VariableDeclarationList parseVariableDeclarationList(TypeName type) {
    List<VariableDeclaration> variables = new ArrayList<VariableDeclaration>();
    variables.add(parseVariableDeclaration());
    while (matches(TokenType.COMMA)) {
      getAndAdvance();
      variables.add(parseVariableDeclaration());
    }
    return new VariableDeclarationList(null, type, variables);
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
   * Return {@code true} if the token the given distance after the current token has the given type.
   * 
   * @param distance the number of tokens to look ahead, where {@code 0} is the current token,
   *          {@code 1} is the next token (equivalent to {@link #peekMatches(TokenType)}), etc.
   * @param type the type of token that can optionally appear at the specified location
   * @return {@code true} if the token at the specified location has the given type
   */
  private boolean peekMatches(int distance, TokenType type) {
    Token token = currentToken;
    for (int i = 0; i < distance; i++) {
      token = token.getNext();
    }
    return token.getType() == type;
  }

  /**
   * Return {@code true} if the token following the current token matches the given keyword.
   * 
   * @param keyword the keyword that can optionally appear after the current location
   * @return {@code true} if the token following the current token matches the given keyword
   */
  private boolean peekMatches(Keyword keyword) {
    return currentToken.getNext().getType() == TokenType.KEYWORD
        && ((KeywordToken) currentToken.getNext()).getKeyword() == keyword;
  }

  /**
   * Return {@code true} if the token following the current token has the given type.
   * 
   * @param type the type of token that can optionally appear after the current location
   * @return {@code true} if the token following the current token has the given type
   */
  private boolean peekMatches(TokenType type) {
    return currentToken.getNext().getType() == type;
  }

  /**
   * Return {@code true} if the token after the current token is a valid identifier. Valid
   * identifiers include built-in identifiers (pseudo-keywords).
   * 
   * @return {@code true} if the token after the current token is a valid identifier
   */
  private boolean peekMatchesIdentifier() {
    return peekMatches(TokenType.IDENTIFIER)
        || (peekMatches(TokenType.KEYWORD) && ((KeywordToken) currentToken.getNext()).getKeyword().isPseudoKeyword());
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
    } else if (token.getType() != TokenType.PERIOD) {
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
    if (startToken.getType() == TokenType.KEYWORD
        && ((KeywordToken) startToken).getKeyword() == Keyword.VOID) {
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
    if (startToken.getType() == TokenType.IDENTIFIER || (startToken.getType() == TokenType.KEYWORD)
        && ((KeywordToken) startToken).getKeyword().isPseudoKeyword()) {
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
    while (token != null && token.getType() == TokenType.STRING) {
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
    if (token.getType() != TokenType.LT) {
      return null;
    }
    token = skipTypeName(token.getNext());
    if (token == null) {
      return null;
    }
    while (token.getType() == TokenType.COMMA) {
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
    if (token.getType() == TokenType.LT) {
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
      // reportError(ParserErrorCode.?);
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
        // reportError(ParserErrorCode.?);
        return length;
      }
      char firstDigit = lexeme.charAt(currentIndex + 1);
      char secondDigit = lexeme.charAt(currentIndex + 2);
      if (!isHexDigit(firstDigit) || !isHexDigit(secondDigit)) {
        // Illegal escape sequence: invalid hex digit
        // reportError(ParserErrorCode.?);
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
        // reportError(ParserErrorCode.?);
        return length;
      }
      currentChar = lexeme.charAt(currentIndex);
      if (currentChar == '{') {
        currentIndex++;
        if (currentIndex >= length) {
          // Illegal escape sequence: incomplete escape
          // reportError(ParserErrorCode.?);
          return length;
        }
        currentChar = lexeme.charAt(currentIndex);
        int digitCount = 0;
        int value = 0;
        while (currentChar != '}') {
          if (!isHexDigit(currentChar)) {
            // Illegal escape sequence: invalid hex digit
            // reportError(ParserErrorCode.?);
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
            // reportError(ParserErrorCode.?);
            return length;
          }
          currentChar = lexeme.charAt(currentIndex);
        }
        if (digitCount < 1 || digitCount > 6) {
          // Illegal escape sequence: not enough or too many hex digits
          // reportError(ParserErrorCode.?);
        }
        appendScalarValue(builder, value, index, currentIndex);
        return currentIndex + 1;
      } else {
        if (currentIndex + 3 >= length) {
          // Illegal escape sequence: not enough hex digits
          // reportError(ParserErrorCode.?);
          return length;
        }
        char firstDigit = currentChar;
        char secondDigit = lexeme.charAt(currentIndex + 1);
        char thirdDigit = lexeme.charAt(currentIndex + 2);
        char fourthDigit = lexeme.charAt(currentIndex + 3);
        if (!isHexDigit(firstDigit) || !isHexDigit(secondDigit) || !isHexDigit(thirdDigit)
            || !isHexDigit(fourthDigit)) {
          // Illegal escape sequence: invalid hex digits
          // reportError(ParserErrorCode.?);
        } else {
          appendScalarValue(
              builder,
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
}
