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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code AssignmentExpression} represent an assignment expression.
 * 
 * <pre>
 * assignmentExpression ::=
 *     {@link Expression leftHandSide} {@link Token operator} {@link Expression rightHandSide}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class AssignmentExpression extends Expression {
  /**
   * The expression used to compute the left hand side.
   */
  private Expression leftHandSide;

  /**
   * The assignment operator being applied.
   */
  private Token operator;

  /**
   * The expression used to compute the right hand side.
   */
  private Expression rightHandSide;

  /**
   * The element associated with the operator based on the static type of the left-hand-side, or
   * {@code null} if the AST structure has not been resolved, if the operator is not a compound
   * operator, or if the operator could not be resolved.
   */
  private MethodElement staticElement;

  /**
   * The element associated with the operator based on the propagated type of the left-hand-side, or
   * {@code null} if the AST structure has not been resolved, if the operator is not a compound
   * operator, or if the operator could not be resolved.
   */
  private MethodElement propagatedElement;

  /**
   * Initialize a newly created assignment expression.
   * 
   * @param leftHandSide the expression used to compute the left hand side
   * @param operator the assignment operator being applied
   * @param rightHandSide the expression used to compute the right hand side
   */
  public AssignmentExpression(Expression leftHandSide, Token operator, Expression rightHandSide) {
    if (leftHandSide == null || rightHandSide == null) {
      String message;
      if (leftHandSide == null) {
        if (rightHandSide == null) {
          message = "Both the left-hand and right-hand sides are null";
        } else {
          message = "The left-hand size is null";
        }
      } else {
        message = "The right-hand size is null";
      }
      AnalysisEngine.getInstance().getLogger().logError(message, new Exception(message));
    }
    this.leftHandSide = becomeParentOf(leftHandSide);
    this.operator = operator;
    this.rightHandSide = becomeParentOf(rightHandSide);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitAssignmentExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return leftHandSide.getBeginToken();
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
    return rightHandSide.getEndToken();
  }

  /**
   * Set the expression used to compute the left hand side to the given expression.
   * 
   * @return the expression used to compute the left hand side
   */
  public Expression getLeftHandSide() {
    return leftHandSide;
  }

  /**
   * Return the assignment operator being applied.
   * 
   * @return the assignment operator being applied
   */
  public Token getOperator() {
    return operator;
  }

  @Override
  public int getPrecedence() {
    return 1;
  }

  /**
   * Return the element associated with the operator based on the propagated type of the
   * left-hand-side, or {@code null} if the AST structure has not been resolved, if the operator is
   * not a compound operator, or if the operator could not be resolved. One example of the latter
   * case is an operator that is not defined for the type of the left-hand operand.
   * 
   * @return the element associated with the operator
   */
  public MethodElement getPropagatedElement() {
    return propagatedElement;
  }

  /**
   * Return the expression used to compute the right hand side.
   * 
   * @return the expression used to compute the right hand side
   */
  public Expression getRightHandSide() {
    return rightHandSide;
  }

  /**
   * Return the element associated with the operator based on the static type of the left-hand-side,
   * or {@code null} if the AST structure has not been resolved, if the operator is not a compound
   * operator, or if the operator could not be resolved. One example of the latter case is an
   * operator that is not defined for the type of the left-hand operand.
   * 
   * @return the element associated with the operator
   */
  public MethodElement getStaticElement() {
    return staticElement;
  }

  /**
   * Return the expression used to compute the left hand side.
   * 
   * @param expression the expression used to compute the left hand side
   */
  public void setLeftHandSide(Expression expression) {
    leftHandSide = becomeParentOf(expression);
  }

  /**
   * Set the assignment operator being applied to the given operator.
   * 
   * @param operator the assignment operator being applied
   */
  public void setOperator(Token operator) {
    this.operator = operator;
  }

  /**
   * Set the element associated with the operator based on the propagated type of the left-hand-side
   * to the given element.
   * 
   * @param element the element to be associated with the operator
   */
  public void setPropagatedElement(MethodElement element) {
    propagatedElement = element;
  }

  /**
   * Set the expression used to compute the left hand side to the given expression.
   * 
   * @param expression the expression used to compute the left hand side
   */
  public void setRightHandSide(Expression expression) {
    rightHandSide = becomeParentOf(expression);
  }

  /**
   * Set the element associated with the operator based on the static type of the left-hand-side to
   * the given element.
   * 
   * @param element the static element to be associated with the operator
   */
  public void setStaticElement(MethodElement element) {
    staticElement = element;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(leftHandSide, visitor);
    safelyVisitChild(rightHandSide, visitor);
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on
   * propagated type information, then return the parameter element representing the parameter to
   * which the value of the right operand will be bound. Otherwise, return {@code null}.
   * <p>
   * This method is only intended to be used by {@link Expression#getPropagatedParameterElement()}.
   * 
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  protected ParameterElement getPropagatedParameterElementForRightHandSide() {
    ExecutableElement executableElement = null;
    if (propagatedElement != null) {
      executableElement = propagatedElement;
    } else {
      if (leftHandSide instanceof Identifier) {
        Identifier identifier = (Identifier) leftHandSide;
        Element leftElement = identifier.getPropagatedElement();
        if (leftElement instanceof ExecutableElement) {
          executableElement = (ExecutableElement) leftElement;
        }
      }
      if (leftHandSide instanceof PropertyAccess) {
        SimpleIdentifier identifier = ((PropertyAccess) leftHandSide).getPropertyName();
        Element leftElement = identifier.getPropagatedElement();
        if (leftElement instanceof ExecutableElement) {
          executableElement = (ExecutableElement) leftElement;
        }
      }
    }
    if (executableElement == null) {
      return null;
    }
    ParameterElement[] parameters = executableElement.getParameters();
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on static
   * type information, then return the parameter element representing the parameter to which the
   * value of the right operand will be bound. Otherwise, return {@code null}.
   * <p>
   * This method is only intended to be used by {@link Expression#getStaticParameterElement()}.
   * 
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  protected ParameterElement getStaticParameterElementForRightHandSide() {
    ExecutableElement executableElement = null;
    if (staticElement != null) {
      executableElement = staticElement;
    } else {
      if (leftHandSide instanceof Identifier) {
        Element leftElement = ((Identifier) leftHandSide).getStaticElement();
        if (leftElement instanceof ExecutableElement) {
          executableElement = (ExecutableElement) leftElement;
        }
      }
      if (leftHandSide instanceof PropertyAccess) {
        Element leftElement = ((PropertyAccess) leftHandSide).getPropertyName().getStaticElement();
        if (leftElement instanceof ExecutableElement) {
          executableElement = (ExecutableElement) leftElement;
        }
      }
    }
    if (executableElement == null) {
      return null;
    }
    ParameterElement[] parameters = executableElement.getParameters();
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }
}
