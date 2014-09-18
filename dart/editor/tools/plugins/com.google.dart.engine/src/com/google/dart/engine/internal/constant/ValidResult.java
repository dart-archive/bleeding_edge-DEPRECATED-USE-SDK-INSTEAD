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
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.EvaluationException;
import com.google.dart.engine.internal.resolver.TypeProvider;

/**
 * Instances of the class {@code ValidResult} represent the result of attempting to evaluate a valid
 * compile time constant expression.
 */
public class ValidResult extends EvaluationResultImpl {
  /**
   * The value of the expression.
   */
  private final DartObjectImpl value;

  /**
   * Initialize a newly created result to represent the given value.
   * 
   * @param value the value of the expression
   */
  public ValidResult(DartObjectImpl value) {
    this.value = value;
  }

  @Override
  public EvaluationResultImpl add(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.addToValid(typeProvider, node, this);
  }

  /**
   * Return the result of applying boolean conversion to this result.
   * 
   * @param node the node against which errors should be reported
   * @return the result of applying boolean conversion to the given value
   */
  @Override
  public EvaluationResultImpl applyBooleanConversion(TypeProvider typeProvider, AstNode node) {
    try {
      return valueOf(value.convertToBool(typeProvider));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  public EvaluationResultImpl bitAnd(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.bitAndValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl bitNot(TypeProvider typeProvider, Expression node) {
    try {
      return valueOf(value.bitNot(typeProvider));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  public EvaluationResultImpl bitOr(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.bitOrValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl bitXor(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.bitXorValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl concatenate(TypeProvider typeProvider, Expression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.concatenateValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl divide(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.divideValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl equalEqual(TypeProvider typeProvider, Expression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.equalEqualValid(typeProvider, node, this);
  }

  @Override
  public boolean equalValues(TypeProvider typeProvider, EvaluationResultImpl result) {
    if (!(result instanceof ValidResult)) {
      return false;
    }
    return value.equals(((ValidResult) result).value);
  }

  public DartObjectImpl getValue() {
    return value;
  }

  @Override
  public EvaluationResultImpl greaterThan(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.greaterThanValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl greaterThanOrEqual(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.greaterThanOrEqualValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl integerDivide(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.integerDivideValid(typeProvider, node, this);
  }

  /**
   * Return {@code true} if this object represents an object whose type is 'bool'.
   * 
   * @return {@code true} if this object represents a boolean value
   */
  public boolean isBool() {
    return value.isBool();
  }

  /**
   * Return {@code true} if this object represents an object whose type is either 'bool', 'num',
   * 'String', or 'Null'.
   * 
   * @return {@code true} if this object represents either a boolean, numeric, string or null value
   */
  public boolean isBoolNumStringOrNull() {
    return value.isBoolNumStringOrNull();
  }

  /**
   * Return {@code true} if this result represents the value 'false'.
   * 
   * @return {@code true} if this result represents the value 'false'
   */
  public boolean isFalse() {
    return value.isFalse();
  }

  /**
   * Return {@code true} if this result represents the value 'null'.
   * 
   * @return {@code true} if this result represents the value 'null'
   */
  public boolean isNull() {
    return value.isNull();
  }

  /**
   * Return {@code true} if this result represents the value 'true'.
   * 
   * @return {@code true} if this result represents the value 'true'
   */
  public boolean isTrue() {
    return value.isTrue();
  }

  /**
   * Return {@code true} if this object represents an instance of a user-defined class.
   * 
   * @return {@code true} if this object represents an instance of a user-defined class
   */
  public boolean isUserDefinedObject() {
    return value.isUserDefinedObject();
  }

  @Override
  public EvaluationResultImpl lessThan(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.lessThanValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl lessThanOrEqual(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.lessThanOrEqualValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl logicalAnd(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.logicalAndValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl logicalNot(TypeProvider typeProvider, Expression node) {
    try {
      return valueOf(value.logicalNot(typeProvider));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  public EvaluationResultImpl logicalOr(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.logicalOrValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl minus(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.minusValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl negated(TypeProvider typeProvider, Expression node) {
    try {
      return valueOf(value.negated(typeProvider));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  public EvaluationResultImpl notEqual(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.notEqualValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl performToString(TypeProvider typeProvider, AstNode node) {
    try {
      return valueOf(value.performToString(typeProvider));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  public EvaluationResultImpl remainder(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.remainderValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl shiftLeft(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.shiftLeftValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl shiftRight(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.shiftRightValid(typeProvider, node, this);
  }

  @Override
  public EvaluationResultImpl stringLength(TypeProvider typeProvider, Expression node) {
    try {
      return valueOf(value.stringLength(typeProvider));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  public EvaluationResultImpl times(TypeProvider typeProvider, BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.timesValid(typeProvider, node, this);
  }

  @Override
  public String toString() {
    if (value == null) {
      return "null";
    }
    return value.toString();
  }

  @Override
  protected EvaluationResultImpl addToError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl addToValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().add(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl bitAndError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl bitAndValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().bitAnd(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl bitOrError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl bitOrValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().bitOr(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl bitXorError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl bitXorValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().bitXor(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl concatenateError(Expression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl concatenateValid(TypeProvider typeProvider, Expression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().concatenate(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl divideError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl divideValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().divide(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl equalEqualError(Expression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl equalEqualValid(TypeProvider typeProvider, Expression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().equalEqual(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl greaterThanError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl greaterThanOrEqualError(BinaryExpression node,
      ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl greaterThanOrEqualValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().greaterThanOrEqual(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl greaterThanValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().greaterThan(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl integerDivideError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl integerDivideValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().integerDivide(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl lessThanError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl lessThanOrEqualError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl lessThanOrEqualValid(TypeProvider typeProvider,
      BinaryExpression node, ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().lessThanOrEqual(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl lessThanValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().lessThan(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl logicalAndError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl logicalAndValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().logicalAnd(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl logicalOrError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl logicalOrValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().logicalOr(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl minusError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl minusValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().minus(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl notEqualError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl notEqualValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().notEqual(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl remainderError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl remainderValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().remainder(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl shiftLeftError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl shiftLeftValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().shiftLeft(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl shiftRightError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl shiftRightValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().shiftRight(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  @Override
  protected EvaluationResultImpl timesError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl timesValid(TypeProvider typeProvider, BinaryExpression node,
      ValidResult leftOperand) {
    try {
      return valueOf(leftOperand.getValue().times(typeProvider, value));
    } catch (EvaluationException exception) {
      return error(node, exception.getErrorCode());
    }
  }

  /**
   * Return a result object representing an error associated with the given node.
   * 
   * @param node the AST node associated with the error
   * @param code the error code indicating the nature of the error
   * @return a result object representing an error associated with the given node
   */
  private ErrorResult error(AstNode node, ErrorCode code) {
    return new ErrorResult(node, code);
  }

  /**
   * Return a result object representing the given value.
   * 
   * @param value the value to be represented as a result object
   * @return a result object representing the given value
   */
  private ValidResult valueOf(DartObjectImpl value) {
    return new ValidResult(value);
  }
}
