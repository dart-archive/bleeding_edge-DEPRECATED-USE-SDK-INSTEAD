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

import com.google.dart.engine.constant.DartObject;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * Instances of the class {@code DartObjectImpl} represent an instance of a Dart class.
 */
public class DartObjectImpl implements DartObject {
  /**
   * The run-time type of this object.
   */
  private InterfaceType type;

  /**
   * The state of the object.
   */
  private InstanceState state;

  /**
   * Initialize a newly created object to have the given type and state.
   * 
   * @param type the run-time type of this object
   * @param state the state of the object
   */
  public DartObjectImpl(InterfaceType type, InstanceState state) {
    this.type = type;
    this.state = state;
  }

  /**
   * Return the result of invoking the '+' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '+' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl add(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    InstanceState result = state.add(rightOperand.state);
    if (result instanceof IntState) {
      return new DartObjectImpl(typeProvider.getIntType(), result);
    } else if (result instanceof DoubleState) {
      return new DartObjectImpl(typeProvider.getDoubleType(), result);
    } else if (result instanceof NumState) {
      return new DartObjectImpl(typeProvider.getNumType(), result);
    } else if (result instanceof StringState) {
      return new DartObjectImpl(typeProvider.getStringType(), result);
    }
    // We should never get here.
    throw new IllegalStateException("add returned a " + result.getClass().getName());
  }

  /**
   * Return the result of invoking the '&' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl bitAnd(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getIntType(), state.bitAnd(rightOperand.state));
  }

  /**
   * Return the result of invoking the '~' operator on this object.
   * 
   * @param typeProvider the type provider used to find known types
   * @return the result of invoking the '~' operator on this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl bitNot(TypeProvider typeProvider) throws EvaluationException {
    return new DartObjectImpl(typeProvider.getIntType(), state.bitNot());
  }

  /**
   * Return the result of invoking the '|' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '|' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl bitOr(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getIntType(), state.bitOr(rightOperand.state));
  }

  /**
   * Return the result of invoking the '^' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '^' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl bitXor(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getIntType(), state.bitXor(rightOperand.state));
  }

  /**
   * Return the result of invoking the ' ' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the ' ' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl concatenate(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getStringType(), state.concatenate(rightOperand.state));
  }

  /**
   * Return the result of applying boolean conversion to this object.
   * 
   * @param typeProvider the type provider used to find known types
   * @return the result of applying boolean conversion to this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl convertToBool(TypeProvider typeProvider) throws EvaluationException {
    InterfaceType boolType = typeProvider.getBoolType();
    if (type == boolType) {
      return this;
    }
    return new DartObjectImpl(boolType, state.convertToBool());
  }

  /**
   * Return the result of invoking the '/' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '/' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl divide(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    InstanceState result = state.divide(rightOperand.state);
    if (result instanceof IntState) {
      return new DartObjectImpl(typeProvider.getIntType(), result);
    } else if (result instanceof DoubleState) {
      return new DartObjectImpl(typeProvider.getDoubleType(), result);
    } else if (result instanceof NumState) {
      return new DartObjectImpl(typeProvider.getNumType(), result);
    }
    // We should never get here.
    throw new IllegalStateException("divide returned a " + result.getClass().getName());
  }

  /**
   * Return the result of invoking the '==' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '==' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl equalEqual(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    if (!type.equals(rightOperand.type)) {
      String typeName = type.getName();
      if (!(typeName.equals("bool") || typeName.equals("double") || typeName.equals("int")
          || typeName.equals("num") || typeName.equals("String") || typeName.equals("Null") || type.isDynamic())) {
        throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING);
      }
    }
    return new DartObjectImpl(typeProvider.getBoolType(), state.equalEqual(rightOperand.state));
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof DartObjectImpl)) {
      return false;
    }
    DartObjectImpl dartObject = (DartObjectImpl) object;
    return type.equals(dartObject.type) && state.equals(dartObject.state);
  }

  @Override
  public Boolean getBoolValue() {
    if (state instanceof BoolState) {
      return ((BoolState) state).getValue();
    }
    return null;
  }

  @Override
  public Double getDoubleValue() {
    if (state instanceof DoubleState) {
      return ((DoubleState) state).getValue();
    }
    return null;
  }

  public HashMap<String, DartObjectImpl> getFields() {
    return state.getFields();
  }

  @Override
  public BigInteger getIntValue() {
    if (state instanceof IntState) {
      return ((IntState) state).getValue();
    }
    return null;
  }

  @Override
  public String getStringValue() {
    if (state instanceof StringState) {
      return ((StringState) state).getValue();
    }
    return null;
  }

  @Override
  public InterfaceType getType() {
    return type;
  }

  @Override
  public Object getValue() {
    return state.getValue();
  }

  /**
   * Return the result of invoking the '&gt;' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&gt;' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl greaterThan(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getBoolType(), state.greaterThan(rightOperand.state));
  }

  /**
   * Return the result of invoking the '&gt;=' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&gt;=' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl greaterThanOrEqual(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(
        typeProvider.getBoolType(),
        state.greaterThanOrEqual(rightOperand.state));
  }

  @Override
  public boolean hasExactValue() {
    return state.hasExactValue();
  }

  @Override
  public int hashCode() {
    return ObjectUtilities.combineHashCodes(type.hashCode(), state.hashCode());
  }

  /**
   * Return the result of invoking the '~/' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '~/' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl integerDivide(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getIntType(), state.integerDivide(rightOperand.state));
  }

  /**
   * Return {@code true} if this object represents an object whose type is 'bool'.
   * 
   * @return {@code true} if this object represents a boolean value
   */
  public boolean isBool() {
    return state.isBool();
  }

