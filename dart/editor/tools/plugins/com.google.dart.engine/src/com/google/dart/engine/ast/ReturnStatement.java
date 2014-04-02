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
 * Instances of the class {@code ReturnStatement} represent a return statement.
 * 
 * <pre>
 * returnStatement ::=
 *     'return' {@link Expression expression}? ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ReturnStatement extends Statement {
  /**
   * The token representing the 'return' keyword.
   */
  private Token keyword;

  /**
   * The expression computing the value to be returned, or {@code null} if no explicit value was
   * provided.
   */
  private Expression expression;

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created return statement.
   * 
   * @param keyword the token representing the 'return' keyword
   * @param expression the expression computing the value to be returned
   * @param semicolon the semicolon terminating the statement
   */
  public ReturnStatement(Token keyword, Expression expression, Token semicolon) {
    this.keyword = keyword;
    this.expression = becomeParentOf(expression);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitReturnStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the expression computing the value to be returned, or {@code null} if no explicit value
   * was provided.
   * 
   * @return the expression computing the value to be returned
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the token representing the 'return' keyword.
   * 
   * @return the token representing the 'return' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the semicolon terminating the statement.
   * 
   * @return the semicolon terminating the statement
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the expression computing the value to be returned to the given expression.
   * 
   * @param expression the expression computing the value to be returned
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the token representing the 'return' keyword to the given token.
   * 
   * @param keyword the token representing the 'return' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
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
