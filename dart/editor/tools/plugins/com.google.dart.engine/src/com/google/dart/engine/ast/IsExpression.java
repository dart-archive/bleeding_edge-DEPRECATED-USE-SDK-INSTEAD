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
 * Instances of the class <code>IsExpression</code> represent an is expression.
 * 
 * <pre>
 * isExpression ::=
 *     {@link Expression expression} 'is' '!'? {@link TypeName type}
 * </pre>
 */
public class IsExpression extends Expression {
  /**
   * The expression used to compute the value whose type is being tested.
   */
  private Expression expression;

  /**
   * The is operator.
   */
  private Token isOperator;

  /**
   * The not operator, or <code>null</code> if the sense of the test is not negated.
   */
  private Token notOperator;

  /**
   * The name of the type being tested for.
   */
  private TypeName type;

  /**
   * Initialize a newly created is expression.
   */
  public IsExpression() {
  }

  /**
   * Initialize a newly created is expression.
   * 
   * @param expression the expression used to compute the value whose type is being tested
   * @param isOperator the is operator
   * @param notOperator the not operator, or <code>null</code> if the sense of the test is not
   *          negated
   * @param type the name of the type being tested for
   */
  public IsExpression(Expression expression, Token isOperator, Token notOperator, TypeName type) {
    this.expression = becomeParentOf(expression);
    this.isOperator = isOperator;
    this.notOperator = notOperator;
    this.type = becomeParentOf(type);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitIsExpression(this);
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
   * Return the expression used to compute the value whose type is being tested.
   * 
   * @return the expression used to compute the value whose type is being tested
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the is operator being applied.
   * 
   * @return the is operator being applied
   */
  public Token getIsOperator() {
    return isOperator;
  }

  /**
   * Return the not operator being applied.
   * 
   * @return the not operator being applied
   */
  public Token getNotOperator() {
    return notOperator;
  }

  /**
   * Return the name of the type being tested for.
   * 
   * @return the name of the type being tested for
   */
  public TypeName getType() {
    return type;
  }

  /**
   * Set the expression used to compute the value whose type is being tested to the given
   * expression.
   * 
   * @param expression the expression used to compute the value whose type is being tested
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the is operator being applied to the given operator.
   * 
   * @param isOperator the is operator being applied
   */
  public void setIsOperator(Token isOperator) {
    this.isOperator = isOperator;
  }

  /**
   * Set the not operator being applied to the given operator.
   * 
   * @param notOperator the is operator being applied
   */
  public void setNotOperator(Token notOperator) {
    this.notOperator = notOperator;
  }

  /**
   * Set the name of the type being tested for to the given name.
   * 
   * @param name the name of the type being tested for
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