  /**
   * Return {@code true} if this object represents an object whose type is either 'bool', 'num',
   * 'String', or 'Null'.
   * 
   * @return {@code true} if this object represents either a boolean, numeric, string or null value
   */
  public boolean isBoolNumStringOrNull() {
    return state.isBoolNumStringOrNull();
  }

  @Override
  public boolean isFalse() {
    return state instanceof BoolState && ((BoolState) state).getValue() == Boolean.FALSE;
  }

  @Override
  public boolean isNull() {
    return state instanceof NullState;
  }

  @Override
  public boolean isTrue() {
    return state instanceof BoolState && ((BoolState) state).getValue() == Boolean.TRUE;
  }

  /**
   * Return true if this object represents an unknown value.
   */
  public boolean isUnknown() {
    return state.isUnknown();
  }

  /**
   * Return {@code true} if this object represents an instance of a user-defined class.
   * 
   * @return {@code true} if this object represents an instance of a user-defined class
   */
  public boolean isUserDefinedObject() {
    return state instanceof GenericState;
  }

  /**
   * Return the result of invoking the '&lt;' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&lt;' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl lessThan(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getBoolType(), state.lessThan(rightOperand.state));
  }

  /**
   * Return the result of invoking the '&lt;=' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&lt;=' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl lessThanOrEqual(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getBoolType(), state.lessThanOrEqual(rightOperand.state));
  }

  /**
   * Return the result of invoking the '&&' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&&' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl logicalAnd(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getBoolType(), state.logicalAnd(rightOperand.state));
  }

  /**
   * Return the result of invoking the '!' operator on this object.
   * 
   * @param typeProvider the type provider used to find known types
   * @return the result of invoking the '!' operator on this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl logicalNot(TypeProvider typeProvider) throws EvaluationException {
    return new DartObjectImpl(typeProvider.getBoolType(), state.logicalNot());
  }

  /**
   * Return the result of invoking the '||' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '||' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl logicalOr(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getBoolType(), state.logicalOr(rightOperand.state));
  }

  /**
   * Return the result of invoking the '-' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '-' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl minus(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    InstanceState result = state.minus(rightOperand.state);
    if (result instanceof IntState) {
      return new DartObjectImpl(typeProvider.getIntType(), result);
    } else if (result instanceof DoubleState) {
      return new DartObjectImpl(typeProvider.getDoubleType(), result);
    } else if (result instanceof NumState) {
      return new DartObjectImpl(typeProvider.getNumType(), result);
    }
    // We should never get here.
    throw new IllegalStateException("minus returned a " + result.getClass().getName());
  }

  /**
   * Return the result of invoking the '-' operator on this object.
   * 
   * @param typeProvider the type provider used to find known types
   * @return the result of invoking the '-' operator on this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl negated(TypeProvider typeProvider) throws EvaluationException {
    InstanceState result = state.negated();
    if (result instanceof IntState) {
      return new DartObjectImpl(typeProvider.getIntType(), result);
    } else if (result instanceof DoubleState) {
      return new DartObjectImpl(typeProvider.getDoubleType(), result);
    } else if (result instanceof NumState) {
      return new DartObjectImpl(typeProvider.getNumType(), result);
    }
    // We should never get here.
    throw new IllegalStateException("negated returned a " + result.getClass().getName());
  }

  /**
   * Return the result of invoking the '!=' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '!=' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl notEqual(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    if (!type.equals(rightOperand.type)) {
      String typeName = type.getName();
      if (!typeName.equals("bool") && !typeName.equals("double") && !typeName.equals("int")
          && !typeName.equals("num") && !typeName.equals("String")) {
        return new DartObjectImpl(typeProvider.getBoolType(), BoolState.TRUE_STATE);
      }
    }
    return new DartObjectImpl(
        typeProvider.getBoolType(),
        state.equalEqual(rightOperand.state).logicalNot());
  }

  /**
   * Return the result of converting this object to a String.
   * 
   * @param typeProvider the type provider used to find known types
   * @return the result of converting this object to a String
   * @throws EvaluationException if the object cannot be converted to a String
   */
  public DartObjectImpl performToString(TypeProvider typeProvider) throws EvaluationException {
    InterfaceType stringType = typeProvider.getStringType();
    if (type == stringType) {
      return this;
    }
    return new DartObjectImpl(stringType, state.convertToString());
  }

