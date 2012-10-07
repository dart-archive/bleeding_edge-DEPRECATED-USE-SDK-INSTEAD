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
 * Instances of the class {@code PropertyAccess} represent the access of a property of an object.
 * <p>
 * Note, however, that accesses to properties of objects can also be represented as
 * {@link PrefixedIdentifier prefixed identifier} nodes in cases where the target is also a simple
 * identifier.
 * 
 * <pre>
 * propertyAccess ::=
 *     {@link Expression target} '.' {@link SimpleIdentifier propertyName}
 * </pre>
 */
public class PropertyAccess extends Expression {
  /**
   * The expression computing the object defining the property being accessed.
   */
  private Expression target;

  /**
   * The property access operator.
   */
  private Token operator;

  /**
   * The name of the property being accessed.
   */
  private SimpleIdentifier propertyName;

  /**
   * Initialize a newly created property access expression.
   */
  public PropertyAccess() {
  }

  /**
   * Initialize a newly created property access expression.
   * 
   * @param target the expression computing the object defining the property being accessed
   * @param operator the property access operator
   * @param propertyName the name of the property being accessed
   */
  public PropertyAccess(Expression target, Token operator, SimpleIdentifier propertyName) {
    this.target = becomeParentOf(target);
    this.operator = operator;
    this.propertyName = becomeParentOf(propertyName);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitPropertyAccess(this);
  }

  @Override
  public Token getBeginToken() {
    return target.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return propertyName.getEndToken();
  }

  /**
   * Return the property access operator.
   * 
   * @return the property access operator
   */
  public Token getOperator() {
    return operator;
  }

  /**
   * Return the name of the property being accessed.
   * 
   * @return the name of the property being accessed
   */
  public SimpleIdentifier getPropertyName() {
    return propertyName;
  }

  /**
   * Return the expression computing the object defining the property being accessed.
   * 
   * @return the expression computing the object defining the property being accessed
   */
  public Expression getTarget() {
    return target;
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
    return operator.getType() == TokenType.PERIOD_PERIOD;
  }

  /**
   * Set the property access operator to the given token.
   * 
   * @param operator the property access operator
   */
  public void setOperator(Token operator) {
    this.operator = operator;
  }

  /**
   * Set the name of the property being accessed to the given identifier.
   * 
   * @param identifier the name of the property being accessed
   */
  public void setPropertyName(SimpleIdentifier identifier) {
    propertyName = becomeParentOf(identifier);
  }

  /**
   * Set the expression computing the object defining the property being accessed to the given
   * expression.
   * 
   * @param expression the expression computing the object defining the property being accessed
   */
  public void setTarget(Expression expression) {
    target = becomeParentOf(expression);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(target, visitor);
    safelyVisitChild(propertyName, visitor);
  }
}
