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
 * Instances of the class <code>BinaryExpression</code> represent a binary (infix) expression.
 * 
 * <pre>
 * binaryExpression ::=
 *     {@link Expression leftOperand} {@link Token operator} {@link Expression rightOperand}
 * </pre>
 */
public class BinaryExpression extends Expression {
  /**
   * The expression used to compute the left operand.
   */
  private Expression leftOperand;

  /**
   * The binary operator being applied.
   */
  private Token operator;

  /**
   * The expression used to compute the right operand.
   */
  private Expression rightOperand;

  /**
   * Initialize a newly created binary expression.
   */
  public BinaryExpression() {
  }

  /**
   * Initialize a newly created binary expression.
   * 
   * @param leftOperand the expression used to compute the left operand
   * @param operator the binary operator being applied
   * @param rightOperand the expression used to compute the right operand
   */
  public BinaryExpression(Expression leftOperand, Token operator, Expression rightOperand) {
    this.leftOperand = becomeParentOf(leftOperand);
    this.operator = operator;
    this.rightOperand = becomeParentOf(rightOperand);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitBinaryExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return leftOperand.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return rightOperand.getEndToken();
  }

  /**
   * Return the expression used to compute the left operand.
   * 
   * @return the expression used to compute the left operand
   */
  public Expression getLeftOperand() {
    return leftOperand;
  }

  /**
   * Return the binary operator being applied.
   * 
   * @return the binary operator being applied
   */
  public Token getOperator() {
    return operator;
  }

  /**
   * Return the expression used to compute the right operand.
   * 
   * @return the expression used to compute the right operand
   */
  public Expression getRightOperand() {
    return rightOperand;
  }

  /**
   * Set the expression used to compute the left operand to the given expression.
   * 
   * @param expression the expression used to compute the left operand
   */
  public void setLeftOperand(Expression expression) {
    leftOperand = becomeParentOf(expression);
  }

  /**
   * Set the binary operator being applied to the given operator.
   * 
   * @return the binary operator being applied
   */
  public void setOperator(Token operator) {
    this.operator = operator;
  }

  /**
   * Set the expression used to compute the right operand to the given expression.
   * 
   * @param expression the expression used to compute the right operand
   */
  public void setRightOperand(Expression expression) {
    rightOperand = becomeParentOf(expression);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    if (leftOperand != null) {
      leftOperand.accept(visitor);
    }
    if (rightOperand != null) {
      rightOperand.accept(visitor);
    }
  }
}
