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
 * Instances of the class {@code DynamicState} represent the state of an object representing a Dart
 * object for which there is no type information.
 */
public class DynamicState extends InstanceState {
  /**
   * The unique instance of this class.
   */
  public static final DynamicState DYNAMIC_STATE = new DynamicState();

  /**
   * Prevent the creation of instances of this class.
   */
  private DynamicState() {
    super();
  }

  @Override
  public NumState add(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return unknownNum(rightOperand);
  }

  @Override
  public IntState bitAnd(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    return IntState.UNKNOWN_VALUE;
  }

  @Override
  public IntState bitNot() throws EvaluationException {
    return IntState.UNKNOWN_VALUE;
  }

  @Override
  public IntState bitOr(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    return IntState.UNKNOWN_VALUE;
  }

  @Override
  public IntState bitXor(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    return IntState.UNKNOWN_VALUE;
  }

  @Override
  public StringState concatenate(InstanceState rightOperand) throws EvaluationException {
    assertString(rightOperand);
    return StringState.UNKNOWN_VALUE;
  }

  @Override
  public BoolState convertToBool() throws EvaluationException {
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public StringState convertToString() throws EvaluationException {
    return StringState.UNKNOWN_VALUE;
  }

  @Override
  public NumState divide(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return unknownNum(rightOperand);
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    assertBoolNumStringOrNull(rightOperand);
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public String getTypeName() {
    return "dynamic"; //$NON-NLS-0$
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
  public IntState integerDivide(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return IntState.UNKNOWN_VALUE;
  }

  @Override
  public boolean isBool() {
    return true;
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
  public BoolState logicalAnd(InstanceState rightOperand) throws EvaluationException {
    assertBool(rightOperand);
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public BoolState logicalNot() throws EvaluationException {
    return BoolState.UNKNOWN_VALUE;
  }

  @Override
  public BoolState logicalOr(InstanceState rightOperand) throws EvaluationException {
    assertBool(rightOperand);
    return rightOperand.convertToBool();
  }

  @Override
  public NumState minus(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return unknownNum(rightOperand);
  }

  @Override
  public NumState negated() throws EvaluationException {
    return NumState.UNKNOWN_VALUE;
  }

  @Override
  public NumState remainder(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return unknownNum(rightOperand);
  }

  @Override
  public IntState shiftLeft(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    return IntState.UNKNOWN_VALUE;
  }

  @Override
  public IntState shiftRight(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    return IntState.UNKNOWN_VALUE;
  }

  @Override
  public NumState times(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    return unknownNum(rightOperand);
  }

  /**
   * Return an object representing an unknown numeric value whose type is based on the type of the
   * right-hand operand.
   * 
   * @param rightOperand the operand whose type will determine the type of the result
   * @return an object representing an unknown numeric value
   */
  private NumState unknownNum(InstanceState rightOperand) {
    if (rightOperand instanceof IntState) {
      return IntState.UNKNOWN_VALUE;
    } else if (rightOperand instanceof DoubleState) {
      return DoubleState.UNKNOWN_VALUE;
    }
    return NumState.UNKNOWN_VALUE;
  }
}
