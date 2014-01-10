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

/**
 * The unique instance of the class {@code NullState} represents the state of the value 'null'.
 */
public class NullState extends InstanceState {
  /**
   * An instance representing the boolean value 'true'.
   */
  public static final NullState NULL_STATE = new NullState();

  /**
   * Initialize a newly created state to represent the value 'null'.
   */
  private NullState() {
    super();
  }

  @Override
  public BoolState convertToBool() throws EvaluationException {
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public StringState convertToString() {
    return new StringState("null");
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    assertBoolNumStringOrNull(rightOperand);
    if (rightOperand instanceof DynamicState) {
      return BoolState.UNKNOWN_VALUE;
    }
    return BoolState.from(rightOperand instanceof NullState);
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof NullState;
  }

  @Override
  public String getTypeName() {
    return "Null"; //$NON-NLS-0$
  }

  @Override
  public boolean hasExactValue() {
    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean isBoolNumStringOrNull() {
    return true;
  }

  @Override
  public BoolState logicalNot() throws EvaluationException {
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public String toString() {
    return "null";
  }
}
