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
 * Instances of the class {@code ParenthesizedExpression} represent a parenthesized expression.
 * 
 * <pre>
 * parenthesizedExpression ::=
 *     '(' {@link Expression expression} ')'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ParenthesizedExpression extends Expression {
  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The expression within the parentheses.
   */
  private Expression expression;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * Initialize a newly created parenthesized expression.
   * 
   * @param leftParenthesis the left parenthesis
   * @param expression the expression within the parentheses
   * @param rightParenthesis the right parenthesis
   */
  public ParenthesizedExpression(Token leftParenthesis, Expression expression,
      Token rightParenthesis) {
    this.leftParenthesis = leftParenthesis;
    this.expression = becomeParentOf(expression);
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitParenthesizedExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return leftParenthesis;
  }

  @Override
  public Token getEndToken() {
    return rightParenthesis;
  }

  /**
   * Return the expression within the parentheses.
   * 
   * @return the expression within the parentheses
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the left parenthesis.
   * 
   * @return the left parenthesis
   */
  public Token getLeftParenthesis() {
    return leftParenthesis;
  }

  @Override
  public int getPrecedence() {
    return 15;
  }

  /**
   * Return the right parenthesis.
   * 
   * @return the right parenthesis
   */
  public Token getRightParenthesis() {
    return rightParenthesis;
  }

  /**
   * Set the expression within the parentheses to the given expression.
   * 
   * @param expression the expression within the parentheses
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the left parenthesis to the given token.
   * 
   * @param parenthesis the left parenthesis
   */
  public void setLeftParenthesis(Token parenthesis) {
    leftParenthesis = parenthesis;
  }

  /**
   * Set the right parenthesis to the given token.
   * 
   * @param parenthesis the right parenthesis
   */
  public void setRightParenthesis(Token parenthesis) {
    rightParenthesis = parenthesis;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(expression, visitor);
  }
}
