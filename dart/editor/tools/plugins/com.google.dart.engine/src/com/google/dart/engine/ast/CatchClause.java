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
 * Instances of the class {@code CatchClause} represent a catch clause within a try statement.
 * 
 * <pre>
 * onPart ::=
 *     catchPart {@link Block block}
 *   | 'on' type catchPart? {@link Block block}
 * 
 * catchPart ::=
 *     'catch' '(' {@link SimpleIdentifier exceptionParameter} (',' {@link SimpleIdentifier stackTraceParameter})? ')'
 * </pre>
 */
public class CatchClause extends ASTNode {
  /**
   * The token representing the 'on' keyword, or {@code null} if there is no 'on' keyword.
   */
  private Token onKeyword;

  /**
   * The type of exceptions caught by this catch clause, or {@code null} if this catch clause
   * catches every type of exception.
   */
  private TypeName exceptionType;

  /**
   * The token representing the 'catch' keyword, or {@code null} if there is no 'catch' keyword.
   */
  private Token catchKeyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The parameter whose value will be the exception that was thrown.
   */
  private SimpleIdentifier exceptionParameter;

  /**
   * The comma separating the exception parameter from the stack trace parameter.
   */
  private Token comma;

  /**
   * The parameter whose value will be the stack trace associated with the exception.
   */
  private SimpleIdentifier stackTraceParameter;

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
   * @param onKeyword the token representing the 'on' keyword
   * @param exceptionType the type of exceptions caught by this catch clause
   * @param leftParenthesis the left parenthesis
   * @param exceptionParameter the parameter whose value will be the exception that was thrown
   * @param comma the comma separating the exception parameter from the stack trace parameter
   * @param stackTraceParameter the parameter whose value will be the stack trace associated with
   *          the exception
   * @param rightParenthesis the right parenthesis
   * @param body the body of the catch block
   */
  public CatchClause(Token onKeyword, TypeName exceptionType, Token catchKeyword,
      Token leftParenthesis, SimpleIdentifier exceptionParameter, Token comma,
      SimpleIdentifier stackTraceParameter, Token rightParenthesis, Block body) {
    this.onKeyword = onKeyword;
    this.exceptionType = becomeParentOf(exceptionType);
    this.catchKeyword = catchKeyword;
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
    if (onKeyword != null) {
      return onKeyword;
    }
    return catchKeyword;
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
   * Return the token representing the 'catch' keyword, or {@code null} if there is no 'catch'
   * keyword.
   * 
   * @return the token representing the 'catch' keyword
   */
  public Token getCatchKeyword() {
    return catchKeyword;
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
  public SimpleIdentifier getExceptionParameter() {
    return exceptionParameter;
  }

  /**
   * Return the type of exceptions caught by this catch clause, or {@code null} if this catch clause
   * catches every type of exception.
   * 
   * @return the type of exceptions caught by this catch clause
   */
  public TypeName getExceptionType() {
    return exceptionType;
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
   * Return the token representing the 'on' keyword, or {@code null} if there is no 'on' keyword.
   * 
   * @return the token representing the 'on' keyword
   */
  public Token getOnKeyword() {
    return onKeyword;
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
  public SimpleIdentifier getStackTraceParameter() {
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
   * Set the token representing the 'catch' keyword to the given token.
   * 
   * @param catchKeyword the token representing the 'catch' keyword
   */
  public void setCatchKeyword(Token catchKeyword) {
    this.catchKeyword = catchKeyword;
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
  public void setExceptionParameter(SimpleIdentifier parameter) {
    exceptionParameter = becomeParentOf(parameter);
  }

  /**
   * Set the type of exceptions caught by this catch clause to the given type.
   * 
   * @param exceptionType the type of exceptions caught by this catch clause
   */
  public void setExceptionType(TypeName exceptionType) {
    this.exceptionType = exceptionType;
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
   * Set the token representing the 'on' keyword to the given keyword.
   * 
   * @param onKeyword the token representing the 'on' keyword
   */
  public void setOnKeyword(Token onKeyword) {
    this.onKeyword = onKeyword;
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
  public void setStackTraceParameter(SimpleIdentifier parameter) {
    stackTraceParameter = becomeParentOf(parameter);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(exceptionType, visitor);
    safelyVisitChild(exceptionParameter, visitor);
    safelyVisitChild(stackTraceParameter, visitor);
    safelyVisitChild(body, visitor);
  }
}
