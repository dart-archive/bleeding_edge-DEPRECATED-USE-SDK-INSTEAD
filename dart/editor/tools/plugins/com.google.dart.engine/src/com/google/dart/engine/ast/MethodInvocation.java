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
 * Instances of the class <code>MethodInvocation</code> represent the invocation of either a
 * function or a method. Invocations of getters and setters are represented by
 * {@link PropertyAccess property access} nodes.
 * 
 * <pre>
 * methodInvoction ::=
 *     ({@link Expression target} '.')? {@link SimpleIdentifier methodName} '(' argumentList? ')'
 *
 * argumentList:
 *     {@link NamedExpression namedArgument} (',' {@link NamedExpression namedArgument})*
 *   | {@link Expression expressionList} (',' {@link NamedExpression namedArgument})*
 * </pre>
 */
public class MethodInvocation extends Expression {
  /**
   * The expression producing the object on which the method is defined, or <code>null</code> if
   * there is no target (that is, the target is implicitly <code>this</code>).
   */
  private Expression target;

  /**
   * The period that separates the target from the method name, or <code>null</code> if there is no
   * target.
   */
  private Token period;

  /**
   * The name of the method being invoked.
   */
  private SimpleIdentifier methodName;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The expressions producing the values of the arguments to the method.
   */
  private NodeList<Expression> arguments = new NodeList<Expression>(this);

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * Initialize a newly created method invocation.
   */
  public MethodInvocation() {
  }

  /**
   * Initialize a newly created method invocation.
   * 
   * @param target the expression producing the object on which the method is defined
   * @param period the period that separates the target from the method name
   * @param methodName the name of the method being invoked
   * @param leftParenthesis the left parenthesis
   * @param arguments the expressions producing the values of the arguments to the method
   * @param rightParenthesis the right parenthesis
   */
  public MethodInvocation(Expression target, Token period, SimpleIdentifier methodName,
      Token leftParenthesis, List<Expression> arguments, Token rightParenthesis) {
    this.target = becomeParentOf(target);
    this.period = period;
    this.methodName = becomeParentOf(methodName);
    this.leftParenthesis = leftParenthesis;
    this.arguments.addAll(arguments);
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitMethodInvocation(this);
  }

  /**
   * Return the expressions producing the values of the arguments to the method.
   * 
   * @return the expressions producing the values of the arguments to the method
   */
  public NodeList<Expression> getArguments() {
    return arguments;
  }

  @Override
  public Token getBeginToken() {
    if (target != null) {
      return target.getBeginToken();
    }
    return methodName.getBeginToken();
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
   * Return the name of the method being invoked.
   * 
   * @return the name of the method being invoked
   */
  public SimpleIdentifier getMethodName() {
    return methodName;
  }

  /**
   * Return the period that separates the target from the method name, or <code>null</code> if there
   * is no target.
   * 
   * @return the period that separates the target from the method name
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
   * Return the expression producing the object on which the method is defined, or <code>null</code>
   * if there is no target (that is, the target is implicitly <code>this</code>).
   * 
   * @return the expression producing the object on which the method is defined
   */
  public Expression getTarget() {
    return target;
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
   * Set the name of the method being invoked to the given identifier.
   * 
   * @param identifier the name of the method being invoked
   */
  public void setMethodName(SimpleIdentifier identifier) {
    methodName = becomeParentOf(identifier);
  }

  /**
   * Set the period that separates the target from the method name to the given token.
   * 
   * @param period the period that separates the target from the method name
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
   * Set the expression producing the object on which the method is defined to the given expression.
   * 
   * @param expression the expression producing the object on which the method is defined
   */
  public void setTarget(Expression expression) {
    target = becomeParentOf(expression);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(target, visitor);
    safelyVisitChild(methodName, visitor);
    arguments.accept(visitor);
  }
}
