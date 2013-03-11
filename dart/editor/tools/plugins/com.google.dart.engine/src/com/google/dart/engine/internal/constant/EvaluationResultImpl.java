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

/**
 * Instances of the class {@code InternalResult} represent the result of attempting to evaluate a
 * expression.
 */
public abstract class EvaluationResultImpl {
  public abstract EvaluationResultImpl add(BinaryExpression node, EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl bitAnd(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl bitNot(Expression node);

  public abstract EvaluationResultImpl bitOr(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl bitXor(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl concatenate(Expression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl divide(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl equalEqual(Expression node, EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl greaterThan(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl greaterThanOrEqual(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl integerDivide(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl lessThan(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl lessThanOrEqual(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl logicalAnd(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl logicalNot(Expression node);

  public abstract EvaluationResultImpl logicalOr(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl minus(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl negated(Expression node);

  public abstract EvaluationResultImpl notEqual(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl performToString(ASTNode node);

  public abstract EvaluationResultImpl remainder(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl shiftLeft(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl shiftRight(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl times(BinaryExpression node,
      EvaluationResultImpl rightOperand);

  protected abstract EvaluationResultImpl addToError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl addToValid(BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl bitAndError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl bitAndValid(BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl bitOrError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl bitOrValid(BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl bitXorError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl bitXorValid(BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl concatenateError(Expression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl concatenateValid(Expression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl divideError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl divideValid(BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl equalEqualError(Expression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl equalEqualValid(Expression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl greaterThanError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl greaterThanOrEqualError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl greaterThanOrEqualValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl greaterThanValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl integerDivideError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl integerDivideValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl lessThanError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl lessThanOrEqualError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl lessThanOrEqualValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl lessThanValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl logicalAndError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl logicalAndValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl logicalOrError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl logicalOrValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl minusError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl minusValid(BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl notEqualError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl notEqualValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl remainderError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl remainderValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl shiftLeftError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl shiftLeftValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl shiftRightError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl shiftRightValid(BinaryExpression node,
      ValidResult leftOperand);

  protected abstract EvaluationResultImpl timesError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl timesValid(BinaryExpression node, ValidResult leftOperand);
}
