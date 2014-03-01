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

import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code FormalParameterList} represent the formal parameter list of a
 * method declaration, function declaration, or function type alias.
 * <p>
 * While the grammar requires all optional formal parameters to follow all of the normal formal
 * parameters and at most one grouping of optional formal parameters, this class does not enforce
 * those constraints. All parameters are flattened into a single list, which can have any or all
 * kinds of parameters (normal, named, and positional) in any order.
 * 
 * <pre>
 * formalParameterList ::=
 *     '(' ')'
 *   | '(' normalFormalParameters (',' optionalFormalParameters)? ')'
 *   | '(' optionalFormalParameters ')'
 *
 * normalFormalParameters ::=
 *     {@link NormalFormalParameter normalFormalParameter} (',' {@link NormalFormalParameter normalFormalParameter})*
 *
 * optionalFormalParameters ::=
 *     optionalPositionalFormalParameters
 *   | namedFormalParameters
 * 
 * optionalPositionalFormalParameters ::=
 *     '[' {@link DefaultFormalParameter positionalFormalParameter} (',' {@link DefaultFormalParameter positionalFormalParameter})* ']'
 * 
 * namedFormalParameters ::=
 *     '{' {@link DefaultFormalParameter namedFormalParameter} (',' {@link DefaultFormalParameter namedFormalParameter})* '}'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class FormalParameterList extends AstNode {
  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The parameters associated with the method.
   */
  private NodeList<FormalParameter> parameters = new NodeList<FormalParameter>(this);

  /**
   * The left square bracket ('[') or left curly brace ('{') introducing the optional parameters, or
   * {@code null} if there are no optional parameters.
   */
  private Token leftDelimiter;

  /**
   * The right square bracket (']') or right curly brace ('}') introducing the optional parameters,
   * or {@code null} if there are no optional parameters.
   */
  private Token rightDelimiter;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * Initialize a newly created parameter list.
   * 
   * @param leftParenthesis the left parenthesis
   * @param parameters the parameters associated with the method
   * @param leftDelimiter the left delimiter introducing the optional parameters
   * @param rightDelimiter the right delimiter introducing the optional parameters
   * @param rightParenthesis the right parenthesis
   */
  public FormalParameterList(Token leftParenthesis, List<FormalParameter> parameters,
      Token leftDelimiter, Token rightDelimiter, Token rightParenthesis) {
    this.leftParenthesis = leftParenthesis;
    this.parameters.addAll(parameters);
    this.leftDelimiter = leftDelimiter;
    this.rightDelimiter = rightDelimiter;
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
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
   * Return the left square bracket ('[') or left curly brace ('{') introducing the optional
   * parameters, or {@code null} if there are no optional parameters.
   * 
   * @return the left square bracket ('[') or left curly brace ('{') introducing the optional
   *         parameters
   */
  public Token getLeftDelimiter() {
    return leftDelimiter;
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
   * Return an array containing the elements representing the parameters in this list. The array
   * will contain {@code null}s if the parameters in this list have not been resolved.
   * 
   * @return the elements representing the parameters in this list
   */
  public ParameterElement[] getParameterElements() {
    int count = parameters.size();
    ParameterElement[] types = new ParameterElement[count];
    for (int i = 0; i < count; i++) {
      types[i] = parameters.get(i).getElement();
    }
    return types;
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
   * Return the right square bracket (']') or right curly brace ('}') introducing the optional
   * parameters, or {@code null} if there are no optional parameters.
   * 
   * @return the right square bracket (']') or right curly brace ('}') introducing the optional
   *         parameters
   */
  public Token getRightDelimiter() {
    return rightDelimiter;
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
   * Set the left square bracket ('[') or left curly brace ('{') introducing the optional parameters
   * to the given token.
   * 
   * @param bracket the left delimiter introducing the optional parameters
   */
  public void setLeftDelimiter(Token bracket) {
    leftDelimiter = bracket;
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
   * Set the right square bracket (']') or right curly brace ('}') introducing the optional
   * parameters to the given token.
   * 
   * @param bracket the right delimiter introducing the optional parameters
   */
  public void setRightDelimiter(Token bracket) {
    rightDelimiter = bracket;
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
  public void visitChildren(AstVisitor<?> visitor) {
    parameters.accept(visitor);
  }
}
