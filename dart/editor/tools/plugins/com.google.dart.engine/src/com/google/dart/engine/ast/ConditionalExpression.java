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
 * Instances of the class {@code ConditionalExpression} represent a conditional expression.
 * 
 * <pre>
 * conditionalExpression ::=
 *     {@link Expression condition} '?' {@link Expression thenExpression} ':' {@link Expression elseExpression}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ConditionalExpression extends Expression {
  /**
   * The condition used to determine which of the expressions is executed next.
   */
  private Expression condition;

  /**
   * The token used to separate the condition from the then expression.
   */
  private Token question;

  /**
   * The expression that is executed if the condition evaluates to {@code true}.
   */
  private Expression thenExpression;

  /**
   * The token used to separate the then expression from the else expression.
   */
  private Token colon;

  /**
   * The expression that is executed if the condition evaluates to {@code false}.
   */
  private Expression elseExpression;

  /**
   * Initialize a newly created conditional expression.
   * 
   * @param condition the condition used to determine which expression is executed next
   * @param question the token used to separate the condition from the then expression
   * @param thenExpression the expression that is executed if the condition evaluates to
   *          {@code true}
   * @param colon the token used to separate the then expression from the else expression
   * @param elseExpression the expression that is executed if the condition evaluates to
   *          {@code false}
   */
  public ConditionalExpression(Expression condition, Token question, Expression thenExpression,
      Token colon, Expression elseExpression) {
    this.condition = becomeParentOf(condition);
    this.question = question;
    this.thenExpression = becomeParentOf(thenExpression);
    this.colon = colon;
    this.elseExpression = becomeParentOf(elseExpression);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitConditionalExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return condition.getBeginToken();
  }

  /**
   * Return the token used to separate the then expression from the else expression.
   * 
   * @return the token used to separate the then expression from the else expression
   */
  public Token getColon() {
    return colon;
  }

  /**
   * Return the condition used to determine which of the expressions is executed next.
   * 
   * @return the condition used to determine which expression is executed next
   */
  public Expression getCondition() {
    return condition;
  }

  /**
   * Return the expression that is executed if the condition evaluates to {@code false}.
   * 
   * @return the expression that is executed if the condition evaluates to {@code false}
   */
  public Expression getElseExpression() {
    return elseExpression;
  }

  @Override
  public Token getEndToken() {
    return elseExpression.getEndToken();
  }

  @Override
  public int getPrecedence() {
    return 3;
  }

  /**
   * Return the token used to separate the condition from the then expression.
   * 
   * @return the token used to separate the condition from the then expression
   */
  public Token getQuestion() {
    return question;
  }

  /**
   * Return the expression that is executed if the condition evaluates to {@code true}.
   * 
   * @return the expression that is executed if the condition evaluates to {@code true}
   */
  public Expression getThenExpression() {
    return thenExpression;
  }

  /**
   * Set the token used to separate the then expression from the else expression to the given token.
   * 
   * @param colon the token used to separate the then expression from the else expression
   */
  public void setColon(Token colon) {
    this.colon = colon;
  }

  /**
   * Set the condition used to determine which of the expressions is executed next to the given
   * expression.
   * 
   * @param expression the condition used to determine which expression is executed next
   */
  public void setCondition(Expression expression) {
    condition = becomeParentOf(expression);
  }

  /**
   * Set the expression that is executed if the condition evaluates to {@code false} to the given
   * expression.
   * 
   * @param expression the expression that is executed if the condition evaluates to {@code false}
   */
  public void setElseExpression(Expression expression) {
    elseExpression = becomeParentOf(expression);
  }

  /**
   * Set the token used to separate the condition from the then expression to the given token.
   * 
   * @param question the token used to separate the condition from the then expression
   */
  public void setQuestion(Token question) {
    this.question = question;
  }

  /**
   * Set the expression that is executed if the condition evaluates to {@code true} to the given
   * expression.
   * 
   * @param expression the expression that is executed if the condition evaluates to {@code true}
   */
  public void setThenExpression(Expression expression) {
    thenExpression = becomeParentOf(expression);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(condition, visitor);
    safelyVisitChild(thenExpression, visitor);
    safelyVisitChild(elseExpression, visitor);
  }
}
