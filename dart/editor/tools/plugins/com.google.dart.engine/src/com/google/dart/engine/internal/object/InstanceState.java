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
package com.google.dart.engine.internal.object;

import com.google.dart.engine.error.CompileTimeErrorCode;

import java.util.HashMap;

/**
 * The class {@code InstanceState} defines the behavior of objects representing the state of a Dart
 * object.
 */
public abstract class InstanceState {
  /**
   * Return the result of invoking the '+' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '+' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public InstanceState add(InstanceState rightOperand) throws EvaluationException {
    if (this instanceof StringState || rightOperand instanceof StringState) {
      return concatenate(rightOperand);
    }
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '&' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public IntState bitAnd(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(this);
    assertIntOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '~' operator on this object.
   * 
   * @return the result of invoking the '~' operator on this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public IntState bitNot() throws EvaluationException {
    assertIntOrNull(this);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '|' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '|' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public IntState bitOr(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(this);
    assertIntOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '^' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '^' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public IntState bitXor(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(this);
    assertIntOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the ' ' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the ' ' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public StringState concatenate(InstanceState rightOperand) throws EvaluationException {
    assertString(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of applying boolean conversion to this object.
   * 
   * @param typeProvider the type provider used to find known types
   * @return the result of applying boolean conversion to this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public BoolState convertToBool() throws EvaluationException {
    return BoolState.FALSE_STATE;
  }

  /**
   * Return the result of converting this object to a String.
   * 
   * @return the result of converting this object to a String
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public abstract StringState convertToString() throws EvaluationException;

  /**
   * Return the result of invoking the '/' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '/' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public NumState divide(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '==' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '==' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public abstract BoolState equalEqual(InstanceState rightOperand) throws EvaluationException;

  /**
   * If this represents a generic dart object, return a map from its fieldnames to their values.
   * Otherwise return null.
   */
  public HashMap<String, DartObjectImpl> getFields() {
    return null;
  }

  /**
   * Return the name of the type of this value.
   * 
   * @return the name of the type of this value
   */
  public abstract String getTypeName();

  /**
   * Return this object's value if it can be represented exactly, or {@code null} if either the
   * value cannot be represented exactly or if the value is {@code null}. Clients should use
   * {@link #hasExactValue()} to distinguish between these two cases.
   * 
   * @return this object's value
   */
  public Object getValue() {
    return null;
  }

  /**
   * Return the result of invoking the '&gt;' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&gt;' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public BoolState greaterThan(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '&gt;=' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&gt;=' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public BoolState greaterThanOrEqual(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return {@code true} if this object's value can be represented exactly.
   * 
   * @return {@code true} if this object's value can be represented exactly
   */
  public boolean hasExactValue() {
    return false;
  }

  /**
   * Return the result of invoking the '~/' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '~/' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public IntState integerDivide(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return {@code true} if this object represents an object whose type is 'bool'.
   * 
   * @return {@code true} if this object represents a boolean value
   */
  public boolean isBool() {
    return false;
  }

  /**
   * Return {@code true} if this object represents an object whose type is either 'bool', 'num',
   * 'String', or 'Null'.
   * 
   * @return {@code true} if this object represents either a boolean, numeric, string or null value
   */
  public boolean isBoolNumStringOrNull() {
    return false;
  }

  /**
   * Return true if this object represents an unknown value.
   */
  public boolean isUnknown() {
    return false;
  }

  /**
   * Return the result of invoking the '&lt;' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&lt;' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public BoolState lessThan(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '&lt;=' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&lt;=' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public BoolState lessThanOrEqual(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '&&' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&&' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public BoolState logicalAnd(InstanceState rightOperand) throws EvaluationException {
    assertBool(this);
    assertBool(rightOperand);
    return BoolState.FALSE_STATE;
  }

  /**
   * Return the result of invoking the '!' operator on this object.
   * 
   * @return the result of invoking the '!' operator on this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public BoolState logicalNot() throws EvaluationException {
    assertBool(this);
    return BoolState.TRUE_STATE;
  }

  /**
   * Return the result of invoking the '||' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '||' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public BoolState logicalOr(InstanceState rightOperand) throws EvaluationException {
    assertBool(this);
    assertBool(rightOperand);
    return rightOperand.convertToBool();
  }

  /**
   * Return the result of invoking the '-' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '-' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public NumState minus(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '-' operator on this object.
   * 
   * @return the result of invoking the '-' operator on this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public NumState negated() throws EvaluationException {
    assertNumOrNull(this);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '%' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '%' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public NumState remainder(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '&lt;&lt;' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&lt;&lt;' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public IntState shiftLeft(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(this);
    assertIntOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '&gt;&gt;' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&gt;&gt;' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public IntState shiftRight(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(this);
    assertIntOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the 'length' getter on this object.
   * 
   * @return the result of invoking the 'length' getter on this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public IntState stringLength() throws EvaluationException {
    assertString(this);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return the result of invoking the '*' operator on this object with the given argument.
   * 
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '*' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public NumState times(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(this);
    assertNumOrNull(rightOperand);
    throw new EvaluationException(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Throw an exception if the given state does not represent a boolean value.
   * 
   * @param state the state being tested
   * @throws EvaluationException if the given state does not represent a boolean value
   */
  protected void assertBool(InstanceState state) throws EvaluationException {
    if (!(state instanceof BoolState || state instanceof DynamicState)) {
      throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL);
    }
  }

  /**
   * Throw an exception if the given state does not represent a boolean, numeric, string or null
   * value.
   * 
   * @param state the state being tested
   * @throws EvaluationException if the given state does not represent a boolean, numeric, string or
   *           null value
   */
  protected void assertBoolNumStringOrNull(InstanceState state) throws EvaluationException {
    if (!(state instanceof BoolState || state instanceof DoubleState || state instanceof IntState
        || state instanceof NumState || state instanceof StringState || state instanceof NullState || state instanceof DynamicState)) {
      throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING);
    }
  }

  /**
   * Throw an exception if the given state does not represent an integer or null value.
   * 
   * @param state the state being tested
   * @throws EvaluationException if the given state does not represent an integer or null value
   */
  protected void assertIntOrNull(InstanceState state) throws EvaluationException {
    if (!(state instanceof IntState || state instanceof NumState || state instanceof NullState || state instanceof DynamicState)) {
      throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_TYPE_INT);
    }
  }

  /**
   * Throw an exception if the given state does not represent a boolean, numeric, string or null
   * value.
   * 
   * @param state the state being tested
   * @throws EvaluationException if the given state does not represent a boolean, numeric, string or
   *           null value
   */
  protected void assertNumOrNull(InstanceState state) throws EvaluationException {
    if (!(state instanceof DoubleState || state instanceof IntState || state instanceof NumState
        || state instanceof NullState || state instanceof DynamicState)) {
      throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_TYPE_NUM);
    }
  }

  /**
   * Throw an exception if the given state does not represent a String value.
   * 
   * @param state the state being tested
   * @throws EvaluationException if the given state does not represent a String value
   */
  protected void assertString(InstanceState state) throws EvaluationException {
    if (!(state instanceof StringState || state instanceof DynamicState)) {
      throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL);
    }
  }
}
