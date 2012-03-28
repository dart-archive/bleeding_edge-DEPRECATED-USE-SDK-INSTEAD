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
 * Instances of the class <code>AssertStatement</code> represent an assert statement.
 * 
 * <pre>
 * assertStatement ::=
 *     'assert' '(' {@link Expression condition} ')' ';'
 * </pre>
 */
public class AssertStatement extends Statement {
  /**
   * The token representing the 'assert' keyword.
   */
  private Token keyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The condition being asserted.
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
   * Initialize a newly created assert statement to have the given condition.
   */
  public AssertStatement() {
  }

  /**
   * Initialize a newly created assert statement to have the given condition.
   * 
   * @param keyword the token representing the 'assert' keyword
   * @param leftParenthesis the left parenthesis
   * @param condition the condition being asserted
   * @param rightParenthesis the right parenthesis
   * @param semicolon the semicolon terminating the statement
   */
  public AssertStatement(Token keyword, Token leftParenthesis, Expression condition,
      Token rightParenthesis, Token semicolon) {
    this.keyword = keyword;
    this.leftParenthesis = leftParenthesis;
    this.condition = becomeParentOf(condition);
    this.rightParenthesis = rightParenthesis;
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitAssertStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  /**
   * Return the condition being asserted.
   * 
   * @return the condition being asserted
   */
  public Expression getCondition() {
    return condition;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the 'assert' keyword.
   * 
   * @return the token representing the 'assert' keyword
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
   * Return the semicolon terminating the statement.
   * 
   * @return the semicolon terminating the statement
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the condition being asserted to the given expression.
   * 
   * @param expression the condition being asserted
   */
  public void setCondition(Expression expression) {
    condition = becomeParentOf(expression);
  }

  /**
   * Set the token representing the 'assert' keyword to the given token.
   * 
   * @param keyword the token representing the 'assert' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
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

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(condition, visitor);
  }
}
