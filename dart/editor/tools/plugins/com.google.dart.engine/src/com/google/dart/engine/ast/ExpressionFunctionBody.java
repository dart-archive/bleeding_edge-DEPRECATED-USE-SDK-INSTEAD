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
 * Instances of the class {@code ExpressionFunctionBody} represent a function body consisting of a
 * single expression.
 * 
 * <pre>
 * expressionFunctionBody ::=
 *     'async'? '=>' {@link Expression expression} ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ExpressionFunctionBody extends FunctionBody {
  /**
   * The token representing the 'async' keyword, or {@code null} if there is no such keyword.
   */
  private Token keyword;

  /**
   * The token introducing the expression that represents the body of the function.
   */
  private Token functionDefinition;

  /**
   * The expression representing the body of the function.
   */
  private Expression expression;

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created function body consisting of a block of statements.
   * 
   * @param keyword the token representing the 'async' keyword
   * @param functionDefinition the token introducing the expression that represents the body of the
   *          function
   * @param expression the expression representing the body of the function
   * @param semicolon the semicolon terminating the statement
   */
  public ExpressionFunctionBody(Token keyword, Token functionDefinition, Expression expression,
      Token semicolon) {
    this.keyword = keyword;
    this.functionDefinition = functionDefinition;
    this.expression = becomeParentOf(expression);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitExpressionFunctionBody(this);
  }

  @Override
  public Token getBeginToken() {
    return functionDefinition;
  }

  @Override
  public Token getEndToken() {
    if (semicolon != null) {
      return semicolon;
    }
    return expression.getEndToken();
  }

  /**
   * Return the expression representing the body of the function.
   * 
   * @return the expression representing the body of the function
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the token introducing the expression that represents the body of the function.
   * 
   * @return the function definition token
   */
  public Token getFunctionDefinition() {
    return functionDefinition;
  }

  /**
   * Return the token representing the 'async' keyword, or {@code null} if there is no such keyword.
   * 
   * @return the token representing the 'async' keyword
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

  @Override
  public boolean isAsynchronous() {
    return keyword != null;
  }

  @Override
  public boolean isSynchronous() {
    return keyword == null;
  }

  /**
   * Set the expression representing the body of the function to the given expression.
   * 
   * @param expression the expression representing the body of the function
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the token introducing the expression that represents the body of the function to the given
   * token.
   * 
   * @param functionDefinition the function definition token
   */
  public void setFunctionDefinition(Token functionDefinition) {
    this.functionDefinition = functionDefinition;
  }

  /**
   * Set the token representing the 'async' keyword to the given token.
   * 
   * @param keyword the token representing the 'async' keyword
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
