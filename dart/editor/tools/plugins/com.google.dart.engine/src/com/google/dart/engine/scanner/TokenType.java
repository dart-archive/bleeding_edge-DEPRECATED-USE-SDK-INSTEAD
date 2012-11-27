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
package com.google.dart.engine.scanner;

import static com.google.dart.engine.scanner.TokenClass.ADDITIVE_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.ASSIGNMENT_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.BITWISE_AND_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.BITWISE_OR_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.BITWISE_XOR_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.CASCADE_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.CONDITIONAL_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.EQUALITY_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.LOGICAL_AND_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.LOGICAL_OR_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.MULTIPLICATIVE_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.NO_CLASS;
import static com.google.dart.engine.scanner.TokenClass.RELATIONAL_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.SHIFT_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.UNARY_POSTFIX_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.UNARY_PREFIX_OPERATOR;

/**
 * The enumeration {@code TokenType} defines the types of tokens that can be returned by the
 * scanner.
 */
public enum TokenType {
  /**
   * The type of the token that marks the end of the input.
   */
  EOF(null, "") {
    @Override
    public String toString() {
      return "-eof-";
    }
  },

  DOUBLE,
  HEXADECIMAL,
  IDENTIFIER,
  INT,
  KEYWORD,
  MULTI_LINE_COMMENT,
  SCRIPT_TAG,
  SINGLE_LINE_COMMENT,
  STRING,

  AMPERSAND(BITWISE_AND_OPERATOR, "&"),
  AMPERSAND_AMPERSAND(LOGICAL_AND_OPERATOR, "&&"),
  AMPERSAND_EQ(ASSIGNMENT_OPERATOR, "&="),
  AT(null, "@"),
  BANG(UNARY_PREFIX_OPERATOR, "!"),
  BANG_EQ(EQUALITY_OPERATOR, "!="),
  BANG_EQ_EQ(EQUALITY_OPERATOR, "!=="),
  BAR(BITWISE_OR_OPERATOR, "|"),
  BAR_BAR(LOGICAL_OR_OPERATOR, "||"),
  BAR_EQ(ASSIGNMENT_OPERATOR, "|="),
  COLON(null, ":"),
  COMMA(null, ","),
  CARET(BITWISE_XOR_OPERATOR, "^"),
  CARET_EQ(ASSIGNMENT_OPERATOR, "^="),
  CLOSE_CURLY_BRACKET(null, "}"),
  CLOSE_PAREN(null, ")"),
  CLOSE_SQUARE_BRACKET(null, "]"),
  EQ(ASSIGNMENT_OPERATOR, "="),
  EQ_EQ(EQUALITY_OPERATOR, "=="),
  EQ_EQ_EQ(EQUALITY_OPERATOR, "==="),
  FUNCTION(null, "=>"),
  GT(RELATIONAL_OPERATOR, ">"),
  GT_EQ(RELATIONAL_OPERATOR, ">="),
  GT_GT(SHIFT_OPERATOR, ">>"),
  GT_GT_EQ(ASSIGNMENT_OPERATOR, ">>="),
  GT_GT_GT(SHIFT_OPERATOR, ">>>"),
  GT_GT_GT_EQ(ASSIGNMENT_OPERATOR, ">>>="),
  HASH(null, "#"),
  INDEX(UNARY_POSTFIX_OPERATOR, "[]"),
  INDEX_EQ(UNARY_POSTFIX_OPERATOR, "[]="),
  IS(RELATIONAL_OPERATOR, "is"),
  LT(RELATIONAL_OPERATOR, "<"),
  LT_EQ(RELATIONAL_OPERATOR, "<="),
  LT_LT(SHIFT_OPERATOR, "<<"),
  LT_LT_EQ(ASSIGNMENT_OPERATOR, "<<="),
  MINUS(ADDITIVE_OPERATOR, "-"),
  MINUS_EQ(ASSIGNMENT_OPERATOR, "-="),
  MINUS_MINUS(UNARY_PREFIX_OPERATOR, "--"),
  OPEN_CURLY_BRACKET(null, "{"),
  OPEN_PAREN(UNARY_POSTFIX_OPERATOR, "("),
  OPEN_SQUARE_BRACKET(UNARY_POSTFIX_OPERATOR, "["),
  PERCENT(MULTIPLICATIVE_OPERATOR, "%"),
  PERCENT_EQ(ASSIGNMENT_OPERATOR, "%="),
  PERIOD(UNARY_POSTFIX_OPERATOR, "."),
  PERIOD_PERIOD(CASCADE_OPERATOR, ".."),
  PLUS(ADDITIVE_OPERATOR, "+"),
  PLUS_EQ(ASSIGNMENT_OPERATOR, "+="),
  PLUS_PLUS(UNARY_PREFIX_OPERATOR, "++"),
  QUESTION(CONDITIONAL_OPERATOR, "?"),
  SEMICOLON(null, ";"),
  SLASH(MULTIPLICATIVE_OPERATOR, "/"),
  SLASH_EQ(ASSIGNMENT_OPERATOR, "/="),
  STAR(MULTIPLICATIVE_OPERATOR, "*"),
  STAR_EQ(ASSIGNMENT_OPERATOR, "*="),
  STRING_INTERPOLATION_EXPRESSION(null, "${"),
  STRING_INTERPOLATION_IDENTIFIER(null, "$"),
  TILDE(UNARY_PREFIX_OPERATOR, "~"),
  TILDE_SLASH(MULTIPLICATIVE_OPERATOR, "~/"),
  TILDE_SLASH_EQ(ASSIGNMENT_OPERATOR, "~/="),

