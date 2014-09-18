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
import com.google.dart.engine.internal.resolver.TypeProvider;

/**
 * Instances of the class {@code InternalResult} represent the result of attempting to evaluate a
 * expression.
 */
public abstract class EvaluationResultImpl {
  public abstract EvaluationResultImpl add(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  /**
   * Return the result of applying boolean conversion to this result.
   * 
   * @param typeProvider the type provider used to access known types
   * @param node the node against which errors should be reported
   * @return the result of applying boolean conversion to the given value
   */
  public abstract EvaluationResultImpl applyBooleanConversion(TypeProvider typeProvider,
      AstNode node);

  public abstract EvaluationResultImpl bitAnd(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl bitNot(TypeProvider typeProvider, Expression node);

  public abstract EvaluationResultImpl bitOr(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl bitXor(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl concatenate(TypeProvider typeProvider, Expression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl divide(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl equalEqual(TypeProvider typeProvider, Expression node,
      EvaluationResultImpl rightOperand);

  public abstract boolean equalValues(TypeProvider typeProvider, EvaluationResultImpl result);

  public abstract EvaluationResultImpl greaterThan(TypeProvider typeProvider,
      BinaryExpression node, EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl greaterThanOrEqual(TypeProvider typeProvider,
      BinaryExpression node, EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl integerDivide(TypeProvider typeProvider,
      BinaryExpression node, EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl lessThan(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl lessThanOrEqual(TypeProvider typeProvider,
      BinaryExpression node, EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl logicalAnd(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl logicalNot(TypeProvider typeProvider, Expression node);

  public abstract EvaluationResultImpl logicalOr(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl minus(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl negated(TypeProvider typeProvider, Expression node);

  public abstract EvaluationResultImpl notEqual(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl performToString(TypeProvider typeProvider, AstNode node);

  public abstract EvaluationResultImpl remainder(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl shiftLeft(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  public abstract EvaluationResultImpl shiftRight(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  /**
   * Return the result of invoking the 'length' getter on this result.
   * 
   * @param typeProvider the type provider used to access known types
   * @param node the node against which errors should be reported
   * @return the result of invoking the 'length' getter on this result
   */
  public abstract EvaluationResultImpl stringLength(TypeProvider typeProvider, Expression node);

  public abstract EvaluationResultImpl times(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand);

  protected abstract EvaluationResultImpl addToError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl addToValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl bitAndError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl bitAndValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl bitOrError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl bitOrValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl bitXorError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl bitXorValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl concatenateError(Expression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl concatenateValid(TypeProvider typeProvider,
      Expression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl divideError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl divideValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl equalEqualError(Expression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl equalEqualValid(TypeProvider typeProvider,
      Expression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl greaterThanError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl greaterThanOrEqualError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl greaterThanOrEqualValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl greaterThanValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl integerDivideError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl integerDivideValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl lessThanError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl lessThanOrEqualError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl lessThanOrEqualValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl lessThanValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl logicalAndError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl logicalAndValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl logicalOrError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl logicalOrValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl minusError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl minusValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl notEqualError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl notEqualValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl remainderError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl remainderValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl shiftLeftError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl shiftLeftValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl shiftRightError(BinaryExpression node,
      ErrorResult leftOperand);

  protected abstract EvaluationResultImpl shiftRightValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);

  protected abstract EvaluationResultImpl timesError(BinaryExpression node, ErrorResult leftOperand);

  protected abstract EvaluationResultImpl timesValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand);
}
