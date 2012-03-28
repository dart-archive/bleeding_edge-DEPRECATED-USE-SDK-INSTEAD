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
 * Instances of the class <code>PostfixExpression</code> represent a postfix unary expression.
 * 
 * <pre>
 * postfixExpression ::=
 *     {@link Expression operand} {@link Token operator}
 * </pre>
 */
public class PostfixExpression extends Expression {
  /**
   * The expression computing the operand for the operator.
   */
  private Expression operand;

  /**
   * The postfix operator being applied to the operand.
   */
  private Token operator;

  /**
   * Initialize a newly created postfix expression.
   */
  public PostfixExpression() {
  }

  /**
   * Initialize a newly created postfix expression.
   * 
   * @param operand the expression computing the operand for the operator
   * @param operator the postfix operator being applied to the operand
   */
  public PostfixExpression(Expression operand, Token operator) {
    this.operand = becomeParentOf(operand);
    this.operator = operator;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitPostfixExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return operand.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return operator;
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
   * Return the postfix operator being applied to the operand.
   * 
   * @return the postfix operator being applied to the operand
   */
  public Token getOperator() {
    return operator;
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
   * Set the postfix operator being applied to the operand to the given operator.
   * 
   * @param operator the postfix operator being applied to the operand
   */
  public void setOperator(Token operator) {
    this.operator = operator;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(operand, visitor);
  }
}
