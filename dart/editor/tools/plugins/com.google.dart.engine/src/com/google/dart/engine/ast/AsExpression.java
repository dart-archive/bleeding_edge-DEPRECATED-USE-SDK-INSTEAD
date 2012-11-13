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
 * Instances of the class {@code AsExpression} represent an 'as' expression.
 * 
 * <pre>
 * asExpression ::=
 *     {@link Expression expression} 'as' {@link TypeName type}
 * </pre>
 */
public class AsExpression extends Expression {
  /**
   * The expression used to compute the value being cast.
   */
  private Expression expression;

  /**
   * The as operator.
   */
  private Token asOperator;

  /**
   * The name of the type being cast to.
   */
  private TypeName type;

  /**
   * Initialize a newly created as expression.
   */
  public AsExpression() {
  }

  /**
   * Initialize a newly created as expression.
   * 
   * @param expression the expression used to compute the value being cast
   * @param isOperator the is operator
   * @param type the name of the type being cast to
   */
  public AsExpression(Expression expression, Token isOperator, TypeName type) {
    this.expression = becomeParentOf(expression);
    this.asOperator = isOperator;
    this.type = becomeParentOf(type);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitAsExpression(this);
  }

  /**
   * Return the is operator being applied.
   * 
   * @return the is operator being applied
   */
  public Token getAsOperator() {
    return asOperator;
  }

  @Override
  public Token getBeginToken() {
    return expression.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return type.getEndToken();
  }

  /**
   * Return the expression used to compute the value being cast.
   * 
   * @return the expression used to compute the value being cast
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the name of the type being cast to.
   * 
   * @return the name of the type being cast to
   */
  public TypeName getType() {
    return type;
  }

  /**
   * Set the is operator being applied to the given operator.
   * 
   * @param asOperator the is operator being applied
   */
  public void setAsOperator(Token asOperator) {
    this.asOperator = asOperator;
  }

  /**
   * Set the expression used to compute the value being cast to the given expression.
   * 
   * @param expression the expression used to compute the value being cast
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the name of the type being cast to to the given name.
   * 
   * @param name the name of the type being cast to
   */
  public void setType(TypeName name) {
    this.type = becomeParentOf(name);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(expression, visitor);
    safelyVisitChild(type, visitor);
  }
}
