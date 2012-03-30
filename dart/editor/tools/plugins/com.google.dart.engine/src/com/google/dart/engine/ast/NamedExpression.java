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
 * Instances of the class <code>NamedExpression</code> represent an expression that has a name
 * associated with it. They are used in method invocations when there are named parameters.
 * 
 * <pre>
 * namedExpression ::=
 *     {@link Label name} {@link Expression expression}
 * </pre>
 */
public class NamedExpression extends Expression {
  /**
   * The name associated with the expression.
   */
  private Label name;

  /**
   * The expression with which the name is associated.
   */
  private Expression expression;

  /**
   * Initialize a newly created named expression.
   */
  public NamedExpression() {
  }

  /**
   * Initialize a newly created named expression.
   * 
   * @param name the name associated with the expression
   * @param expression the expression with which the name is associated
   */
  public NamedExpression(Label name, Expression expression) {
    this.name = becomeParentOf(name);
    this.expression = becomeParentOf(expression);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitNamedExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return name.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return expression.getEndToken();
  }

  /**
   * Return the expression with which the name is associated.
   * 
   * @return the expression with which the name is associated
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the name associated with the expression.
   * 
   * @return the name associated with the expression
   */
  public Label getName() {
    return name;
  }

  /**
   * Set the expression with which the name is associated to the given expression.
   * 
   * @param expression the expression with which the name is associated
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the name associated with the expression to the given identifier.
   * 
   * @param identifier the name associated with the expression
   */
  public void setName(Label identifier) {
    name = becomeParentOf(identifier);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(name, visitor);
    safelyVisitChild(expression, visitor);
  }
}
