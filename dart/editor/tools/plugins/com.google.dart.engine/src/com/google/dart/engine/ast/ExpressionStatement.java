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
 * Instances of the class {@code ExpressionStatement} wrap an expression as a statement.
 * 
 * <pre>
 * expressionStatement ::=
 *     {@link Expression expression}? ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ExpressionStatement extends Statement {
  /**
   * The expression that comprises the statement.
   */
  private Expression expression;

  /**
   * The semicolon terminating the statement, or {@code null} if the expression is a function
   * expression and therefore isn't followed by a semicolon.
   */
  private Token semicolon;

  /**
   * Initialize a newly created expression statement.
   * 
   * @param expression the expression that comprises the statement
   * @param semicolon the semicolon terminating the statement
   */
  public ExpressionStatement(Expression expression, Token semicolon) {
    this.expression = becomeParentOf(expression);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitExpressionStatement(this);
  }

  @Override
  public Token getBeginToken() {
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
   * Return the expression that comprises the statement.
   * 
   * @return the expression that comprises the statement
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the semicolon terminating the statement, or {@code null} if the expression is a function
   * expression and therefore isn't followed by a semicolon.
   * 
   * @return the semicolon terminating the statement
   */
  public Token getSemicolon() {
    return semicolon;
  }

  @Override
  public boolean isSynthetic() {
    return expression.isSynthetic() && semicolon.isSynthetic();
  }

  /**
   * Set the expression that comprises the statement to the given expression.
   * 
   * @param expression the expression that comprises the statement
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the semicolon terminating the statement to the given token.
   * 
   * @param semicolon the semicolon terminating the statement
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(expression, visitor);
  }
}
