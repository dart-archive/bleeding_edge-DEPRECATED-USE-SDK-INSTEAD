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

/**
 * Instances of the class {@code StringState} represent the state of an object representing a
 * symbol.
 */
public class SymbolState extends InstanceState {
  /**
   * The value of this instance.
   */
  private String value;

  /**
   * Initialize a newly created state to represent the given value.
   * 
   * @param value the value of this instance
   */
  public SymbolState(String value) {
    this.value = value;
  }

  @Override
  public StringState convertToString() {
    if (value == null) {
      return StringState.UNKNOWN_VALUE;
    }
    return new StringState(value);
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    assertBoolNumStringOrNull(rightOperand);
    if (value == null) {
      return BoolState.UNKNOWN_VALUE;
    }
    if (rightOperand instanceof SymbolState) {
      String rightValue = ((SymbolState) rightOperand).getValue();
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
    return object instanceof SymbolState
        && ObjectUtilities.equals(value, ((SymbolState) object).value);
  }

  @Override
  public String getTypeName() {
    return "Symbol"; //$NON-NLS-0$
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
  public String toString() {
    return value == null ? "-unknown-" : "#" + value;
  }
}
