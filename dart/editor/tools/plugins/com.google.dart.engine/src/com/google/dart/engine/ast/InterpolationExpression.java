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
 * Instances of the class {@code InterpolationExpression} represent an expression embedded in a
 * string interpolation.
 * 
 * <pre>
 * interpolationExpression ::=
 *     '$' {@link SimpleIdentifier identifier}
 *   | '$' '{' {@link Expression expression} '}'
 * </pre>
 */
public class InterpolationExpression extends InterpolationElement {
  /**
   * The token used to introduce the interpolation expression; either '$' if the expression is a
   * simple identifier or '${' if the expression is a full expression.
   */
  private Token leftBracket;

  /**
   * The expression to be evaluated for the value to be converted into a string.
   */
  private Expression expression;

  /**
   * The right curly bracket, or {@code null} if the expression is an identifier without brackets.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created interpolation expression.
   */
  public InterpolationExpression() {
  }

  /**
   * Initialize a newly created interpolation expression.
   * 
   * @param leftBracket the left curly bracket
   * @param expression the expression to be evaluated for the value to be converted into a string
   * @param rightBracket the right curly bracket
   */
  public InterpolationExpression(Token leftBracket, Expression expression, Token rightBracket) {
    this.leftBracket = leftBracket;
    this.expression = becomeParentOf(expression);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitInterpolationExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return leftBracket;
  }

  @Override
  public Token getEndToken() {
    if (rightBracket != null) {
      return rightBracket;
    }
    return expression.getEndToken();
  }

  /**
   * Return the expression to be evaluated for the value to be converted into a string.
   * 
   * @return the expression to be evaluated for the value to be converted into a string
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the left curly bracket.
   * 
   * @return the left curly bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the right curly bracket.
   * 
   * @return the right curly bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Set the expression to be evaluated for the value to be converted into a string to the given
   * expression.
   * 
   * @param expression the expression to be evaluated for the value to be converted into a string
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the left curly bracket to the given token.
   * 
   * @param leftBracket the left curly bracket
   */
  public void setLeftBracket(Token leftBracket) {
    this.leftBracket = leftBracket;
  }

  /**
   * Set the right curly bracket to the given token.
   * 
   * @param rightBracket the right curly bracket
   */
  public void setRightBracket(Token rightBracket) {
    this.rightBracket = rightBracket;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(expression, visitor);
  }
}
