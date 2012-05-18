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
 * Instances of the class <code>MethodInvocation</code> represent the invocation of either a
 * function or a method. Invocations of functions resulting from evaluating an expression are
 * represented by {@link FunctionExpressionInvocation function expression invocation} nodes.
 * Invocations of getters and setters are represented by either {@link PrefixedIdentifier prefixed
 * identifier} or {@link PropertyAccess property access} nodes.
 * 
 * <pre>
 * methodInvoction ::=
 *     ({@link Expression target} '.')? {@link SimpleIdentifier methodName} {@link ArgumentList argumentList}
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
   * The list of arguments to the method.
   */
  private ArgumentList argumentList;

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
   * @param argumentList the list of arguments to the method
   */
  public MethodInvocation(Expression target, Token period, SimpleIdentifier methodName,
      ArgumentList argumentList) {
    this.target = becomeParentOf(target);
    this.period = period;
    this.methodName = becomeParentOf(methodName);
    this.argumentList = becomeParentOf(argumentList);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitMethodInvocation(this);
  }

  /**
   * Return the list of arguments to the method.
   * 
   * @return the list of arguments to the method
   */
  public ArgumentList getArgumentList() {
    return argumentList;
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
    return argumentList.getEndToken();
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
   * Return the expression producing the object on which the method is defined, or <code>null</code>
   * if there is no target (that is, the target is implicitly <code>this</code>).
   * 
   * @return the expression producing the object on which the method is defined
   */
  public Expression getTarget() {
    return target;
  }

  /**
   * Set the list of arguments to the method to the given list.
   * 
   * @param argumentList the list of arguments to the method
   */
  public void setArgumentList(ArgumentList argumentList) {
    this.argumentList = becomeParentOf(argumentList);
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
    safelyVisitChild(argumentList, visitor);
  }
}
