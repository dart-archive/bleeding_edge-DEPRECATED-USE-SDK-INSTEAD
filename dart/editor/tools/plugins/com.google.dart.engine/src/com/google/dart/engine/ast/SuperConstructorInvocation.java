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
 * Instances of the class <code>SuperConstructorInvocation</code> represent the invocation of a
 * superclass' constructor from within a constructor's initialization list.
 * 
 * <pre>
 * superInvocation ::=
 *     'super' ('.' {@link SimpleIdentifier name})? arguments
 * </pre>
 */
public class SuperConstructorInvocation extends ConstructorInitializer {
  /**
   * The token for the 'super' keyword.
   */
  private Token keyword;

  /**
   * The token for the period before the name of the constructor that is being invoked, or
   * <code>null</code> if the unnamed constructor is being invoked.
   */
  private Token period;

  /**
   * The name of the constructor that is being invoked, or <code>null</code> if the unnamed
   * constructor is being invoked.
   */
  private SimpleIdentifier constructorName;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The expressions producing the values of the arguments to the constructor.
   */
  private NodeList<Expression> arguments = new NodeList<Expression>(this);

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * Initialize a newly created super invocation to invoke the inherited constructor with the given
   * name with the given arguments.
   */
  public SuperConstructorInvocation() {
  }

  /**
   * Initialize a newly created super invocation to invoke the inherited constructor with the given
   * name with the given arguments.
   * 
   * @param keyword the token for the 'super' keyword
   * @param period the token for the period before the name of the constructor that is being invoked
   * @param constructorName the name of the constructor that is being invoked
   * @param leftParenthesis the left parenthesis
   * @param arguments the expressions producing the values of the arguments to the constructor
   * @param rightParenthesis the right parenthesis
   */
  public SuperConstructorInvocation(Token keyword, Token period, SimpleIdentifier constructorName,
      Token leftParenthesis, List<Expression> arguments, Token rightParenthesis) {
    this.keyword = keyword;
    this.period = period;
    this.constructorName = becomeParentOf(constructorName);
    this.leftParenthesis = leftParenthesis;
    this.arguments.addAll(arguments);
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSuperConstructorInvocation(this);
  }

  /**
   * Return the expressions producing the values of the arguments to the constructor.
   * 
   * @return the expressions producing the values of the arguments to the constructor
   */
  public NodeList<Expression> getArguments() {
    return arguments;
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  /**
   * Return the name of the constructor that is being invoked, or <code>null</code> if the unnamed
   * constructor is being invoked.
   * 
   * @return the name of the constructor that is being invoked
   */
  public SimpleIdentifier getConstructorName() {
    return constructorName;
  }

  @Override
  public Token getEndToken() {
    return rightParenthesis;
  }

  /**
   * Return the token for the 'super' keyword.
   * 
   * @return the token for the 'super' keyword
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
   * Return the token for the period before the name of the constructor that is being invoked, or
   * <code>null</code> if the unnamed constructor is being invoked.
   * 
   * @return the token for the period before the name of the constructor that is being invoked
   */
  public Token getPeriod() {
    return period;
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
   * Set the name of the constructor that is being invoked to the given identifier.
   * 
   * @param identifier the name of the constructor that is being invoked
   */
  public void setConstructorName(SimpleIdentifier identifier) {
    constructorName = becomeParentOf(identifier);
  }

  /**
   * Set the token for the 'super' keyword to the given token.
   * 
   * @param keyword the token for the 'super' keyword
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
   * Set the token for the period before the name of the constructor that is being invoked to the
   * given token.
   * 
   * @param period the token for the period before the name of the constructor that is being invoked
   */
  public void setPeriod(Token period) {
    this.period = period;
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
    safelyVisitChild(constructorName, visitor);
    arguments.accept(visitor);
  }
}
