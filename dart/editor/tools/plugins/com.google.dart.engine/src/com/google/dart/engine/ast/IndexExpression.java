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
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

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
   * The element associated with the operator, or {@code null} if the AST structure has not been
   * resolved or if the operator could not be resolved.
   */
  private MethodElement element;

  /**
   * Initialize a newly created index expression.
   * 
   * @param target the expression used to compute the object being indexed
   * @param leftBracket the left square bracket
   * @param index the expression used to compute the index
   * @param rightBracket the right square bracket
   */
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
  public IndexExpression(Token period, Token leftBracket, Expression index, Token rightBracket) {
    this.period = period;
    this.leftBracket = leftBracket;
    this.index = becomeParentOf(index);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitIndexExpression(this);
  }

  /**
   * Return the expression used to compute the object being indexed, or {@code null} if this index
   * expression is part of a cascade expression.
   * 
   * @return the expression used to compute the object being indexed
   * @see #getRealTarget()
   */
  public Expression getArray() {
    return target;
  }

  @Override
  public Token getBeginToken() {
    if (target != null) {
      return target.getBeginToken();
    }
    return period;
  }

  /**
   * Return the element associated with the operator, or {@code null} if the AST structure has not
   * been resolved or if the operator could not be resolved. One example of the latter case is an
   * operator that is not defined for the type of the left-hand operand.
   * 
   * @return the element associated with this operator
   */
  public MethodElement getElement() {
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

  /**
   * Return the expression used to compute the object being indexed. If this index expression is not
   * part of a cascade expression, then this is the same as {@link #getArray()}. If this index
   * expression is part of a cascade expression, then the target expression stored with the cascade
   * expression is returned.
   * 
   * @return the expression used to compute the object being indexed
   * @see #getArray()
   */
  public Expression getRealTarget() {
    if (isCascaded()) {
      ASTNode ancestor = getParent();
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
   * Return {@code true} if this expression is computing a right-hand value.
   * <p>
   * Note that {@link #inGetterContext()} and {@link #inSetterContext()} are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return {@code true}
   * when invoked on the same node.
   * 
   * @return {@code true} if this expression is in a context where the operator '[]' will be invoked
   */
  public boolean inGetterContext() {
    ASTNode parent = getParent();
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
    ASTNode parent = getParent();
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
   * Set the expression used to compute the object being indexed to the given expression.
   * 
   * @param expression the expression used to compute the object being indexed
   */
  public void setArray(Expression expression) {
    target = becomeParentOf(expression);
  }

  /**
   * Set the element associated with the operator to the given element.
   * 
   * @param element the element associated with this operator
   */
  public void setElement(MethodElement element) {
    this.element = element;
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
   * Set the right square bracket to the given token.
   * 
   * @param bracket the right square bracket
   */
  public void setRightBracket(Token bracket) {
    rightBracket = bracket;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(target, visitor);
    safelyVisitChild(index, visitor);
  }
}
