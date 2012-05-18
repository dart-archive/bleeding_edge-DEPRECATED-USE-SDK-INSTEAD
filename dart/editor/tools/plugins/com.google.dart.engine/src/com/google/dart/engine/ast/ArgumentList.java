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
 * Instances of the class {@code ArgumentList} represent a list of arguments in the invocation of a
 * executable element: a function, method, or constructor.
 * 
 * <pre>
 * argumentList ::=
 *     '(' arguments? ')'
 *
 * arguments:
 *     {@link NamedExpression namedArgument} (',' {@link NamedExpression namedArgument})*
 *   | {@link Expression expressionList} (',' {@link NamedExpression namedArgument})*
 * </pre>
 */
public class ArgumentList extends ASTNode {
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
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitArgumentList(this);
  }

  /**
   * Return the expressions producing the values of the arguments.
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
  public void visitChildren(ASTVisitor<?> visitor) {
    arguments.accept(visitor);
  }
}
