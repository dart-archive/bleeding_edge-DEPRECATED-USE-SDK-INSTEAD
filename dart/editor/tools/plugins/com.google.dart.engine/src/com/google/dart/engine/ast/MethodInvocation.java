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
import com.google.dart.engine.scanner.TokenType;

/**
 * Instances of the class {@code MethodInvocation} represent the invocation of either a function or
 * a method. Invocations of functions resulting from evaluating an expression are represented by
 * {@link FunctionExpressionInvocation function expression invocation} nodes. Invocations of getters
 * and setters are represented by either {@link PrefixedIdentifier prefixed identifier} or
 * {@link PropertyAccess property access} nodes.
 * 
 * <pre>
 * methodInvoction ::=
 *     ({@link Expression target} '.')? {@link SimpleIdentifier methodName} {@link ArgumentList argumentList}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class MethodInvocation extends Expression {
  /**
   * The expression producing the object on which the method is defined, or {@code null} if there is
   * no target (that is, the target is implicitly {@code this}).
   */
  private Expression target;

  /**
   * The period that separates the target from the method name, or {@code null} if there is no
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
  public <R> R accept(AstVisitor<R> visitor) {
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
    } else if (period != null) {
      return period;
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
   * Return the period that separates the target from the method name, or {@code null} if there is
   * no target.
   * 
   * @return the period that separates the target from the method name
   */
  public Token getPeriod() {
    return period;
  }

  @Override
  public int getPrecedence() {
    return 15;
  }

  /**
   * Return the expression used to compute the receiver of the invocation. If this invocation is not
   * part of a cascade expression, then this is the same as {@link #getTarget()}. If this invocation
   * is part of a cascade expression, then the target stored with the cascade expression is
   * returned.
   * 
   * @return the expression used to compute the receiver of the invocation
   * @see #getTarget()
   */
  public Expression getRealTarget() {
    if (isCascaded()) {
      AstNode ancestor = getParent();
      while (!(ancestor instanceof CascadeExpression)) {
        if (ancestor == null) {
          return target;
        }
        ancestor = ancestor.getParent();
      }
      return ((CascadeExpression) ancestor).getTarget();
    }
    return target;
  }

  /**
   * Return the expression producing the object on which the method is defined, or {@code null} if
   * there is no target (that is, the target is implicitly {@code this}) or if this method
   * invocation is part of a cascade expression.
   * 
   * @return the expression producing the object on which the method is defined
   * @see #getRealTarget()
   */
  public Expression getTarget() {
    return target;
  }

  /**
   * Return {@code true} if this expression is cascaded. If it is, then the target of this
   * expression is not stored locally but is stored in the nearest ancestor that is a
   * {@link CascadeExpression}.
   * 
   * @return {@code true} if this expression is cascaded
   */
  public boolean isCascaded() {
    return period != null && period.getType() == TokenType.PERIOD_PERIOD;
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
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(target, visitor);
    safelyVisitChild(methodName, visitor);
    safelyVisitChild(argumentList, visitor);
  }
}
