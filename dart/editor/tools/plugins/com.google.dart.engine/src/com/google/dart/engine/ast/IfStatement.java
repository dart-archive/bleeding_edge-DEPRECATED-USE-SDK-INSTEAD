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
 * Instances of the class <code>IfStatement</code> represent an if statement.
 * 
 * <pre>
 * ifStatement ::=
 *     'if' '(' expression ')' thenStatement ('else' elseStatement)?
 * </pre>
 */
public class IfStatement extends Statement {
  /**
   * The token representing the 'if' keyword.
   */
  private Token ifKeyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The condition used to determine which of the statements is executed next.
   */
  private Expression condition;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The statement that is executed if the condition evaluates to <code>true</code>.
   */
  private Statement thenStatement;

  /**
   * The token representing the 'else' keyword.
   */
  private Token elseKeyword;

  /**
   * The statement that is executed if the condition evaluates to <code>false</code>, or
   * <code>null</code> if there is no else statement.
   */
  private Statement elseStatement;

  /**
   * Initialize a newly created if statement.
   */
  public IfStatement() {
  }

  /**
   * Initialize a newly created if statement.
   * 
   * @param ifKeyword the token representing the 'if' keyword
   * @param leftParenthesis the left parenthesis
   * @param condition the condition used to determine which of the statements is executed next
   * @param rightParenthesis the right parenthesis
   * @param thenStatement the statement that is executed if the condition evaluates to
   *          <code>true</code>
   * @param elseKeyword the token representing the 'else' keyword
   * @param elseStatement the statement that is executed if the condition evaluates to
   *          <code>false</code>
   */
  public IfStatement(Token ifKeyword, Token leftParenthesis, Expression condition,
      Token rightParenthesis, Statement thenStatement, Token elseKeyword, Statement elseStatement) {
    this.ifKeyword = ifKeyword;
    this.leftParenthesis = leftParenthesis;
    this.condition = becomeParentOf(condition);
    this.rightParenthesis = rightParenthesis;
    this.thenStatement = becomeParentOf(thenStatement);
    this.elseKeyword = elseKeyword;
    this.elseStatement = becomeParentOf(elseStatement);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitIfStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return ifKeyword;
  }

  /**
   * Return the condition used to determine which of the statements is executed next.
   * 
   * @return the condition used to determine which statement is executed next
   */
  public Expression getCondition() {
    return condition;
  }

  /**
   * Return the token representing the 'else' keyword.
   * 
   * @return the token representing the 'else' keyword
   */
  public Token getElseKeyword() {
    return elseKeyword;
  }

  /**
   * Return the statement that is executed if the condition evaluates to <code>false</code>, or
   * <code>null</code> if there is no else statement.
   * 
   * @return the statement that is executed if the condition evaluates to <code>false</code>
   */
  public Statement getElseStatement() {
    return elseStatement;
  }

  @Override
  public Token getEndToken() {
    if (elseStatement != null) {
      return elseStatement.getEndToken();
    }
    return thenStatement.getEndToken();
  }

  /**
   * Return the token representing the 'if' keyword.
   * 
   * @return the token representing the 'if' keyword
   */
  public Token getIfKeyword() {
    return ifKeyword;
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
   * Return the statement that is executed if the condition evaluates to <code>true</code>.
   * 
   * @return the statement that is executed if the condition evaluates to <code>true</code>
   */
  public Statement getThenStatement() {
    return thenStatement;
  }

  /**
   * Set the condition used to determine which of the statements is executed next to the given
   * expression.
   * 
   * @param expression the condition used to determine which statement is executed next
   */
  public void setCondition(Expression expression) {
    condition = becomeParentOf(expression);
  }

  /**
   * Set the token representing the 'else' keyword to the given token.
   * 
   * @param elseKeyword the token representing the 'else' keyword
   */
  public void setElseKeyword(Token elseKeyword) {
    this.elseKeyword = elseKeyword;
  }

  /**
   * Set the statement that is executed if the condition evaluates to <code>false</code> to the
   * given statement.
   * 
   * @param statement the statement that is executed if the condition evaluates to
   *          <code>false</code>
   */
  public void setElseStatement(Statement statement) {
    elseStatement = becomeParentOf(statement);
  }

  /**
   * Set the token representing the 'if' keyword to the given token.
   * 
   * @param ifKeyword the token representing the 'if' keyword
   */
  public void setIfKeyword(Token ifKeyword) {
    this.ifKeyword = ifKeyword;
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

  /**
   * Set the statement that is executed if the condition evaluates to <code>true</code> to the given
   * statement.
   * 
   * @param statement the statement that is executed if the condition evaluates to <code>true</code>
   */
  public void setThenStatement(Statement statement) {
    thenStatement = becomeParentOf(statement);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(condition, visitor);
    safelyVisitChild(thenStatement, visitor);
    safelyVisitChild(elseStatement, visitor);
  }
}
