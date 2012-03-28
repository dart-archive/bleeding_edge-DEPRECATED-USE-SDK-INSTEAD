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
 * Instances of the class <code>DoubleLiteral</code> represent a floating point literal expression.
 * 
 * <pre>
 * doubleLiteral ::=
 *     decimalDigit+ ('.' decimalDigit*)? exponent?
 *   | '.' decimalDigit+ exponent?
 *
 * exponent ::=
 *     ('e' | 'E') ('+' | '-')? decimalDigit+
 * </pre>
 */
public class DoubleLiteral extends Literal {
  /**
   * The token representing the literal.
   */
  private Token literal;

  /**
   * The value of the literal.
   */
  private double value;

  /**
   * Initialize a newly created floating point literal.
   */
  public DoubleLiteral() {
  }

  /**
   * Initialize a newly created floating point literal.
   * 
   * @param literal the token representing the literal
   * @param value the value of the literal
   */
  public DoubleLiteral(Token literal, double value) {
    this.literal = literal;
    this.value = value;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitDoubleLiteral(this);
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
  public double getValue() {
    return value;
  }

  @Override
  public boolean isConstant() {
    return true;
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
   * Set the value of the literal to the given value.
   * 
   * @param value the value of the literal
   */
  public void setValue(double value) {
    this.value = value;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    // There are no children to visit.
  }
}
