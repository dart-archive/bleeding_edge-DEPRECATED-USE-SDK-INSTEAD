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

/**
 * Instances of the class {@code BoolState} represent the state of an object representing a boolean
 * value.
 */
public class BoolState extends InstanceState {
  /**
   * The value of this instance.
   */
  private Boolean value;

  /**
   * An instance representing the boolean value 'false'.
   */
  public static final BoolState FALSE_STATE = new BoolState(Boolean.FALSE);

  /**
   * An instance representing the boolean value 'true'.
   */
  public static final BoolState TRUE_STATE = new BoolState(Boolean.TRUE);

  /**
   * A state that can be used to represent a boolean whose value is not known.
   */
  public static final BoolState UNKNOWN_VALUE = new BoolState(null);

  /**
   * Return the boolean state representing the given boolean value.
   * 
   * @param value the value to be represented
   * @return the boolean state representing the given boolean value
   */
  public static BoolState from(boolean value) {
    return value ? BoolState.TRUE_STATE : BoolState.FALSE_STATE;
  }

  /**
   * Initialize a newly created state to represent the given value.
   * 
   * @param value the value of this instance
   */
  private BoolState(Boolean value) {
    this.value = value;
  }

  @Override
  public BoolState convertToBool() {
    return this;
  }

  @Override
  public StringState convertToString() {
    if (value == null) {
      return StringState.UNKNOWN_VALUE;
    }
    return new StringState(value ? "true" : "false");
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    assertBoolNumStringOrNull(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof BoolState) {
      Boolean rightValue = ((BoolState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      return BoolState.from(value == rightValue);
    } else if (rightOperand instanceof DynamicState) {
      return UNKNOWN_VALUE;
    }
    return FALSE_STATE;
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof BoolState && value == ((BoolState) object).value;
  }

  @Override
  public String getTypeName() {
    return "bool"; //$NON-NLS-0$
  }

  @Override
  public Boolean getValue() {
    return value;
  }

  @Override
  public boolean hasExactValue() {
    return true;
  }

  @Override
  public int hashCode() {
    return value == null ? 0 : (value ? 2 : 3);
  }

  /**
   * Return {@code true} if this object represents an object whose type is 'bool'.
   * 
   * @return {@code true} if this object represents a boolean value
   */
  @Override
  public boolean isBool() {
    return true;
  }

  @Override
  public boolean isBoolNumStringOrNull() {
    return true;
  }

  @Override
  public BoolState logicalAnd(InstanceState rightOperand) throws EvaluationException {
    assertBool(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    return value ? rightOperand.convertToBool() : FALSE_STATE;
  }

  @Override
  public BoolState logicalNot() {
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    return value ? FALSE_STATE : TRUE_STATE;
  }

  @Override
  public BoolState logicalOr(InstanceState rightOperand) throws EvaluationException {
    assertBool(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    return value ? TRUE_STATE : rightOperand.convertToBool();
  }

  @Override
  public String toString() {
    return value == null ? "-unknown-" : (value ? "true" : "false");
  }
}
