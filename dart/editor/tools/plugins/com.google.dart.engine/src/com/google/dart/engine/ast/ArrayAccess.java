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
 * Instances of the class <code>ArrayAccess</code> represent an array access expression.
 * 
 * <pre>
 * arrayAccess ::=
 *     {@link Expression array} '[' {@link Expression index} ']'
 * </pre>
 */
public class ArrayAccess extends Expression {
  /**
   * The expression used to compute the array being indexed.
   */
  private Expression array;

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

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitArrayAccess(this);
  }

  /**
   * Return the expression used to compute the array being indexed.
   * 
   * @return the expression used to compute the array being indexed
   */
  public Expression getArray() {
    return array;
  }

  @Override
  public Token getBeginToken() {
    return array.getBeginToken();
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
   * Return the right square bracket.
   * 
   * @return the right square bracket
   */
  public Token getRightBracket() {
    return rightBracket;
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
