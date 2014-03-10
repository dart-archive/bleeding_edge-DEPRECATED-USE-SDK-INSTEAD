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

import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.internal.element.AuxiliaryElements;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code IndexExpression} represent an index expression.
 * 
 * <pre>
 * indexExpression ::=
 *     {@link Expression target} '[' {@link Expression index} ']'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class IndexExpression extends Expression {
  /**
   * The expression used to compute the object being indexed, or {@code null} if this index
   * expression is part of a cascade expression.
   */
  private Expression target;

  /**
   * The period ("..") before a cascaded index expression, or {@code null} if this index expression
   * is not part of a cascade expression.
   */
  private Token period;

  /**
   * The left square bracket.
   */
  private Token leftBracket;

  /**
   * The expression used to compute the index.
   */
  private Expression index;

  /**
   * The right square bracket.
   */
  private Token rightBracket;

  /**
   * The element associated with the operator based on the static type of the target, or
   * {@code null} if the AST structure has not been resolved or if the operator could not be
   * resolved.
   */
  private MethodElement staticElement;

  /**
   * The element associated with the operator based on the propagated type of the target, or
   * {@code null} if the AST structure has not been resolved or if the operator could not be
   * resolved.
   */
  private MethodElement propagatedElement;

  /**
   * If this expression is both in a getter and setter context, the {@link AuxiliaryElements} will
   * be set to hold onto the static and propagated information. The auxiliary element will hold onto
   * the elements from the getter context.
   */
  private AuxiliaryElements auxiliaryElements = null;

  /**
   * Initialize a newly created index expression.
   * 
   * @param target the expression used to compute the object being indexed
   * @param leftBracket the left square bracket
   * @param index the expression used to compute the index
   * @param rightBracket the right square bracket
   */
  @DartName("forTarget")
  public IndexExpression(Expression target, Token leftBracket, Expression index, Token rightBracket) {
    this.target = becomeParentOf(target);
    this.leftBracket = leftBracket;
    this.index = becomeParentOf(index);
    this.rightBracket = rightBracket;
  }

  /**
   * Initialize a newly created index expression.
   * 
   * @param period the period ("..") before a cascaded index expression
   * @param leftBracket the left square bracket
   * @param index the expression used to compute the index
   * @param rightBracket the right square bracket
   */
  @DartName("forCascade")
  public IndexExpression(Token period, Token leftBracket, Expression index, Token rightBracket) {
    this.period = period;
    this.leftBracket = leftBracket;
    this.index = becomeParentOf(index);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitIndexExpression(this);
  }

  /**
   * Get the auxiliary elements, this will be {@code null} if the node is not in a getter and setter
   * context, or if it is not yet fully resolved.
   */
  public AuxiliaryElements getAuxiliaryElements() {
    return auxiliaryElements;
  }

  @Override
  public Token getBeginToken() {
    if (target != null) {
      return target.getBeginToken();
    }
    return period;
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
    return rightBracket;
  }

  /**
   * Return the expression used to compute the index.
   * 
   * @return the expression used to compute the index
   */
  public Expression getIndex() {
    return index;
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
   * Return the period ("..") before a cascaded index expression, or {@code null} if this index
   * expression is not part of a cascade expression.
   * 
   * @return the period ("..") before a cascaded index expression
   */
  public Token getPeriod() {
    return period;
  }

  @Override
  public int getPrecedence() {
    return 15;
  }

  /**
   * Return the element associated with the operator based on the propagated type of the target, or
   * {@code null} if the AST structure has not been resolved or if the operator could not be
   * resolved. One example of the latter case is an operator that is not defined for the type of the
   * target.
   * 
   * @return the element associated with this operator
   */
  public MethodElement getPropagatedElement() {
    return propagatedElement;
  }

  /**
   * Return the expression used to compute the object being indexed. If this index expression is not
   * part of a cascade expression, then this is the same as {@link #getTarget()}. If this index
   * expression is part of a cascade expression, then the target expression stored with the cascade
   * expression is returned.
   * 
   * @return the expression used to compute the object being indexed
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
   * Return the right square bracket.
   * 
   * @return the right square bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Return the element associated with the operator based on the static type of the target, or
   * {@code null} if the AST structure has not been resolved or if the operator could not be
   * resolved. One example of the latter case is an operator that is not defined for the type of the
   * target.
   * 
   * @return the element associated with the operator
   */
  public MethodElement getStaticElement() {
    return staticElement;
  }

  /**
   * Return the expression used to compute the object being indexed, or {@code null} if this index
   * expression is part of a cascade expression.
   * 
   * @return the expression used to compute the object being indexed
   * @see #getRealTarget()
   */
  public Expression getTarget() {
    return target;
  }

  /**
   * Return {@code true} if this expression is computing a right-hand value.
   * <p>
   * Note that {@link #inGetterContext()} and {@link #inSetterContext()} are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return {@code true}
   * when invoked on the same node.
   * 
   * @return {@code true} if this expression is in a context where the operator '[]' will be invoked
   */
  public boolean inGetterContext() {
    AstNode parent = getParent();
    if (parent instanceof AssignmentExpression) {
      AssignmentExpression assignment = (AssignmentExpression) parent;
      if (assignment.getLeftHandSide() == this
          && assignment.getOperator().getType() == TokenType.EQ) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return {@code true} if this expression is computing a left-hand value.
   * <p>
   * Note that {@link #inGetterContext()} and {@link #inSetterContext()} are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return {@code true}
   * when invoked on the same node.
   * 
   * @return {@code true} if this expression is in a context where the operator '[]=' will be
   *         invoked
   */
  public boolean inSetterContext() {
    AstNode parent = getParent();
    if (parent instanceof PrefixExpression) {
      return ((PrefixExpression) parent).getOperator().getType().isIncrementOperator();
    } else if (parent instanceof PostfixExpression) {
      return true;
    } else if (parent instanceof AssignmentExpression) {
      return ((AssignmentExpression) parent).getLeftHandSide() == this;
    }
    return false;
  }

  @Override
  public boolean isAssignable() {
    return true;
  }

  /**
   * Return {@code true} if this expression is cascaded. If it is, then the target of this
   * expression is not stored locally but is stored in the nearest ancestor that is a
   * {@link CascadeExpression}.
   * 
   * @return {@code true} if this expression is cascaded
   */
  public boolean isCascaded() {
    return period != null;
  }

  /**
   * Set the auxiliary elements.
   */
  public void setAuxiliaryElements(AuxiliaryElements auxiliaryElements) {
    this.auxiliaryElements = auxiliaryElements;
  }

  /**
   * Set the expression used to compute the index to the given expression.
   * 
   * @param expression the expression used to compute the index
   */
  public void setIndex(Expression expression) {
    index = becomeParentOf(expression);
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
   * Set the period ("..") before a cascaded index expression to the given token.
   * 
   * @param period the period ("..") before a cascaded index expression
   */
  public void setPeriod(Token period) {
    this.period = period;
  }

  /**
   * Set the element associated with the operator based on the propagated type of the target to the
   * given element.
   * 
   * @param element the element to be associated with this operator
   */
  public void setPropagatedElement(MethodElement element) {
    propagatedElement = element;
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
   * Set the element associated with the operator based on the static type of the target to the
   * given element.
   * 
   * @param element the static element to be associated with the operator
   */
  public void setStaticElement(MethodElement element) {
    staticElement = element;
  }

  /**
   * Set the expression used to compute the object being indexed to the given expression.
   * 
   * @param expression the expression used to compute the object being indexed
   */
  public void setTarget(Expression expression) {
    target = becomeParentOf(expression);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(target, visitor);
    safelyVisitChild(index, visitor);
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on
   * propagated type information, then return the parameter element representing the parameter to
   * which the value of the index expression will be bound. Otherwise, return {@code null}.
   * <p>
   * This method is only intended to be used by {@link Expression#getPropagatedParameterElement()}.
   * 
   * @return the parameter element representing the parameter to which the value of the index
   *         expression will be bound
   */
  protected ParameterElement getPropagatedParameterElementForIndex() {
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
   * value of the index expression will be bound. Otherwise, return {@code null}.
   * <p>
   * This method is only intended to be used by {@link Expression#getStaticParameterElement()}.
   * 
   * @return the parameter element representing the parameter to which the value of the index
   *         expression will be bound
   */
  protected ParameterElement getStaticParameterElementForIndex() {
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
