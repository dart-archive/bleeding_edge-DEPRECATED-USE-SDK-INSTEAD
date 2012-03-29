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
 * Instances of the class <code>WhileStatement</code> represent a while statement.
 * 
 * <pre>
 * whileStatement ::=
 *     'while' '(' {@link Expression condition} ')' {@link Statement body}
 * </pre>
 */
public class WhileStatement extends Statement {
  /**
   * The token representing the 'while' keyword.
   */
  private Token keyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The expression used to determine whether to execute the body of the loop.
   */
  private Expression condition;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The body of the loop.
   */
  private Statement body;

  /**
   * Initialize a newly created while statement.
   */
  public WhileStatement() {
  }

  /**
   * Initialize a newly created while statement.
   * 
   * @param keyword the token representing the 'while' keyword
   * @param leftParenthesis the left parenthesis
   * @param condition the expression used to determine whether to execute the body of the loop
   * @param rightParenthesis the right parenthesis
   * @param body the body of the loop
   */
  public WhileStatement(Token keyword, Token leftParenthesis, Expression condition,
      Token rightParenthesis, Statement body) {
    this.keyword = keyword;
    this.leftParenthesis = leftParenthesis;
    this.condition = becomeParentOf(condition);
    this.rightParenthesis = rightParenthesis;
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitWhileStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  /**
   * Return the body of the loop.
   * 
   * @return the body of the loop
   */
  public Statement getBody() {
    return body;
  }

  /**
   * Return the expression used to determine whether to execute the body of the loop.
   * 
   * @return the expression used to determine whether to execute the body of the loop
   */
  public Expression getCondition() {
    return condition;
  }

  @Override
  public Token getEndToken() {
    return body.getEndToken();
  }

  /**
   * Return the token representing the 'while' keyword.
   * 
   * @return the token representing the 'while' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the left parenthesis.
   * 
   * @return the left parenthesis
   */
  public Token getLeftParenthesis() {
    return leftParenthesis;
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
   * Set the body of the loop to the given statement.
   * 
   * @param statement the body of the loop
   */
  public void setBody(Statement statement) {
    body = becomeParentOf(statement);
  }

  /**
   * Set the expression used to determine whether to execute the body of the loop to the given
   * expression.
   * 
   * @param expression the expression used to determine whether to execute the body of the loop
   */
  public void setCondition(Expression expression) {
    condition = becomeParentOf(expression);
  }

  /**
   * Set the token representing the 'while' keyword to the given token.
   * 
   * @param keyword the token representing the 'while' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the left parenthesis to the given token.
   * 
   * @param leftParenthesis the left parenthesis
   */
  public void setLeftParenthesis(Token leftParenthesis) {
    this.leftParenthesis = leftParenthesis;
  }

  /**
   * Set the right parenthesis to the given token.
   * 
   * @param rightParenthesis the right parenthesis
   */
  public void setRightParenthesis(Token rightParenthesis) {
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(condition, visitor);
    safelyVisitChild(body, visitor);
  }
}