  /**
   * Return the result of invoking the '%' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '%' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl remainder(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    InstanceState result = state.remainder(rightOperand.state);
    if (result instanceof IntState) {
      return new DartObjectImpl(typeProvider.getIntType(), result);
    } else if (result instanceof DoubleState) {
      return new DartObjectImpl(typeProvider.getDoubleType(), result);
    } else if (result instanceof NumState) {
      return new DartObjectImpl(typeProvider.getNumType(), result);
    }
    // We should never get here.
    throw new IllegalStateException("remainder returned a " + result.getClass().getName());
  }

  /**
   * Return the result of invoking the '&lt;&lt;' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&lt;&lt;' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl shiftLeft(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getIntType(), state.shiftLeft(rightOperand.state));
  }

  /**
   * Return the result of invoking the '&gt;&gt;' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '&gt;&gt;' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl shiftRight(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    return new DartObjectImpl(typeProvider.getIntType(), state.shiftRight(rightOperand.state));
  }

  /**
   * Return the result of invoking the 'length' getter on this object.
   * 
   * @param typeProvider the type provider used to find known types
   * @return the result of invoking the 'length' getter on this object
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl stringLength(TypeProvider typeProvider) throws EvaluationException {
    return new DartObjectImpl(typeProvider.getIntType(), state.stringLength());
  }

  /**
   * Return the result of invoking the '*' operator on this object with the given argument.
   * 
   * @param typeProvider the type provider used to find known types
   * @param rightOperand the right-hand operand of the operation
   * @return the result of invoking the '*' operator on this object with the given argument
   * @throws EvaluationException if the operator is not appropriate for an object of this kind
   */
  public DartObjectImpl times(TypeProvider typeProvider, DartObjectImpl rightOperand)
      throws EvaluationException {
    InstanceState result = state.times(rightOperand.state);
    if (result instanceof IntState) {
      return new DartObjectImpl(typeProvider.getIntType(), result);
    } else if (result instanceof DoubleState) {
      return new DartObjectImpl(typeProvider.getDoubleType(), result);
    } else if (result instanceof NumState) {
      return new DartObjectImpl(typeProvider.getNumType(), result);
    }
    // We should never get here.
    throw new IllegalStateException("times returned a " + result.getClass().getName());
  }

  @Override
  public String toString() {
    return type.getDisplayName() + " (" + state.toString() + ")";
  }
}
