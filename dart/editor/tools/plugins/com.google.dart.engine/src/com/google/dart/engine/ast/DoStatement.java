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
 * Instances of the class <code>DoStatement</code> represent a do statement.
 * 
 * <pre>
 * doStatement ::=
 *     'do' {@link Statement body} 'while' '(' {@link Expression condition} ')' ';'
 * </pre>
 */
public class DoStatement extends Statement {
  /**
   * The token representing the 'do' keyword.
   */
  private Token doKeyword;

  /**
   * The body of the loop.
   */
  private Statement body;

  /**
   * The token representing the 'while' keyword.
   */
  private Token whileKeyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The condition that determines when the loop will terminate.
   */
  private Expression condition;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created do loop.
   */
  public DoStatement() {
  }

  /**
   * Initialize a newly created do loop.
   * 
   * @param doKeyword the token representing the 'do' keyword
   * @param body the body of the loop
   * @param whileKeyword the token representing the 'while' keyword
   * @param leftParenthesis the left parenthesis
   * @param condition the condition that determines when the loop will terminate
   * @param rightParenthesis the right parenthesis
   * @param semicolon the semicolon terminating the statement
   */
  public DoStatement(Token doKeyword, Statement body, Token whileKeyword, Token leftParenthesis,
      Expression condition, Token rightParenthesis, Token semicolon) {
    this.doKeyword = doKeyword;
    this.body = becomeParentOf(body);
    this.whileKeyword = whileKeyword;
    this.leftParenthesis = leftParenthesis;
    this.condition = becomeParentOf(condition);
    this.rightParenthesis = rightParenthesis;
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitDoStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return doKeyword;
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
   * Return the condition that determines when the loop will terminate.
   * 
   * @return the condition that determines when the loop will terminate
   */
  public Expression getCondition() {
    return condition;
  }

  /**
   * Return the token representing the 'do' keyword.
   * 
   * @return the token representing the 'do' keyword
   */
  public Token getDoKeyword() {
    return doKeyword;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
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
   * Return the semicolon terminating the statement.
   * 
   * @return the semicolon terminating the statement
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Return the token representing the 'while' keyword.
   * 
   * @return the token representing the 'while' keyword
   */
  public Token getWhileKeyword() {
    return whileKeyword;
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
   * Set the condition that determines when the loop will terminate to the given expression.
   * 
   * @param expression the condition that determines when the loop will terminate
   */
  public void setCondition(Expression expression) {
    condition = becomeParentOf(expression);
  }

  /**
   * Set the token representing the 'do' keyword to the given token.
   * 
   * @param doKeyword the token representing the 'do' keyword
   */
  public void setDoKeyword(Token doKeyword) {
    this.doKeyword = doKeyword;
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

  /**
   * Set the semicolon terminating the statement to the given token.
   * 
   * @param semicolon the semicolon terminating the statement
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  /**
   * Set the token representing the 'while' keyword to the given token.
   * 
   * @param whileKeyword the token representing the 'while' keyword
   */
  public void setWhileKeyword(Token whileKeyword) {
    this.whileKeyword = whileKeyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(body, visitor);
    safelyVisitChild(condition, visitor);
  }
}
