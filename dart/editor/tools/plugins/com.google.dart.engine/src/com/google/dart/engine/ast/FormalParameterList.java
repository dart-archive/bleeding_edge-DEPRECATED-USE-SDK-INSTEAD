/*
 * Copyright (c) 2012, the Dart project authors.
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
 * Instances of the class {@code FormalParameterList} represent the formal parameter list of a
 * method declaration, function declaration, or function type alias.
 * 
 * <pre>
 * formalParameterList ::=
 *    '(' ')'
 *  | '(' normalFormalParameters (',' namedFormalParameters)? ')'
 *  | '(' namedFormalParameters ')'
 *
 * normalFormalParameters ::=
 *     {@link NormalFormalParameter normalFormalParameter} (',' {@link NormalFormalParameter normalFormalParameter})*
 *
 * namedFormalParameters ::=
 *     '[' {@link DefaultFormalParameter namedFormalParameter} (',' {@link DefaultFormalParameter namedFormalParameter})* ']'
 * </pre>
 */
public class FormalParameterList extends ASTNode {
  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The parameters associated with the method.
   */
  private NodeList<FormalParameter> parameters = new NodeList<FormalParameter>(this);

  /**
   * The left square bracket.
   */
  private Token leftBracket;

  /**
   * The right square bracket.
   */
  private Token rightBracket;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * Initialize a newly created parameter list.
   */
  public FormalParameterList() {
  }

  /**
   * Initialize a newly created parameter list.
   * 
   * @param leftParenthesis the left parenthesis
   * @param parameters the parameters associated with the method
   * @param leftBracket the left square bracket
   * @param rightBracket the right square bracket
   * @param rightParenthesis the right parenthesis
   */
  public FormalParameterList(Token leftParenthesis, List<FormalParameter> parameters,
      Token leftBracket, Token rightBracket, Token rightParenthesis) {
    this.leftParenthesis = leftParenthesis;
    this.parameters.addAll(parameters);
    this.leftBracket = leftBracket;
    this.rightBracket = rightBracket;
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitFormalParameterList(this);
  }

  @Override
  public Token getBeginToken() {
    return leftParenthesis;
  }

  @Override
  public Token getEndToken() {
    return rightParenthesis;
  }

  /**
   * Return the left square bracket.
   * 
   * @return the left square bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
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
   * Return the parameters associated with the method.
   * 
   * @return the parameters associated with the method
   */
  public NodeList<FormalParameter> getParameters() {
    return parameters;
  }

  /**
   * Return the right square bracket.
   * 
   * @return the right square bracket
   */
  public Token getRightBracket() {
    return rightBracket;
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
   * Set the left square bracket to the given token.
   * 
   * @param bracket the left square bracket
   */
  public void setLeftBracket(Token bracket) {
    leftBracket = bracket;
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
   * Set the right square bracket to the given token.
   * 
   * @param bracket the right square bracket
   */
  public void setRightBracket(Token bracket) {
    rightBracket = bracket;
  }

  /**
   * Set the right parenthesis to the given token.
   * 
   * @param parenthesis the right parenthesis
   */
  public void setRightParenthesis(Token parenthesis) {
    rightParenthesis = parenthesis;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    parameters.accept(visitor);
  }
}
