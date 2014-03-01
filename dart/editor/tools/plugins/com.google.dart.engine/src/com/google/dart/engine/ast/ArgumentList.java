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
 * Instances of the class {@code ArgumentList} represent a list of arguments in the invocation of a
 * executable element: a function, method, or constructor.
 * 
 * <pre>
 * argumentList ::=
 *     '(' arguments? ')'
 *
 * arguments ::=
 *     {@link NamedExpression namedArgument} (',' {@link NamedExpression namedArgument})*
 *   | {@link Expression expressionList} (',' {@link NamedExpression namedArgument})*
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ArgumentList extends AstNode {
  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The expressions producing the values of the arguments.
   */
  private NodeList<Expression> arguments = new NodeList<Expression>(this);

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * An array containing the elements representing the parameters corresponding to each of the
   * arguments in this list, or {@code null} if the AST has not been resolved or if the function or
   * method being invoked could not be determined based on static type information. The array must
   * be the same length as the number of arguments, but can contain {@code null} entries if a given
   * argument does not correspond to a formal parameter.
   */
  private ParameterElement[] correspondingStaticParameters;

  /**
   * An array containing the elements representing the parameters corresponding to each of the
   * arguments in this list, or {@code null} if the AST has not been resolved or if the function or
   * method being invoked could not be determined based on propagated type information. The array
   * must be the same length as the number of arguments, but can contain {@code null} entries if a
   * given argument does not correspond to a formal parameter.
   */
  private ParameterElement[] correspondingPropagatedParameters;

  /**
   * Initialize a newly created list of arguments.
   * 
   * @param leftParenthesis the left parenthesis
   * @param arguments the expressions producing the values of the arguments
   * @param rightParenthesis the right parenthesis
   */
  public ArgumentList(Token leftParenthesis, List<Expression> arguments, Token rightParenthesis) {
    this.leftParenthesis = leftParenthesis;
    this.arguments.addAll(arguments);
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitArgumentList(this);
  }

  /**
   * Return the expressions producing the values of the arguments. Although the language requires
   * that positional arguments appear before named arguments, this class allows them to be
   * intermixed.
   * 
   * @return the expressions producing the values of the arguments
   */
  public NodeList<Expression> getArguments() {
    return arguments;
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
   * Set the parameter elements corresponding to each of the arguments in this list to the given
   * array of parameters. The array of parameters must be the same length as the number of
   * arguments, but can contain {@code null} entries if a given argument does not correspond to a
   * formal parameter.
   * 
   * @param parameters the parameter elements corresponding to the arguments
   */
  public void setCorrespondingPropagatedParameters(ParameterElement[] parameters) {
    if (parameters.length != arguments.size()) {
      throw new IllegalArgumentException("Expected " + arguments.size() + " parameters, not "
          + parameters.length);
    }
    correspondingPropagatedParameters = parameters;
  }

  /**
   * Set the parameter elements corresponding to each of the arguments in this list to the given
   * array of parameters. The array of parameters must be the same length as the number of
   * arguments, but can contain {@code null} entries if a given argument does not correspond to a
   * formal parameter.
   * 
   * @param parameters the parameter elements corresponding to the arguments
   */
  public void setCorrespondingStaticParameters(ParameterElement[] parameters) {
    if (parameters.length != arguments.size()) {
      throw new IllegalArgumentException("Expected " + arguments.size() + " parameters, not "
          + parameters.length);
    }
    correspondingStaticParameters = parameters;
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

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    arguments.accept(visitor);
  }

  /**
   * If the given expression is a child of this list, and the AST structure has been resolved, and
   * the function being invoked is known based on propagated type information, and the expression
   * corresponds to one of the parameters of the function being invoked, then return the parameter
   * element representing the parameter to which the value of the given expression will be bound.
   * Otherwise, return {@code null}.
   * <p>
   * This method is only intended to be used by {@link Expression#getPropagatedParameterElement()}.
   * 
   * @param expression the expression corresponding to the parameter to be returned
   * @return the parameter element representing the parameter to which the value of the expression
   *         will be bound
   */
  protected ParameterElement getPropagatedParameterElementFor(Expression expression) {
    if (correspondingPropagatedParameters == null) {
      // Either the AST structure has not been resolved or the invocation of which this list is a
      // part could not be resolved.
      return null;
    }
    int index = arguments.indexOf(expression);
    if (index < 0) {
      // The expression isn't a child of this node.
      return null;
    }
    return correspondingPropagatedParameters[index];
  }

  /**
   * If the given expression is a child of this list, and the AST structure has been resolved, and
   * the function being invoked is known based on static type information, and the expression
   * corresponds to one of the parameters of the function being invoked, then return the parameter
   * element representing the parameter to which the value of the given expression will be bound.
   * Otherwise, return {@code null}.
   * <p>
   * This method is only intended to be used by {@link Expression#getStaticParameterElement()}.
   * 
   * @param expression the expression corresponding to the parameter to be returned
   * @return the parameter element representing the parameter to which the value of the expression
   *         will be bound
   */
  protected ParameterElement getStaticParameterElementFor(Expression expression) {
    if (correspondingStaticParameters == null) {
      // Either the AST structure has not been resolved or the invocation of which this list is a
      // part could not be resolved.
      return null;
    }
    int index = arguments.indexOf(expression);
    if (index < 0) {
      // The expression isn't a child of this node.
      return null;
    }
    return correspondingStaticParameters[index];
  }
}
