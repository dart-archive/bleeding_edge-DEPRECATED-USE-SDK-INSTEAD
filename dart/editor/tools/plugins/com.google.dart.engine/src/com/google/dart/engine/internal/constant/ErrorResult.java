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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.resolver.TypeProvider;

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
    private AstNode node;

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
    public ErrorData(AstNode node, ErrorCode errorCode) {
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
    public AstNode getNode() {
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
  public ErrorResult(AstNode node, ErrorCode errorCode) {
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
  public EvaluationResultImpl add(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.addToError(node, this);
  }

  @Override
  public EvaluationResultImpl applyBooleanConversion(TypeProvider typeProvider, AstNode node) {
    return this;
  }

  @Override
  public EvaluationResultImpl bitAnd(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.bitAndError(node, this);
  }

  @Override
  public EvaluationResultImpl bitNot(TypeProvider typeProvider, Expression node) {
    return this;
  }

  @Override
  public EvaluationResultImpl bitOr(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.bitOrError(node, this);
  }

  @Override
  public EvaluationResultImpl bitXor(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.bitXorError(node, this);
  }

  @Override
  public EvaluationResultImpl concatenate(TypeProvider typeProvider, Expression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.concatenateError(node, this);
  }

  @Override
  public EvaluationResultImpl divide(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.divideError(node, this);
  }

  @Override
  public EvaluationResultImpl equalEqual(TypeProvider typeProvider, Expression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.equalEqualError(node, this);
  }

  @Override
  public boolean equalValues(TypeProvider typeProvider, EvaluationResultImpl result) {
    return false;
  }

  public ArrayList<ErrorData> getErrorData() {
    return errors;
  }

  @Override
  public EvaluationResultImpl greaterThan(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.greaterThanError(node, this);
  }

  @Override
  public EvaluationResultImpl greaterThanOrEqual(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.greaterThanOrEqualError(node, this);
  }

  @Override
  public EvaluationResultImpl integerDivide(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.integerDivideError(node, this);
  }

  @Override
  public EvaluationResultImpl integerDivideValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  public EvaluationResultImpl lessThan(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.lessThanError(node, this);
  }

  @Override
  public EvaluationResultImpl lessThanOrEqual(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.lessThanOrEqualError(node, this);
  }

  @Override
  public EvaluationResultImpl logicalAnd(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.logicalAndError(node, this);
  }

  @Override
  public EvaluationResultImpl logicalNot(TypeProvider typeProvider, Expression node) {
    return this;
  }

  @Override
  public EvaluationResultImpl logicalOr(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.logicalOrError(node, this);
  }

  @Override
  public EvaluationResultImpl minus(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.minusError(node, this);
  }

  @Override
  public EvaluationResultImpl negated(TypeProvider typeProvider, Expression node) {
    return this;
  }

  @Override
  public EvaluationResultImpl notEqual(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.notEqualError(node, this);
  }

  @Override
  public EvaluationResultImpl performToString(TypeProvider typeProvider, AstNode node) {
    return this;
  }

  @Override
  public EvaluationResultImpl remainder(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.remainderError(node, this);
  }

  @Override
  public EvaluationResultImpl shiftLeft(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.shiftLeftError(node, this);
  }

  @Override
  public EvaluationResultImpl shiftRight(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.shiftRightError(node, this);
  }

  @Override
  public EvaluationResultImpl stringLength(TypeProvider typeProvider, Expression node) {
    return this;
  }

  @Override
  public EvaluationResultImpl times(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.timesError(node, this);
  }

  @Override
  protected EvaluationResultImpl addToError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl addToValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl bitAndError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl bitAndValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl bitOrError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl bitOrValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl bitXorError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl bitXorValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl concatenateError(Expression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl concatenateValid(TypeProvider typeProvider, Expression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl divideError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl divideValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl equalEqualError(Expression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl equalEqualValid(TypeProvider typeProvider, Expression node,
      ValidResult leftOperand) {
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
  protected EvaluationResultImpl greaterThanOrEqualValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl greaterThanValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
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
  protected EvaluationResultImpl lessThanOrEqualValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl lessThanValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl logicalAndError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl logicalAndValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl logicalOrError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl logicalOrValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl minusError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl minusValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl notEqualError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl notEqualValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl remainderError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl remainderValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl shiftLeftError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl shiftLeftValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl shiftRightError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl shiftRightValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }

  @Override
  protected EvaluationResultImpl timesError(BinaryExpression node, ErrorResult leftOperand) {
    return new ErrorResult(this, leftOperand);
  }

  @Override
  protected EvaluationResultImpl timesValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    return this;
  }
}
