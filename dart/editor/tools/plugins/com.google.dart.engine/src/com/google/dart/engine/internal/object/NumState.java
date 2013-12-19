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

import java.math.BigInteger;

/**
 * Instances of the class {@code NumState} represent the state of an object representing a number of
 * an unknown type (a 'num').
 */
public class NumState extends InstanceState {
  /**
   * A state that can be used to represent a number whose value is not known.
   */
  public static final NumState UNKNOWN_VALUE = new NumState();

  /**
   * Initialize a newly created state to represent a number. Clients should use the value of the
   * static constant {@link #UNKNOWN_VALUE} rather than creating new instances of this class.
   */
  public NumState() {
    super();
  }

  @Override
  public NumState add(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return UNKNOWN_VALUE;
  }

  @Override
  public StringState convertToString() {
    return StringState.UNKNOWN_VALUE;
  }

  @Override
  public NumState divide(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return UNKNOWN_VALUE;
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof NumState;
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    assertBoolNumStringOrNull(rightOperand);
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public String getTypeName() {
    return "num"; //$NON-NLS-0$
  }

  @Override
  public BoolState greaterThan(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public BoolState greaterThanOrEqual(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public int hashCode() {
    return 7;
  }

  @Override
  public IntState integerDivide(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return IntState.UNKNOWN_VALUE;
      } else if (rightValue.equals(BigInteger.ZERO)) {
        throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_IDBZE);
      }
    } else if (rightOperand instanceof DynamicState) {
      return IntState.UNKNOWN_VALUE;
    }
    return IntState.UNKNOWN_VALUE;
  }

  @Override
  public boolean isBoolNumStringOrNull() {
    return true;
  }

  @Override
  public BoolState lessThan(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public BoolState lessThanOrEqual(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public NumState minus(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return UNKNOWN_VALUE;
  }

  @Override
  public NumState negated() throws EvaluationException {
    return UNKNOWN_VALUE;
  }

  @Override
  public NumState remainder(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return UNKNOWN_VALUE;
  }

  @Override
  public NumState times(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return UNKNOWN_VALUE;
  }

  @Override
  public String toString() {
    return "-unknown-";
  }
}
