/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class <code>SimpleStringLiteral</code> represent a string literal expression
 * that does not contain any interpolations.
 * 
 * <pre>
 * simpleStringLiteral ::=
 *     rawStringLiteral
 *   | basicStringLiteral
 *
 * rawStringLiteral ::=
 *     '@' basicStringLiteral
 *
 * simpleStringLiteral ::=
 *     multiLineStringLiteral
 *   | singleLineStringLiteral
 *
 * multiLineStringLiteral ::=
 *     "'''" characters "'''"
 *   | '"""' characters '"""'
 *
 * singleLineStringLiteral ::=
 *     "'" characters "'"
 *     '"' characters '"'
 * </pre>
 */
public class SimpleStringLiteral extends StringLiteral {
  /**
   * The token representing the literal.
   */
  private Token literal;

  /**
   * The value of the literal.
   */
  private String value;

  /**
   * Initialize a newly created simple string literal.
   */
  public SimpleStringLiteral() {
  }

  /**
   * Initialize a newly created simple string literal.
   * 
   * @param literal the token representing the literal
   * @param value the value of the literal
   */
  public SimpleStringLiteral(Token literal, String value) {
    this.literal = literal;
    this.value = value;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSingleStringLiteral(this);
  }

  @Override
  public Token getBeginToken() {
    return literal;
  }

  @Override
  public Token getEndToken() {
    return literal;
  }

  /**
   * Return the token representing the literal.
   * 
   * @return the token representing the literal
   */
  public Token getLiteral() {
    return literal;
  }

  /**
   * Return the value of the literal.
   * 
   * @return the value of the literal
   */
  public String getValue() {
    return value;
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  /**
   * Return <code>true</code> if this string literal is a multi-line string.
   * 
   * @return <code>true</code> if this string literal is a multi-line string
   */
  public boolean isMultiline() {
    if (value.length() < 6) {
      return false;
    }
    return value.endsWith("\"\"\"") || value.endsWith("'''");
  }

  /**
   * Return <code>true</code> if this string literal is a raw string.
   * 
   * @return <code>true</code> if this string literal is a raw string
   */
  public boolean isRaw() {
    return value.charAt(0) == '@';
  }

  /**
   * Set the token representing the literal to the given token.
   * 
   * @param literal the token representing the literal
   */
  public void setLiteral(Token literal) {
    this.literal = literal;
  }

  /**
   * Set the value of the literal to the given string.
   * 
   * @param string the value of the literal
   */
  public void setValue(String string) {
    value = string;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    // There are no children to visit.
  }
}
