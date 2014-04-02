/*
 * Copyright 2011, the Dart project authors.
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

import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code PrefixExpression} represent a prefix unary expression.
 * 
 * <pre>
 * prefixExpression ::=
 *     {@link Token operator} {@link Expression operand}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class PrefixExpression extends Expression {
  /**
   * The prefix operator being applied to the operand.
   */
  private Token operator;

  /**
   * The expression computing the operand for the operator.
   */
  private Expression operand;

  /**
   * The element associated with the operator based on the static type of the operand, or
   * {@code null} if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved.
   */
  private MethodElement staticElement;

  /**
   * The element associated with the operator based on the propagated type of the operand, or
   * {@code null} if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved.
   */
  private MethodElement propagatedElement;

  /**
   * Initialize a newly created prefix expression.
   * 
   * @param operator the prefix operator being applied to the operand
   * @param operand the expression computing the operand for the operator
   */
  public PrefixExpression(Token operator, Expression operand) {
    this.operator = operator;
    this.operand = becomeParentOf(operand);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitPrefixExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return operator;
  }

  /**
   * Return the best element available for this operator. If resolution was able to find a better
   * element based on type propagation, that element will be returned. Otherwise, the element found
   * using the result of static analysis will be returned. If resolution has not been performed,
   * then {@code null} will be returned.
   * 
   * @return the best element available for this operator
   */
  public MethodElement getBestElement() {
    MethodElement element = getPropagatedElement();
    if (element == null) {
      element = getStaticElement();
    }
    return element;
  }

  @Override
  public Token getEndToken() {
    return operand.getEndToken();
  }

  /**
   * Return the expression computing the operand for the operator.
   * 
   * @return the expression computing the operand for the operator
   */
  public Expression getOperand() {
    return operand;
  }

  /**
   * Return the prefix operator being applied to the operand.
   * 
   * @return the prefix operator being applied to the operand
   */
  public Token getOperator() {
    return operator;
  }

  @Override
  public int getPrecedence() {
    return 14;
  }

  /**
   * Return the element associated with the operator based on the propagated type of the operand, or
   * {@code null} if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved. One example of the latter case is an operator that is
   * not defined for the type of the operand.
   * 
   * @return the element associated with the operator
   */
  public MethodElement getPropagatedElement() {
    return propagatedElement;
  }

  /**
   * Return the element associated with the operator based on the static type of the operand, or
   * {@code null} if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved. One example of the latter case is an operator that is
   * not defined for the type of the operand.
   * 
   * @return the element associated with the operator
   */
  public MethodElement getStaticElement() {
    return staticElement;
  }

  /**
   * Set the expression computing the operand for the operator to the given expression.
   * 
   * @param expression the expression computing the operand for the operator
   */
  public void setOperand(Expression expression) {
    operand = becomeParentOf(expression);
  }

  /**
   * Set the prefix operator being applied to the operand to the given operator.
   * 
   * @param operator the prefix operator being applied to the operand
   */
  public void setOperator(Token operator) {
    this.operator = operator;
  }

  /**
   * Set the element associated with the operator based on the propagated type of the operand to the
   * given element.
   * 
   * @param element the element to be associated with the operator
   */
  public void setPropagatedElement(MethodElement element) {
    propagatedElement = element;
  }

  /**
   * Set the element associated with the operator based on the static type of the operand to the
   * given element.
   * 
   * @param element the static element to be associated with the operator
   */
  public void setStaticElement(MethodElement element) {
    staticElement = element;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(operand, visitor);
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on
   * propagated type information, then return the parameter element representing the parameter to
   * which the value of the operand will be bound. Otherwise, return {@code null}.
   * <p>
   * This method is only intended to be used by {@link Expression#getPropagatedParameterElement()}.
   * 
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  protected ParameterElement getPropagatedParameterElementForOperand() {
    if (propagatedElement == null) {
      return null;
    }
    ParameterElement[] parameters = propagatedElement.getParameters();
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on static
   * type information, then return the parameter element representing the parameter to which the
   * value of the operand will be bound. Otherwise, return {@code null}.
   * <p>
   * This method is only intended to be used by {@link Expression#getStaticParameterElement()}.
   * 
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  protected ParameterElement getStaticParameterElementForOperand() {
    if (staticElement == null) {
      return null;
    }
    ParameterElement[] parameters = staticElement.getParameters();
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }
}
