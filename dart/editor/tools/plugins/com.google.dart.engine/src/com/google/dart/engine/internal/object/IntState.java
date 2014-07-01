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
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.math.BigInteger;

/**
 * Instances of the class {@code IntState} represent the state of an object representing an int.
 */
public class IntState extends NumState {
  /**
   * The value of this instance.
   */
  private BigInteger value;

  /**
   * A state that can be used to represent an int whose value is not known.
   */
  public static final IntState UNKNOWN_VALUE = new IntState(null);

  /**
   * Initialize a newly created state to represent an int with the given value.
   * 
   * @param value the value of this instance
   */
  public IntState(BigInteger value) {
    this.value = value;
  }

  @Override
  public NumState add(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      if (rightOperand instanceof DoubleState) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      return new IntState(value.add(rightValue));
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return new DoubleState(value.doubleValue() + rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public IntState bitAnd(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      return new IntState(value.and(rightValue));
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public IntState bitNot() throws EvaluationException {
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    return new IntState(value.not());
  }

  @Override
  public IntState bitOr(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      return new IntState(value.or(rightValue));
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public IntState bitXor(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      return new IntState(value.xor(rightValue));
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public StringState convertToString() {
    if (value == null) {
      return StringState.UNKNOWN_VALUE;
    }
    return new StringState(value.toString());
  }

  @Override
  public NumState divide(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      if (rightOperand instanceof DoubleState) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      } else if (rightValue.equals(BigInteger.ZERO)) {
        return new DoubleState(value.doubleValue() / rightValue.doubleValue());
      }
      return new IntState(value.divide(rightValue));
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return new DoubleState(value.doubleValue() / rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    assertBoolNumStringOrNull(rightOperand);
    if (value == null) {
      return BoolState.UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.equals(rightValue));
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(rightValue.equals(value.doubleValue()));
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return BoolState.UNKNOWN_VALUE;
    }
    return BoolState.FALSE_STATE;
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof IntState && ObjectUtilities.equals(value, ((IntState) object).value);
  }

  @Override
  public String getTypeName() {
    return "int"; //$NON-NLS-0$
  }

  @Override
  public BigInteger getValue() {
    return value;
  }

  @Override
  public BoolState greaterThan(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      return BoolState.UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.compareTo(rightValue) > 0);
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.doubleValue() > rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return BoolState.UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public BoolState greaterThanOrEqual(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      return BoolState.UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.compareTo(rightValue) >= 0);
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.doubleValue() >= rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return BoolState.UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
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
  public IntState integerDivide(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      } else if (rightValue.equals(BigInteger.ZERO)) {
        throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_IDBZE);
      }
      return new IntState(value.divide(rightValue));
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      double result = value.doubleValue() / rightValue.doubleValue();
      return new IntState(BigInteger.valueOf((long) result));
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
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
  public BoolState lessThan(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      return BoolState.UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.compareTo(rightValue) < 0);
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.doubleValue() < rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return BoolState.UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public BoolState lessThanOrEqual(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      return BoolState.UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.compareTo(rightValue) <= 0);
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(value.doubleValue() <= rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return BoolState.UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public NumState minus(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      if (rightOperand instanceof DoubleState) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      return new IntState(value.subtract(rightValue));
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return new DoubleState(value.doubleValue() - rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public NumState negated() throws EvaluationException {
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    return new IntState(value.negate());
  }

  @Override
  public NumState remainder(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      if (rightOperand instanceof DoubleState) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      } else if (rightValue.equals(BigInteger.ZERO)) {
        return new DoubleState(value.doubleValue() % rightValue.doubleValue());
      }
      return new IntState(value.remainder(rightValue));
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return new DoubleState(value.doubleValue() % rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public IntState shiftLeft(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      } else if (rightValue.bitLength() > 31) {
        return UNKNOWN_VALUE;
      }
      return new IntState(value.shiftLeft(rightValue.intValue()));
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public IntState shiftRight(InstanceState rightOperand) throws EvaluationException {
    assertIntOrNull(rightOperand);
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      } else if (rightValue.bitLength() > 31) {
        return UNKNOWN_VALUE;
      }
      return new IntState(value.shiftRight(rightValue.intValue()));
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public NumState times(InstanceState rightOperand) throws EvaluationException {
    assertNumOrNull(rightOperand);
    if (value == null) {
      if (rightOperand instanceof DoubleState) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return UNKNOWN_VALUE;
    }
    if (rightOperand instanceof IntState) {
      BigInteger rightValue = ((IntState) rightOperand).getValue();
      if (rightValue == null) {
        return UNKNOWN_VALUE;
      }
      return new IntState(value.multiply(rightValue));
    } else if (rightOperand instanceof DoubleState) {
      Double rightValue = ((DoubleState) rightOperand).getValue();
      if (rightValue == null) {
        return DoubleState.UNKNOWN_VALUE;
      }
      return new DoubleState(value.doubleValue() * rightValue.doubleValue());
    } else if (rightOperand instanceof DynamicState || rightOperand instanceof NumState) {
      return UNKNOWN_VALUE;
    }
    throw new EvaluationException(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
  }

  @Override
  public String toString() {
    return value == null ? "-unknown-" : value.toString();
  }
}
