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
import static com.google.dart.engine.scanner.TokenClass.EQUALITY_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.INCREMENT_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.MULTIPLICATIVE_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.NO_CLASS;
import static com.google.dart.engine.scanner.TokenClass.RELATIONAL_OPERATOR;
import static com.google.dart.engine.scanner.TokenClass.SHIFT_OPERATOR;

/**
 * The enumeration <code>TokenType</code> defines the types of tokens that can be returned by the
 * scanner.
 */
public enum TokenType {
  EOF, // marks the end of the input

  KEYWORD,
  IDENTIFIER,
  DOUBLE,
  INT,
  HEXADECIMAL,
  STRING,

  AMPERSAND(null, "&"),
  BACKPING(null, "`"),
  BACKSLASH(null, "\\"),
  BANG(null, "!"),
  BAR(null, "|"),
  COLON(null, ":"),
  COMMA(null, ","),
  EQ(ASSIGNMENT_OPERATOR, "="),
  GT(RELATIONAL_OPERATOR, ">"),
  HASH(null, "#"),
  OPEN_CURLY_BRACKET(null, "{"),
  OPEN_SQUARE_BRACKET(null, "["),
  OPEN_PAREN(null, "("),
  LT(RELATIONAL_OPERATOR, "<"),
  MINUS(ADDITIVE_OPERATOR, "-"),
  PERIOD(null, "."),
  PLUS(ADDITIVE_OPERATOR, "+"),
  QUESTION(null, "?"),
  CLOSE_CURLY_BRACKET(null, "}"),
  CLOSE_SQUARE_BRACKET(null, "]"),
  CLOSE_PAREN(null, ")"),
  SEMICOLON(null, ";"),
  SLASH(MULTIPLICATIVE_OPERATOR, "/"),
  TILDE(null, "~"),
  STAR(MULTIPLICATIVE_OPERATOR, "*"),
  PERCENT(MULTIPLICATIVE_OPERATOR, "%"),
  CARET(null, "^"),

  STRING_INTERPOLATION, // either '$' or '${'
  LT_EQ(RELATIONAL_OPERATOR, "<="),
  FUNCTION(null, "=>"),
  SLASH_EQ(ASSIGNMENT_OPERATOR, "/="),
  PERIOD_PERIOD_PERIOD(null, "..."),
  PERIOD_PERIOD(null, ".."),
  EQ_EQ_EQ(EQUALITY_OPERATOR, "==="),
  EQ_EQ(EQUALITY_OPERATOR, "=="),
  LT_LT_EQ(ASSIGNMENT_OPERATOR, "<<="),
  LT_LT(SHIFT_OPERATOR, "<<"),
  GT_EQ(RELATIONAL_OPERATOR, ">="),
  GT_GT_EQ(ASSIGNMENT_OPERATOR, ">>="),
  GT_GT_GT_EQ(ASSIGNMENT_OPERATOR, ">>>="),
  INDEX_EQ(null, "[]="),
  INDEX(null, "[]"),
  BANG_EQ_EQ(EQUALITY_OPERATOR, "!=="),
  BANG_EQ(EQUALITY_OPERATOR, "!="),
  AMPERSAND_AMPERSAND(null, "&&"),
  AMPERSAND_EQ(ASSIGNMENT_OPERATOR, "&="),
  BAR_BAR(null, "||"),
  BAR_EQ(ASSIGNMENT_OPERATOR, "|="),
  STAR_EQ(ASSIGNMENT_OPERATOR, "*="),
  PLUS_PLUS(INCREMENT_OPERATOR, "++"),
  PLUS_EQ(ASSIGNMENT_OPERATOR, "+="),
  MINUS_MINUS(INCREMENT_OPERATOR, "--"),
  MINUS_EQ(ASSIGNMENT_OPERATOR, "-="),
  TILDE_SLASH_EQ(ASSIGNMENT_OPERATOR, "~/="),
  TILDE_SLASH(MULTIPLICATIVE_OPERATOR, "~/"),
  PERCENT_EQ(ASSIGNMENT_OPERATOR, "%="),
  GT_GT(SHIFT_OPERATOR, ">>"),
  GT_GT_GT(SHIFT_OPERATOR, ">>>"),
  CARET_EQ(ASSIGNMENT_OPERATOR, "^="),
  IS(null, "is");

  private TokenClass tokenClass;

  /**
   * The lexeme that defines this type of token, or <code>null</code> if there is more than one
   * possible lexeme for this type of token.
   */
  private String lexeme;

  private TokenType() {
    this(NO_CLASS, null);
  }

  private TokenType(TokenClass tokenClass, String lexeme) {
    this.tokenClass = tokenClass;
    this.lexeme = lexeme;
  }

  /**
   * Return the lexeme that defines this type of token, or <code>null</code> if there is more than
   * one possible lexeme for this type of token.
   * 
   * @return the lexeme that defines this type of token
   */
  public String getLexeme() {
    return lexeme;
  }

  /**
   * Return <code>true</code> if this type of token represents an additive operator.
   * 
   * @return <code>true</code> if this type of token represents an additive operator
   */
  public boolean isAdditiveOperator() {
    return tokenClass == ADDITIVE_OPERATOR;
  }

  /**
   * Return <code>true</code> if this type of token represents an assignment operator.
   * 
   * @return <code>true</code> if this type of token represents an assignment operator
   */
  public boolean isAssignmentOperator() {
    return tokenClass == ASSIGNMENT_OPERATOR;
  }

  /**
   * Return <code>true</code> if this type of token represents an equality operator.
   * 
   * @return <code>true</code> if this type of token represents an equality operator
   */
  public boolean isEqualityOperator() {
    return tokenClass == EQUALITY_OPERATOR;
  }

  /**
   * Return <code>true</code> if this type of token represents an increment operator.
   * 
   * @return <code>true</code> if this type of token represents an increment operator
   */
  public boolean isIncrementOperator() {
    return tokenClass == INCREMENT_OPERATOR;
  }

  /**
   * Return <code>true</code> if this type of token represents a multiplicative operator.
   * 
   * @return <code>true</code> if this type of token represents a multiplicative operator
   */
  public boolean isMultiplicativeOperator() {
    return tokenClass == MULTIPLICATIVE_OPERATOR;
  }

  /**
   * Return <code>true</code> if this type of token represents a relational operator.
   * 
   * @return <code>true</code> if this type of token represents a relational operator
   */
  public boolean isRelationalOperator() {
    return tokenClass == RELATIONAL_OPERATOR;
  }

  /**
   * Return <code>true</code> if this type of token represents a shift operator.
   * 
   * @return <code>true</code> if this type of token represents a shift operator
   */
  public boolean isShiftOperator() {
    return tokenClass == SHIFT_OPERATOR;
  }
}
