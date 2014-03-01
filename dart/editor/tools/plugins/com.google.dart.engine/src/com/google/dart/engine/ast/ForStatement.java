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

import java.util.List;

/**
 * Instances of the class {@code ForStatement} represent a for statement.
 * 
 * <pre>
 * forStatement ::=
 *     'for' '(' forLoopParts ')' {@link Statement statement}
 *
 * forLoopParts ::=
 *     forInitializerStatement ';' {@link Expression expression}? ';' {@link Expression expressionList}?
 *
 * forInitializerStatement ::=
 *     {@link DefaultFormalParameter initializedVariableDeclaration}
 *   | {@link Expression expression}?
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ForStatement extends Statement {
  /**
   * The token representing the 'for' keyword.
   */
  private Token forKeyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The declaration of the loop variables, or {@code null} if there are no variables. Note that a
   * for statement cannot have both a variable list and an initialization expression, but can
   * validly have neither.
   */
  private VariableDeclarationList variableList;

  /**
   * The initialization expression, or {@code null} if there is no initialization expression. Note
   * that a for statement cannot have both a variable list and an initialization expression, but can
   * validly have neither.
   */
  private Expression initialization;

  /**
   * The semicolon separating the initializer and the condition.
   */
  private Token leftSeparator;

  /**
   * The condition used to determine when to terminate the loop, or {@code null} if there is no
   * condition.
   */
  private Expression condition;

  /**
   * The semicolon separating the condition and the updater.
   */
  private Token rightSeparator;

  /**
   * The list of expressions run after each execution of the loop body.
   */
  private NodeList<Expression> updaters = new NodeList<Expression>(this);

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The body of the loop.
   */
  private Statement body;

  /**
   * Initialize a newly created for statement.
   * 
   * @param forKeyword the token representing the 'for' keyword
   * @param leftParenthesis the left parenthesis
   * @param variableList the declaration of the loop variables
   * @param initialization the initialization expression
   * @param leftSeparator the semicolon separating the initializer and the condition
   * @param condition the condition used to determine when to terminate the loop
   * @param rightSeparator the semicolon separating the condition and the updater
   * @param updaters the list of expressions run after each execution of the loop body
   * @param rightParenthesis the right parenthesis
   * @param body the body of the loop
   */
  public ForStatement(Token forKeyword, Token leftParenthesis,
      VariableDeclarationList variableList, Expression initialization, Token leftSeparator,
      Expression condition, Token rightSeparator, List<Expression> updaters,
      Token rightParenthesis, Statement body) {
    this.forKeyword = forKeyword;
    this.leftParenthesis = leftParenthesis;
    this.variableList = becomeParentOf(variableList);
    this.initialization = becomeParentOf(initialization);
    this.leftSeparator = leftSeparator;
    this.condition = becomeParentOf(condition);
    this.rightSeparator = rightSeparator;
    this.updaters.addAll(updaters);
    this.rightParenthesis = rightParenthesis;
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitForStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return forKeyword;
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
   * Return the condition used to determine when to terminate the loop, or {@code null} if there is
   * no condition.
   * 
   * @return the condition used to determine when to terminate the loop
   */
  public Expression getCondition() {
    return condition;
  }

  @Override
  public Token getEndToken() {
    return body.getEndToken();
  }

  /**
   * Return the token representing the 'for' keyword.
   * 
   * @return the token representing the 'for' keyword
   */
  public Token getForKeyword() {
    return forKeyword;
  }

  /**
   * Return the initialization expression, or {@code null} if there is no initialization expression.
   * 
   * @return the initialization expression
   */
  public Expression getInitialization() {
    return initialization;
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
   * Return the semicolon separating the initializer and the condition.
   * 
   * @return the semicolon separating the initializer and the condition
   */
  public Token getLeftSeparator() {
    return leftSeparator;
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
   * Return the semicolon separating the condition and the updater.
   * 
   * @return the semicolon separating the condition and the updater
   */
  public Token getRightSeparator() {
    return rightSeparator;
  }

  /**
   * Return the list of expressions run after each execution of the loop body.
   * 
   * @return the list of expressions run after each execution of the loop body
   */
  public NodeList<Expression> getUpdaters() {
    return updaters;
  }

  /**
   * Return the declaration of the loop variables, or {@code null} if there are no variables.
   * 
   * @return the declaration of the loop variables, or {@code null} if there are no variables
   */
  public VariableDeclarationList getVariables() {
    return variableList;
  }

  /**
   * Set the body of the loop to the given statement.
   * 
   * @param body the body of the loop
   */
  public void setBody(Statement body) {
    this.body = becomeParentOf(body);
  }

  /**
   * Set the condition used to determine when to terminate the loop to the given expression.
   * 
   * @param expression the condition used to determine when to terminate the loop
   */
  public void setCondition(Expression expression) {
    condition = becomeParentOf(expression);
  }

  /**
   * Set the token representing the 'for' keyword to the given token.
   * 
   * @param forKeyword the token representing the 'for' keyword
   */
  public void setForKeyword(Token forKeyword) {
    this.forKeyword = forKeyword;
  }

  /**
   * Set the initialization expression to the given expression.
   * 
   * @param initialization the initialization expression
   */
  public void setInitialization(Expression initialization) {
    this.initialization = becomeParentOf(initialization);
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
   * Set the semicolon separating the initializer and the condition to the given token.
   * 
   * @param leftSeparator the semicolon separating the initializer and the condition
   */
  public void setLeftSeparator(Token leftSeparator) {
    this.leftSeparator = leftSeparator;
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
   * Set the semicolon separating the condition and the updater to the given token.
   * 
   * @param rightSeparator the semicolon separating the condition and the updater
   */
  public void setRightSeparator(Token rightSeparator) {
    this.rightSeparator = rightSeparator;
  }

  /**
   * Set the declaration of the loop variables to the given parameter.
   * 
   * @param variableList the declaration of the loop variables
   */
  public void setVariables(VariableDeclarationList variableList) {
    variableList = becomeParentOf(variableList);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(variableList, visitor);
    safelyVisitChild(initialization, visitor);
    safelyVisitChild(condition, visitor);
    updaters.accept(visitor);
    safelyVisitChild(body, visitor);
  }
}
