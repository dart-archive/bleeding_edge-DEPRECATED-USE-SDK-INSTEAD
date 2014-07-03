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
 * Instances of the class {@code ForEachStatement} represent a for-each statement.
 * 
 * <pre>
 * forEachStatement ::=
 *     'await'? 'for' '(' {@link DeclaredIdentifier loopParameter} 'in' {@link Expression iterator} ')' {@link Block body}
 *   | 'await'? 'for' '(' {@link SimpleIdentifier identifier} 'in' {@link Expression iterator} ')' {@link Block body}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ForEachStatement extends Statement {
  /**
   * The token representing the 'await' keyword, or {@code null} if there is no 'await' keyword.
   */
  private Token awaitKeyword;

  /**
   * The token representing the 'for' keyword.
   */
  private Token forKeyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The declaration of the loop variable, or {@code null} if the loop variable is a simple
   * identifier.
   */
  private DeclaredIdentifier loopVariable;

  /**
   * The loop variable, or {@code null} if the loop variable is declared in the 'for'.
   */
  private SimpleIdentifier identifier;

  /**
   * The token representing the 'in' keyword.
   */
  private Token inKeyword;

  /**
   * The expression evaluated to produce the iterator.
   */
  private Expression iterator;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The body of the loop.
   */
  private Statement body;

  /**
   * Initialize a newly created for-each statement.
   * 
   * @param awaitKeyword the token representing the 'await' keyword
   * @param forKeyword the token representing the 'for' keyword
   * @param leftParenthesis the left parenthesis
   * @param loopVariable the declaration of the loop variable
   * @param iterator the expression evaluated to produce the iterator
   * @param rightParenthesis the right parenthesis
   * @param body the body of the loop
   */
  public ForEachStatement(Token awaitKeyword, Token forKeyword, Token leftParenthesis,
      DeclaredIdentifier loopVariable, Token inKeyword, Expression iterator,
      Token rightParenthesis, Statement body) {
    this.awaitKeyword = awaitKeyword;
    this.forKeyword = forKeyword;
    this.leftParenthesis = leftParenthesis;
    this.loopVariable = becomeParentOf(loopVariable);
    this.inKeyword = inKeyword;
    this.iterator = becomeParentOf(iterator);
    this.rightParenthesis = rightParenthesis;
    this.body = becomeParentOf(body);
  }

  /**
   * Initialize a newly created for-each statement.
   * 
   * @param awaitKeyword the token representing the 'await' keyword
   * @param forKeyword the token representing the 'for' keyword
   * @param leftParenthesis the left parenthesis
   * @param identifier the loop variable
   * @param iterator the expression evaluated to produce the iterator
   * @param rightParenthesis the right parenthesis
   * @param body the body of the loop
   */
  public ForEachStatement(Token awaitKeyword, Token forKeyword, Token leftParenthesis,
      SimpleIdentifier identifier, Token inKeyword, Expression iterator, Token rightParenthesis,
      Statement body) {
    this.awaitKeyword = awaitKeyword;
    this.forKeyword = forKeyword;
    this.leftParenthesis = leftParenthesis;
    this.identifier = becomeParentOf(identifier);
    this.inKeyword = inKeyword;
    this.iterator = becomeParentOf(iterator);
    this.rightParenthesis = rightParenthesis;
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitForEachStatement(this);
  }

  /**
   * Return the token representing the 'await' keyword, or {@code null} if there is no 'await'
   * keyword.
   * 
   * @return the token representing the 'await' keyword
   */
  public Token getAwaitKeyword() {
    return awaitKeyword;
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
   * Return the loop variable, or {@code null} if the loop variable is declared in the 'for'.
   * 
   * @return the loop variable
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * Return the token representing the 'in' keyword.
   * 
   * @return the token representing the 'in' keyword
   */
  public Token getInKeyword() {
    return inKeyword;
  }

  /**
   * Return the expression evaluated to produce the iterator.
   * 
   * @return the expression evaluated to produce the iterator
   */
  public Expression getIterator() {
    return iterator;
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
   * Return the declaration of the loop variable, or {@code null} if the loop variable is a simple
   * identifier.
   * 
   * @return the declaration of the loop variable
   */
  public DeclaredIdentifier getLoopVariable() {
    return loopVariable;
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
   * Set the token representing the 'await' keyword to the given token.
   * 
   * @param awaitKeyword the token representing the 'await' keyword
   */
  public void setAwaitKeyword(Token awaitKeyword) {
    this.awaitKeyword = awaitKeyword;
  }

  /**
   * Set the body of the loop to the given block.
   * 
   * @param body the body of the loop
   */
  public void setBody(Statement body) {
    this.body = becomeParentOf(body);
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
   * Set the loop variable to the given variable.
   * 
   * @param identifier the loop variable
   */
  public void setIdentifier(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }

  /**
   * Set the token representing the 'in' keyword to the given token.
   * 
   * @param inKeyword the token representing the 'in' keyword
   */
  public void setInKeyword(Token inKeyword) {
    this.inKeyword = inKeyword;
  }

  /**
   * Set the expression evaluated to produce the iterator to the given expression.
   * 
   * @param expression the expression evaluated to produce the iterator
   */
  public void setIterator(Expression expression) {
    iterator = becomeParentOf(expression);
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
   * Set the declaration of the loop variable to the given variable.
   * 
   * @param variable the declaration of the loop variable
   */
  public void setLoopVariable(DeclaredIdentifier variable) {
    loopVariable = becomeParentOf(variable);
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
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(loopVariable, visitor);
    safelyVisitChild(identifier, visitor);
    safelyVisitChild(iterator, visitor);
    safelyVisitChild(body, visitor);
  }
}
