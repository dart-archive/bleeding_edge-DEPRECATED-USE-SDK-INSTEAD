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

import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.math.BigInteger;

/**
 * Instances of the class {@code StringState} represent the state of an object representing a
 * string.
 */
public class StringState extends InstanceState {
  /**
   * The value of this instance.
   */
  private String value;

  /**
   * A state that can be used to represent a double whose value is not known.
   */
  public static final StringState UNKNOWN_VALUE = new StringState(null);

  /**
   * Initialize a newly created state to represent the given value.
   * 
   * @param value the value of this instance
   */
  public StringState(String value) {
    this.value = value;
  }

  @Override
  public StringState concatenate(InstanceState rightOperand) throws EvaluationException {
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof StringState) {
      String rightValue = ((StringState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      return new StringState(value + rightValue);
    } else if (rightOperand instanceof DynamicState) {
      return UNKNOWN_VALUE;
    }
    return super.concatenate(rightOperand);
  }

  @Override
  public StringState convertToString() {
    return this;
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    assertBoolNumStringOrNull(rightOperand);
    if (value == null) {
      return BoolState.UNKNOWN_VALUE;
    }
    if (rightOperand instanceof StringState) {
      String rightValue = ((StringState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.equals(rightValue));
    } else if (rightOperand instanceof DynamicState) {
      return BoolState.UNKNOWN_VALUE;
    }
    return BoolState.FALSE_STATE;
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof StringState
        && ObjectUtilities.equals(value, ((StringState) object).value);
  }

  @Override
  public String getTypeName() {
    return "String"; //$NON-NLS-0$
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public boolean hasExactValue() {
    return true;
  }

  @Override
  public int hashCode() {
    return value == null ? 0 : value.hashCode();
  }

  @Override
  public boolean isBoolNumStringOrNull() {
    return true;
  }

  @Override
  public boolean isUnknown() {
    return value == null;
  }

  @Override
  public IntState stringLength() throws EvaluationException {
    if (value == null) {
      return IntState.UNKNOWN_VALUE;
    }
    return new IntState(BigInteger.valueOf(value.length()));
  }

  @Override
  public String toString() {
    return value == null ? "-unknown-" : "'" + value + "'";
  }
}
