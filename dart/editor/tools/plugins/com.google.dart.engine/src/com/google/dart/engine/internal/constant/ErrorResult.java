/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.constant;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.error.ErrorCode;

import java.util.ArrayList;

/**
 * Instances of the class {@code ErrorResult} represent the result of evaluating an expression that
 * is not a valid compile time constant.
 */
public class ErrorResult extends EvaluationResultImpl {
  public static class ErrorData {
    /**
     * The node against which the error should be reported.
     */
    private ASTNode node;

    /**
     * The error code for the error to be generated.
     */
    private ErrorCode errorCode;

    /**
     * Initialize a newly created data holder to represent the error with the given code reported
     * against the given node.
     * 
     * @param node the node against which the error should be reported
     * @param errorCode the error code for the error to be generated
     */
    public ErrorData(ASTNode node, ErrorCode errorCode) {
      this.node = node;
      this.errorCode = errorCode;
    }

    /**
     * Return the error code for the error to be generated.
     * 
     * @return the error code for the error to be generated
     */
    public ErrorCode getErrorCode() {
      return errorCode;
    }

    /**
     * Return the node against which the error should be reported.
     * 
     * @return the node against which the error should be reported
     */
    public ASTNode getNode() {
      return node;
    }
  }

  /**
   * The errors that prevent the expression from being a valid compile time constant.
   */
  private ArrayList<ErrorData> errors = new ArrayList<ErrorData>();

  /**
   * Initialize a newly created result representing the error with the given code reported against
   * the given node.
   * 
   * @param node the node against which the error should be reported
   * @param errorCode the error code for the error to be generated
   */
  public ErrorResult(ASTNode node, ErrorCode errorCode) {
    errors.add(new ErrorData(node, errorCode));
  }

  /**
   * Initialize a newly created result to represent the union of the errors in the given result
   * objects.
   * 
   * @param firstResult the first set of results being merged
   * @param secondResult the second set of results being merged
   */
  public ErrorResult(ErrorResult firstResult, ErrorResult secondResult) {
    errors.addAll(firstResult.errors);
    errors.addAll(secondResult.errors);
  }

  @Override
  public EvaluationResultImpl add(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.addToError(node, this);
  }

  @Override
  public EvaluationResultImpl bitAnd(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.bitAndError(node, this);
  }

  @Override
  public EvaluationResultImpl bitNot(Expression node) {
    return this;
  }

  @Override
  public EvaluationResultImpl bitOr(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.bitOrError(node, this);
  }

  @Override
  public EvaluationResultImpl bitXor(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.bitXorError(node, this);
  }

  @Override
  public EvaluationResultImpl concatenate(Expression node, EvaluationResultImpl rightOperand) {
    return rightOperand.concatenateError(node, this);
  }

  @Override
  public EvaluationResultImpl divide(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.divideError(node, this);
  }

  @Override
  public EvaluationResultImpl equalEqual(Expression node, EvaluationResultImpl rightOperand) {
    return rightOperand.equalEqualError(node, this);
  }

  public ArrayList<ErrorData> getErrorData() {
    return errors;
  }

  @Override
  public EvaluationResultImpl greaterThan(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.greaterThanError(node, this);
  }

  @Override
  public EvaluationResultImpl greaterThanOrEqual(BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.greaterThanOrEqualError(node, this);
  }

  @Override
  public EvaluationResultImpl integerDivide(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.integerDivideError(node, this);
  }

  @Override
  public EvaluationResultImpl integerDivideValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  public EvaluationResultImpl lessThan(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.lessThanError(node, this);
  }

  @Override
  public EvaluationResultImpl lessThanOrEqual(BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.lessThanOrEqualError(node, this);
  }

  @Override
  public EvaluationResultImpl logicalAnd(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.logicalAndError(node, this);
  }

  @Override
  public EvaluationResultImpl logicalNot(Expression node) {
    return this;
  }

  @Override
  public EvaluationResultImpl logicalOr(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.logicalOrError(node, this);
  }

  @Override
  public EvaluationResultImpl minus(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.minusError(node, this);
  }

  @Override
  public EvaluationResultImpl negated(Expression node) {
    return this;
  }

  @Override
  public EvaluationResultImpl notEqual(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.notEqualError(node, this);
  }

  @Override
  public EvaluationResultImpl performToString(ASTNode node) {
    return this;
  }

  @Override
  public EvaluationResultImpl remainder(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.remainderError(node, this);
  }

  @Override
  public EvaluationResultImpl shiftLeft(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.shiftLeftError(node, this);
  }

  @Override
  public EvaluationResultImpl shiftRight(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.shiftRightError(node, this);
  }

  @Override
  public EvaluationResultImpl times(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.timesError(node, this);
  }

  @Override
  protected EvaluationResultImpl addToError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl addToValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl bitAndError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl bitAndValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl bitOrError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl bitOrValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl bitXorError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl bitXorValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl concatenateError(Expression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl concatenateValid(Expression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl divideError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl divideValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl equalEqualError(Expression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl equalEqualValid(Expression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl greaterThanError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl greaterThanOrEqualError(BinaryExpression node,
      ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl greaterThanOrEqualValid(BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl greaterThanValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl integerDivideError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl lessThanError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl lessThanOrEqualError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl lessThanOrEqualValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl lessThanValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl logicalAndError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl logicalAndValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl logicalOrError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl logicalOrValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl minusError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl minusValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl notEqualError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl notEqualValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl remainderError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl remainderValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl shiftLeftError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl shiftLeftValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl shiftRightError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl shiftRightValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl timesError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl timesValid(BinaryExpression node, ValidResult leftOperand) {
    return this;
  }
}
