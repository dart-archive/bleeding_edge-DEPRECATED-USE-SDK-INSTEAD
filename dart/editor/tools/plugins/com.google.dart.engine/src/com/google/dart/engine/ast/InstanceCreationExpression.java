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
 * Instances of the class <code>InstanceCreationExpression</code> represent an instance creation
 * expression.
 * 
 * <pre>
 * newExpression ::=
 *     ('new' | 'const') {@link TypeName type} ('.' {@link SimpleIdentifier identifier})? '(' argumentList? ')'
 *
 * argumentList:
 *     {@link NamedExpression namedArgument} (',' {@link NamedExpression namedArgument})*
 *   | {@link Expression expressionList} (',' {@link NamedExpression namedArgument})*
 * </pre>
 */
public class InstanceCreationExpression extends Expression {
  /**
   * The keyword used to indicate how an object should be created.
   */
  private Token keyword;

  /**
   * The name of the type of the object to be created.
   */
  private TypeName type;

  /**
   * The period that separates the type from the constructor name, or <code>null</code> if the
   * unnamed constructor is to be invoked.
   */
  private Token period;

  /**
   * The name of the constructor to be invoked, or <code>null</code> if the unnamed constructor is
   * to be invoked.
   */
  private SimpleIdentifier identifier;

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
   * Initialize a newly created instance creation expression.
   */
  public InstanceCreationExpression() {
  }

  /**
   * Initialize a newly created instance creation expression.
   * 
   * @param keyword the keyword used to indicate how an object should be created
   * @param type the name of the type of the object to be created
   * @param period the period that separates the type from the constructor name
   * @param identifier the name of the constructor to be invoked
   * @param leftParenthesis the left parenthesis
   * @param arguments the expressions producing the values of the arguments to the constructor
   * @param rightParenthesis the right parenthesis
   */
  public InstanceCreationExpression(Token keyword, TypeName type, Token period,
      SimpleIdentifier identifier, Token leftParenthesis, List<Expression> arguments,
      Token rightParenthesis) {
    this.keyword = keyword;
    this.type = becomeParentOf(type);
    this.period = period;
    this.identifier = becomeParentOf(identifier);
    this.leftParenthesis = leftParenthesis;
    this.arguments.addAll(arguments);
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitInstanceCreationExpression(this);
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

  @Override
  public Token getEndToken() {
    return rightParenthesis;
  }

  /**
   * Return the name of the constructor to be invoked, or <code>null</code> if the unnamed
   * constructor is to be invoked.
   * 
   * @return the name of the constructor to be invoked
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * Return the keyword used to indicate how an object should be created.
   * 
   * @return the keyword used to indicate how an object should be created
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
   * Return the period that separates the type from the constructor name, or <code>null</code> if
   * the unnamed constructor is to be invoked.
   * 
   * @return the period that separates the type from the constructor name
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
   * Return the name of the type of the object to be created.
   * 
   * @return the name of the type of the object to be created
   */
  public TypeName getType() {
    return type;
  }

  /**
   * Set the name of the constructor to be invoked to the given identifier.
   * 
   * @param identifier the name of the constructor to be invoked
   */
  public void setIdentifier(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }

  /**
   * Set the keyword used to indicate how an object should be created to the given keyword.
   * 
   * @param keyword the keyword used to indicate how an object should be created
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
   * Set the period that separates the type from the constructor name to the given token.
   * 
   * @param period the period that separates the type from the constructor name
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

  /**
   * Set the name of the type of the object to be created to the given type name.
   * 
   * @param typeName the name of the type of the object to be created
   */
  public void setType(TypeName typeName) {
    type = becomeParentOf(typeName);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(type, visitor);
    safelyVisitChild(identifier, visitor);
    arguments.accept(visitor);
  }
}
