/*
 * Copyright (c) 2014, the Dart project authors.
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
 * Instances of the class {@code AwaitExpression} implement an await expression.
 */
public class AwaitExpression extends Expression {
  /**
   * The 'await' keyword.
   */
  private Token awaitKeyword;

  /**
   * The expression whose value is being waited on.
   */
  private Expression expression;

  /**
   * The semicolon following the expression.
   */
  private Token semicolon;

  /**
   * Initialize a newly created await expression.
   * 
   * @param awaitKeyword the 'await' keyword
   * @param expression the expression whose value is being waited on
   * @param semicolon the semicolon following the expression
   */
  public AwaitExpression(Token awaitKeyword, Expression expression, Token semicolon) {
    this.awaitKeyword = awaitKeyword;
    this.expression = becomeParentOf(expression);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitAwaitExpression(this);
  }

  /**
   * Return the 'await' keyword.
   * 
   * @return the 'await' keyword
   */
  public Token getAwaitKeyword() {
    return awaitKeyword;
  }

  @Override
  public Token getBeginToken() {
    if (awaitKeyword != null) {
      return awaitKeyword;
    }
    return expression.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    if (semicolon != null) {
      return semicolon;
    }
    return expression.getEndToken();
  }

  /**
   * Return the expression whose value is being waited on.
   * 
   * @return the expression whose value is being waited on
   */
  public Expression getExpression() {
    return expression;
  }

  @Override
  public int getPrecedence() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * Return the semicolon following the expression.
   * 
   * @return the semicolon following the expression
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the 'await' keyword to the given token.
   * 
   * @param awaitKeyword the 'await' keyword
   */
  public void setAwaitKeyword(Token awaitKeyword) {
    this.awaitKeyword = awaitKeyword;
  }

  /**
   * Set the expression whose value is being waited on to the given expression.
   * 
   * @param expression the expression whose value is being waited on
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the semicolon following the expression to the given token.
   * 
   * @param semicolon the semicolon following the expression
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(expression, visitor);
  }
}
