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

import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code Expression} defines the behavior common to nodes that represent an
 * expression.
 * 
 * <pre>
 * expression ::=
 *     {@link AssignmentExpression assignmentExpression}
 *   | {@link ConditionalExpression conditionalExpression} cascadeSection*
 *   | {@link ThrowExpression throwExpression}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public abstract class Expression extends AstNode {
  /**
   * An empty array of expressions.
   */
  public static final Expression[] EMPTY_ARRAY = new Expression[0];

  /**
   * The static type of this expression, or {@code null} if the AST structure has not been resolved.
   */
  private Type staticType;

  /**
   * The propagated type of this expression, or {@code null} if type propagation has not been
   * performed on the AST structure.
   */
  private Type propagatedType;

  /**
   * Return the best parameter element information available for this expression. If type
   * propagation was able to find a better parameter element than static analysis, that type will be
   * returned. Otherwise, the result of static analysis will be returned.
   * 
   * @return the parameter element representing the parameter to which the value of this expression
   *         will be bound
   */
  public ParameterElement getBestParameterElement() {
    ParameterElement propagatedElement = getPropagatedParameterElement();
    if (propagatedElement != null) {
      return propagatedElement;
    }
    return getStaticParameterElement();
  }

  /**
   * Return the best type information available for this expression. If type propagation was able to
   * find a better type than static analysis, that type will be returned. Otherwise, the result of
   * static analysis will be returned. If no type analysis has been performed, then the type
   * 'dynamic' will be returned.
   * 
   * @return the best type information available for this expression
   */
  public Type getBestType() {
    if (propagatedType != null) {
      return propagatedType;
    } else if (staticType != null) {
      return staticType;
    }
    return DynamicTypeImpl.getInstance();
  }

  /**
   * Return the precedence of this expression. The precedence is a positive integer value that
   * defines how the source code is parsed into an AST. For example {@code a * b + c} is parsed as
   * {@code (a * b) + c} because the precedence of {@code *} is greater than the precedence of
   * {@code +}.
   * <p>
   * You should not assume that returned values will stay the same, they might change as result of
   * specification change. Only relative order should be used.
   * 
   * @return the precedence of this expression
   */
  public abstract int getPrecedence();

  /**
   * If this expression is an argument to an invocation, and the AST structure has been resolved,
   * and the function being invoked is known based on propagated type information, and this
   * expression corresponds to one of the parameters of the function being invoked, then return the
   * parameter element representing the parameter to which the value of this expression will be
   * bound. Otherwise, return {@code null}.
   * 
   * @return the parameter element representing the parameter to which the value of this expression
   *         will be bound
   */
  public ParameterElement getPropagatedParameterElement() {
    AstNode parent = getParent();
    if (parent instanceof ArgumentList) {
      return ((ArgumentList) parent).getPropagatedParameterElementFor(this);
    } else if (parent instanceof IndexExpression) {
      IndexExpression indexExpression = (IndexExpression) parent;
      if (indexExpression.getIndex() == this) {
        return indexExpression.getPropagatedParameterElementForIndex();
      }
    } else if (parent instanceof BinaryExpression) {
      BinaryExpression binaryExpression = (BinaryExpression) parent;
      if (binaryExpression.getRightOperand() == this) {
        return binaryExpression.getPropagatedParameterElementForRightOperand();
      }
    } else if (parent instanceof AssignmentExpression) {
      AssignmentExpression assignmentExpression = (AssignmentExpression) parent;
      if (assignmentExpression.getRightHandSide() == this) {
        return assignmentExpression.getPropagatedParameterElementForRightHandSide();
      }
    } else if (parent instanceof PrefixExpression) {
      return ((PrefixExpression) parent).getPropagatedParameterElementForOperand();
    } else if (parent instanceof PostfixExpression) {
      return ((PostfixExpression) parent).getPropagatedParameterElementForOperand();
    }
    return null;
  }

  /**
   * Return the propagated type of this expression, or {@code null} if type propagation has not been
   * performed on the AST structure.
   * 
   * @return the propagated type of this expression
   */
  public Type getPropagatedType() {
    return propagatedType;
  }

  /**
   * If this expression is an argument to an invocation, and the AST structure has been resolved,
   * and the function being invoked is known based on static type information, and this expression
   * corresponds to one of the parameters of the function being invoked, then return the parameter
   * element representing the parameter to which the value of this expression will be bound.
   * Otherwise, return {@code null}.
   * 
   * @return the parameter element representing the parameter to which the value of this expression
   *         will be bound
   */
  public ParameterElement getStaticParameterElement() {
    AstNode parent = getParent();
    if (parent instanceof ArgumentList) {
      return ((ArgumentList) parent).getStaticParameterElementFor(this);
    } else if (parent instanceof IndexExpression) {
      IndexExpression indexExpression = (IndexExpression) parent;
      if (indexExpression.getIndex() == this) {
        return indexExpression.getStaticParameterElementForIndex();
      }
    } else if (parent instanceof BinaryExpression) {
      BinaryExpression binaryExpression = (BinaryExpression) parent;
      if (binaryExpression.getRightOperand() == this) {
        return binaryExpression.getStaticParameterElementForRightOperand();
      }
    } else if (parent instanceof AssignmentExpression) {
      AssignmentExpression assignmentExpression = (AssignmentExpression) parent;
      if (assignmentExpression.getRightHandSide() == this) {
        return assignmentExpression.getStaticParameterElementForRightHandSide();
      }
    } else if (parent instanceof PrefixExpression) {
      return ((PrefixExpression) parent).getStaticParameterElementForOperand();
    } else if (parent instanceof PostfixExpression) {
      return ((PostfixExpression) parent).getStaticParameterElementForOperand();
    }
    return null;
  }

  /**
   * Return the static type of this expression, or {@code null} if the AST structure has not been
   * resolved.
   * 
   * @return the static type of this expression
   */
  public Type getStaticType() {
    return staticType;
  }

  /**
   * Return {@code true} if this expression is syntactically valid for the LHS of an
   * {@link AssignmentExpression assignment expression}.
   * 
   * @return {@code true} if this expression matches the {@code assignableExpression} production
   */
  public boolean isAssignable() {
    return false;
  }

  /**
   * Set the propagated type of this expression to the given type.
   * 
   * @param propagatedType the propagated type of this expression
   */
  public void setPropagatedType(Type propagatedType) {
    this.propagatedType = propagatedType;
  }

  /**
   * Set the static type of this expression to the given type.
   * 
   * @param staticType the static type of this expression
   */
  public void setStaticType(Type staticType) {
    this.staticType = staticType;
  }
}