  // The following are not currently tokens in Dart, but are reserved for future use.

  BACKPING(null, "`"),
  BACKSLASH(null, "\\"),
  PERIOD_PERIOD_PERIOD(null, "...");

  /**
   * The class of the token.
   */
  private TokenClass tokenClass;

  /**
   * The lexeme that defines this type of token, or {@code null} if there is more than one possible
   * lexeme for this type of token.
   */
  private String lexeme;

  private TokenType() {
    this(NO_CLASS, null);
  }

  private TokenType(TokenClass tokenClass, String lexeme) {
    this.tokenClass = tokenClass == null ? NO_CLASS : tokenClass;
    this.lexeme = lexeme;
  }

  /**
   * Return the lexeme that defines this type of token, or {@code null} if there is more than one
   * possible lexeme for this type of token.
   * 
   * @return the lexeme that defines this type of token
   */
  public String getLexeme() {
    return lexeme;
  }

  /**
   * Return the precedence of the token, or {@code 0} if the token does not represent an operator.
   * 
   * @return the precedence of the token
   */
  public int getPrecedence() {
    return tokenClass.getPrecedence();
  }

  /**
   * Return {@code true} if this type of token represents an additive operator.
   * 
   * @return {@code true} if this type of token represents an additive operator
   */
  public boolean isAdditiveOperator() {
    return tokenClass == ADDITIVE_OPERATOR;
  }

  /**
   * Return {@code true} if this type of token represents an assignment operator.
   * 
   * @return {@code true} if this type of token represents an assignment operator
   */
  public boolean isAssignmentOperator() {
    return tokenClass == ASSIGNMENT_OPERATOR;
  }

  /**
   * Return {@code true} if this type of token represents an equality operator.
   * 
   * @return {@code true} if this type of token represents an equality operator
   */
  public boolean isEqualityOperator() {
    return tokenClass == EQUALITY_OPERATOR;
  }

  /**
   * Return {@code true} if this type of token represents an increment operator.
   * 
   * @return {@code true} if this type of token represents an increment operator
   */
  public boolean isIncrementOperator() {
    return lexeme == "++" || lexeme == "--";
  }

  /**
   * Return {@code true} if this type of token represents a multiplicative operator.
   * 
   * @return {@code true} if this type of token represents a multiplicative operator
   */
  public boolean isMultiplicativeOperator() {
    return tokenClass == MULTIPLICATIVE_OPERATOR;
  }

  /**
   * Return {@code true} if this token type represents an operator.
   * 
   * @return {@code true} if this token type represents an operator
   */
  public boolean isOperator() {
    return tokenClass != NO_CLASS && this != OPEN_PAREN && this != OPEN_SQUARE_BRACKET
        && this != PERIOD;
  }

  /**
   * Return {@code true} if this type of token represents a relational operator.
   * 
   * @return {@code true} if this type of token represents a relational operator
   */
  public boolean isRelationalOperator() {
    return tokenClass == RELATIONAL_OPERATOR;
  }

  /**
   * Return {@code true} if this type of token represents a shift operator.
   * 
   * @return {@code true} if this type of token represents a shift operator
   */
  public boolean isShiftOperator() {
    return tokenClass == SHIFT_OPERATOR;
  }

  /**
   * Return {@code true} if this type of token represents a unary postfix operator.
   * 
   * @return {@code true} if this type of token represents a unary postfix operator
   */
  public boolean isUnaryPostfixOperator() {
    return tokenClass == UNARY_POSTFIX_OPERATOR;
  }

  /**
   * Return {@code true} if this type of token represents a unary prefix operator.
   * 
   * @return {@code true} if this type of token represents a unary prefix operator
   */
  public boolean isUnaryPrefixOperator() {
    return tokenClass == UNARY_PREFIX_OPERATOR;
  }

  /**
   * Return {@code true} if this token type represents an operator that can be defined by users.
   * 
   * @return {@code true} if this token type represents an operator that can be defined by users
   */
  public boolean isUserDefinableOperator() {
    return lexeme == "==" || lexeme == "~" || lexeme == "[]" || lexeme == "[]=" || lexeme == "*"
        || lexeme == "/" || lexeme == "%" || lexeme == "~/" || lexeme == "+" || lexeme == "-"
        || lexeme == "<<" || lexeme == ">>" || lexeme == ">=" || lexeme == ">" || lexeme == "<="
        || lexeme == "<" || lexeme == "&" || lexeme == "^" || lexeme == "|";
  }
}
