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
 * Instances of the class <code>CatchClause</code> represent a catch clause within a try statement.
 * 
 * <pre>
 * catchClause ::=
 *     'catch' '(' {@link SimpleFormalParameter exceptionParameter} (',' {@link SimpleFormalParameter stackTraceParameter})? ')' {@link Block block}
 * </pre>
 */
public class CatchClause extends ASTNode {
  /**
   * The token representing the 'catch' keyword.
   */
  private Token keyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The parameter whose value will be the exception that was thrown.
   */
  private SimpleFormalParameter exceptionParameter;

  /**
   * The comma separating the exception parameter from the stack trace parameter.
   */
  private Token comma;

  /**
   * The parameter whose value will be the stack trace associated with the exception.
   */
  private SimpleFormalParameter stackTraceParameter;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The body of the catch block.
   */
  private Block body;

  /**
   * Initialize a newly created catch clause.
   */
  public CatchClause() {
  }

  /**
   * Initialize a newly created catch clause.
   * 
   * @param keyword the token representing the 'catch' keyword
   * @param leftParenthesis the left parenthesis
   * @param exceptionParameter the parameter whose value will be the exception that was thrown
   * @param comma the comma separating the exception parameter from the stack trace parameter
   * @param stackTraceParameter the parameter whose value will be the stack trace associated with
   *          the exception
   * @param rightParenthesis the right parenthesis
   * @param body the body of the catch block
   */
  public CatchClause(Token keyword, Token leftParenthesis,
      SimpleFormalParameter exceptionParameter, Token comma,
      SimpleFormalParameter stackTraceParameter, Token rightParenthesis, Block body) {
    this.keyword = keyword;
    this.leftParenthesis = leftParenthesis;
    this.exceptionParameter = becomeParentOf(exceptionParameter);
    this.comma = comma;
    this.stackTraceParameter = becomeParentOf(stackTraceParameter);
    this.rightParenthesis = rightParenthesis;
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitCatchClause(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  /**
   * Return the body of the catch block.
   * 
   * @return the body of the catch block
   */
  public Block getBody() {
    return body;
  }

  /**
   * Return the comma.
   * 
   * @return the comma
   */
  public Token getComma() {
    return comma;
  }

  @Override
  public Token getEndToken() {
    return body.getEndToken();
  }

  /**
   * Return the parameter whose value will be the exception that was thrown.
   * 
   * @return the parameter whose value will be the exception that was thrown
   */
  public SimpleFormalParameter getExceptionParameter() {
    return exceptionParameter;
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
   * Return the parameter whose value will be the stack trace associated with the exception.
   * 
   * @return the parameter whose value will be the stack trace associated with the exception
   */
  public SimpleFormalParameter getStackTraceParameter() {
    return stackTraceParameter;
  }

  /**
   * Set the body of the catch block to the given block.
   * 
   * @param block the body of the catch block
   */
  public void setBody(Block block) {
    body = becomeParentOf(block);
  }

  /**
   * Set the comma to the given token.
   * 
   * @param comma the comma
   */
  public void setComma(Token comma) {
    this.comma = comma;
  }

  /**
   * Set the parameter whose value will be the exception that was thrown to the given parameter.
   * 
   * @param parameter the parameter whose value will be the exception that was thrown
   */
  public void setExceptionParameter(SimpleFormalParameter parameter) {
    exceptionParameter = becomeParentOf(parameter);
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
   * Set the parameter whose value will be the stack trace associated with the exception to the
   * given parameter.
   * 
   * @param parameter the parameter whose value will be the stack trace associated with the
   *          exception
   */
  public void setStackTraceParameter(SimpleFormalParameter parameter) {
    stackTraceParameter = becomeParentOf(parameter);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(exceptionParameter, visitor);
    safelyVisitChild(stackTraceParameter, visitor);
    safelyVisitChild(body, visitor);
  }
}
