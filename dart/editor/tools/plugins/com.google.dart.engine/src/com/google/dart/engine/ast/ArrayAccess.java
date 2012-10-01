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
 * Instances of the class {@code ArrayAccess} represent an array access expression.
 * 
 * <pre>
 * arrayAccess ::=
 *     {@link Expression array} '[' {@link Expression index} ']'
 * </pre>
 */
public class ArrayAccess extends Expression {
  /**
   * The expression used to compute the array being indexed, or {@code null} if this array access is
   * part of a cascade expression.
   */
  private Expression array;

  /**
   * The period ("..") before a cascaded array access, or {@code null} if this array access is not
   * part of a cascade expression.
   */
  private Token period;

  /**
   * The left square bracket.
   */
  private Token leftBracket;

  /**
   * The expression used to compute the index.
   */
  private Expression index;

  /**
   * The right square bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created array access expression.
   */
  public ArrayAccess() {
  }

  /**
   * Initialize a newly created array access expression.
   * 
   * @param array the expression used to compute the array being indexed
   * @param leftBracket the left square bracket
   * @param index the expression used to compute the index
   * @param rightBracket the right square bracket
   */
  public ArrayAccess(Expression array, Token leftBracket, Expression index, Token rightBracket) {
    this.array = becomeParentOf(array);
    this.leftBracket = leftBracket;
    this.index = becomeParentOf(index);
    this.rightBracket = rightBracket;
  }

  /**
   * Initialize a newly created array access expression.
   * 
   * @param period the period ("..") before a cascaded array access
   * @param leftBracket the left square bracket
   * @param index the expression used to compute the index
   * @param rightBracket the right square bracket
   */
  public ArrayAccess(Token period, Token leftBracket, Expression index, Token rightBracket) {
    this.period = period;
    this.leftBracket = leftBracket;
    this.index = becomeParentOf(index);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitArrayAccess(this);
  }

  /**
   * Return the expression used to compute the array being indexed, or {@code null} if this array
   * access is part of a cascade expression.
   * 
   * @return the expression used to compute the array being indexed
   */
  public Expression getArray() {
    return array;
  }

  @Override
  public Token getBeginToken() {
    if (array != null) {
      return array.getBeginToken();
    }
    return period;
  }

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the expression used to compute the index.
   * 
   * @return the expression used to compute the index
   */
  public Expression getIndex() {
    return index;
  }

  /**
   * Return the left square bracket.
   * 
   * @return the left square bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the period ("..") before a cascaded array access, or {@code null} if this array access
   * is not part of a cascade expression.
   * 
   * @return the period ("..") before a cascaded array access
   */
  public Token getPeriod() {
    return period;
  }

  /**
   * Return the right square bracket.
   * 
   * @return the right square bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Return {@code true} if this expression is cascaded. If it is, then the target of this
   * expression is not stored locally but is stored in the nearest ancestor that is a
   * {@link CascadeExpression}.
   * 
   * @return {@code true} if this expression is cascaded
   */
  public boolean isCascaded() {
    return period != null;
  }

  /**
   * Set the expression used to compute the array being indexed to the given expression.
   * 
   * @param expression the expression used to compute the array being indexed
   */
  public void setArray(Expression expression) {
    array = becomeParentOf(expression);
  }

  /**
   * Set the expression used to compute the index to the given expression.
   * 
   * @param expression the expression used to compute the index
   */
  public void setIndex(Expression expression) {
    index = becomeParentOf(expression);
  }

  /**
   * Set the left square bracket to the given token.
   * 
   * @param bracket the left square bracket
   */
  public void setLeftBracket(Token bracket) {
    leftBracket = bracket;
  }

  /**
   * Set the period ("..") before a cascaded array access to the given token.
   * 
   * @param period the period ("..") before a cascaded array access
   */
  public void setPeriod(Token period) {
    this.period = period;
  }

  /**
   * Set the right square bracket to the given token.
   * 
   * @param bracket the right square bracket
   */
  public void setRightBracket(Token bracket) {
    rightBracket = bracket;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(array, visitor);
    safelyVisitChild(index, visitor);
  }
}
