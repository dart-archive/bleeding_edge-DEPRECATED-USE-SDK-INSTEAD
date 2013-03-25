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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;

import java.math.BigInteger;

/**
 * Instances of the class {@code ValidResult} represent the result of attempting to evaluate a valid
 * compile time constant expression.
 */
public class ValidResult extends EvaluationResultImpl {
  /**
   * A result object representing the value 'false'.
   */
  public static final ValidResult RESULT_FALSE = new ValidResult(Boolean.FALSE);

  /**
   * A result object representing the an arbitrary object on which no further operations can be
   * performed.
   */
  public static final ValidResult RESULT_OBJECT = new ValidResult(new Object());

  /**
   * A result object representing the value 'true'.
   */
  public static final ValidResult RESULT_TRUE = new ValidResult(Boolean.TRUE);

  /**
   * The value of the expression.
   */
  private Object value;

  /**
   * Initialize a newly created result to represent the given value.
   * 
   * @param value the value of the expression
   */
  public ValidResult(Object value) {
    this.value = value;
  }

  @Override
  public EvaluationResultImpl add(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.addToValid(node, this);
  }

  @Override
  public EvaluationResultImpl bitAnd(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.bitAndValid(node, this);
  }

  @Override
  public EvaluationResultImpl bitNot(Expression node) {
    if (value == null) {
      return error(node);
    } else if (value instanceof BigInteger) {
      return valueOf(((BigInteger) value).not());
    }
    return error(node);
  }

  @Override
  public EvaluationResultImpl bitOr(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.bitOrValid(node, this);
  }

  @Override
  public EvaluationResultImpl bitXor(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.bitXorValid(node, this);
  }

  @Override
  public EvaluationResultImpl concatenate(Expression node, EvaluationResultImpl rightOperand) {
    return rightOperand.concatenateValid(node, this);
  }

  @Override
  public EvaluationResultImpl divide(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.divideValid(node, this);
  }

  @Override
  public EvaluationResultImpl equalEqual(Expression node, EvaluationResultImpl rightOperand) {
    return rightOperand.equalEqualValid(node, this);
  }

  public Object getValue() {
    return value;
  }

  @Override
  public EvaluationResultImpl greaterThan(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.greaterThanValid(node, this);
  }

  @Override
  public EvaluationResultImpl greaterThanOrEqual(BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.greaterThanOrEqualValid(node, this);
  }

  @Override
  public EvaluationResultImpl integerDivide(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.integerDivideValid(node, this);
  }

  @Override
  public EvaluationResultImpl lessThan(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.lessThanValid(node, this);
  }

  @Override
  public EvaluationResultImpl lessThanOrEqual(BinaryExpression node,
      EvaluationResultImpl rightOperand) {
    return rightOperand.lessThanOrEqualValid(node, this);
  }

  @Override
  public EvaluationResultImpl logicalAnd(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.logicalAndValid(node, this);
  }

  @Override
  public EvaluationResultImpl logicalNot(Expression node) {
    if (value == null) {
      return RESULT_TRUE;
    } else if (value instanceof Boolean) {
      return ((Boolean) value) ? RESULT_FALSE : RESULT_TRUE;
    }
    return error(node);
  }

  @Override
  public EvaluationResultImpl logicalOr(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.logicalOrValid(node, this);
  }

  @Override
  public EvaluationResultImpl minus(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.minusValid(node, this);
  }

  @Override
  public EvaluationResultImpl negated(Expression node) {
    if (value == null) {
      return error(node);
    } else if (value instanceof BigInteger) {
      return valueOf(((BigInteger) value).negate());
    } else if (value instanceof Double) {
      return valueOf(-((Double) value).doubleValue());
    }
    return error(node);
  }

  @Override
  public EvaluationResultImpl notEqual(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.notEqualValid(node, this);
  }

  @Override
  public EvaluationResultImpl performToString(ASTNode node) {
    if (value == null) {
      return valueOf("null");
    } else if (value instanceof Boolean) {
      return valueOf(((Boolean) value).toString());
    } else if (value instanceof BigInteger) {
      return valueOf(((BigInteger) value).toString());
    } else if (value instanceof Double) {
      return valueOf(((Double) value).toString());
    } else if (value instanceof String) {
      return this;
    }
    return error(node);
  }

  @Override
  public EvaluationResultImpl remainder(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.remainderValid(node, this);
  }

  @Override
  public EvaluationResultImpl shiftLeft(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.shiftLeftValid(node, this);
  }

  @Override
  public EvaluationResultImpl shiftRight(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.shiftRightValid(node, this);
  }

  @Override
  public EvaluationResultImpl times(BinaryExpression node, EvaluationResultImpl rightOperand) {
    return rightOperand.timesValid(node, this);
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
  protected EvaluationResultImpl addToValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).add((BigInteger) value));
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() + ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() + ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() + ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof String) {
      if (value instanceof String) {
        return valueOf(((String) leftValue) + ((String) value));
      }
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl bitAndError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl bitAndValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).and((BigInteger) value));
      }
      return error(node.getLeftOperand());
    }
    if (value instanceof BigInteger) {
      return error(node.getRightOperand());
    }
    return union(error(node.getLeftOperand()), error(node.getRightOperand()));
  }

  @Override
  protected EvaluationResultImpl bitOrError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl bitOrValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).or((BigInteger) value));
      }
      return error(node.getLeftOperand());
    }
    if (value instanceof BigInteger) {
      return error(node.getRightOperand());
    }
    return union(error(node.getLeftOperand()), error(node.getRightOperand()));
  }

  @Override
  protected EvaluationResultImpl bitXorError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl bitXorValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).xor((BigInteger) value));
      }
      return error(node.getLeftOperand());
    }
    if (value instanceof BigInteger) {
      return error(node.getRightOperand());
    }
    return union(error(node.getLeftOperand()), error(node.getRightOperand()));
  }

  @Override
  protected EvaluationResultImpl concatenateError(Expression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl concatenateValid(Expression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue instanceof String && value instanceof String) {
      return valueOf(((String) leftValue) + ((String) value));
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl divideError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl divideValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        if (((BigInteger) value).equals(BigInteger.ZERO)) {
          return valueOf(Double.valueOf(((BigInteger) leftValue).doubleValue()
              / ((BigInteger) value).doubleValue()));
        }
        return valueOf(((BigInteger) leftValue).divide((BigInteger) value));
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() / ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() / ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() / ((Double) value).doubleValue());
      }
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl equalEqualError(Expression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl equalEqualValid(Expression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return valueOf(value == null);
    } else if (leftValue instanceof Boolean) {
      if (value instanceof Boolean) {
        return valueOf(((Boolean) leftValue).booleanValue() == ((Boolean) value).booleanValue());
      }
      return RESULT_FALSE;
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).equals(value));
      } else if (value instanceof Double) {
        return valueOf(toDouble((BigInteger) leftValue).equals(value));
      }
      return RESULT_FALSE;
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).equals(toDouble((BigInteger) value)));
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).equals(value));
      }
      return RESULT_FALSE;
    } else if (leftValue instanceof String) {
      if (value instanceof String) {
        return valueOf(((String) leftValue).equals(value));
      }
      return RESULT_FALSE;
    }
    return RESULT_FALSE;
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
  protected EvaluationResultImpl greaterThanOrEqualValid(BinaryExpression node,
      ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).compareTo((BigInteger) value) >= 0);
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() >= ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() >= ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() >= ((Double) value).doubleValue());
      }
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl greaterThanValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).compareTo((BigInteger) value) > 0);
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() > ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() > ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() > ((Double) value).doubleValue());
      }
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl integerDivideError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl integerDivideValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        if (((BigInteger) value).equals(BigInteger.ZERO)) {
          return valueOf(Double.valueOf(((BigInteger) leftValue).doubleValue()
              / ((BigInteger) value).doubleValue()));
        }
        return valueOf(((BigInteger) leftValue).divide((BigInteger) value));
      } else if (value instanceof Double) {
        double result = ((BigInteger) leftValue).doubleValue() / ((Double) value).doubleValue();
        return valueOf(BigInteger.valueOf((long) result));
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        double result = ((Double) leftValue).doubleValue() / ((BigInteger) value).doubleValue();
        return valueOf(BigInteger.valueOf((long) result));
      } else if (value instanceof Double) {
        double result = ((Double) leftValue).doubleValue() / ((Double) value).doubleValue();
        return valueOf(BigInteger.valueOf((long) result));
      }
    }
    return error(node);
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
  protected EvaluationResultImpl lessThanOrEqualValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).compareTo((BigInteger) value) <= 0);
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() <= ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() <= ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() <= ((Double) value).doubleValue());
      }
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl lessThanValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).compareTo((BigInteger) value) < 0);
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() < ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() < ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() < ((Double) value).doubleValue());
      }
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl logicalAndError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl logicalAndValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue instanceof Boolean && ((Boolean) leftValue).booleanValue()) {
      return booleanConversion(node.getRightOperand(), value);
    }
    return RESULT_FALSE;
  }

  @Override
  protected EvaluationResultImpl logicalOrError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl logicalOrValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue instanceof Boolean && ((Boolean) leftValue).booleanValue()) {
      return RESULT_TRUE;
    }
    return booleanConversion(node.getRightOperand(), value);
  }

  @Override
  protected EvaluationResultImpl minusError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl minusValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).subtract((BigInteger) value));
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() - ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() - ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() - ((Double) value).doubleValue());
      }
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl notEqualError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl notEqualValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return valueOf(value != null);
    } else if (leftValue instanceof Boolean) {
      if (value instanceof Boolean) {
        return valueOf(((Boolean) leftValue).booleanValue() != ((Boolean) value).booleanValue());
      }
      return RESULT_TRUE;
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(!((BigInteger) leftValue).equals(value));
      } else if (value instanceof Double) {
        return valueOf(!toDouble((BigInteger) leftValue).equals(value));
      }
      return RESULT_TRUE;
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(!((Double) leftValue).equals(toDouble((BigInteger) value)));
      } else if (value instanceof Double) {
        return valueOf(!((Double) leftValue).equals(value));
      }
      return RESULT_TRUE;
    } else if (leftValue instanceof String) {
      if (value instanceof String) {
        return valueOf(!((String) leftValue).equals(value));
      }
      return RESULT_TRUE;
    }
    return RESULT_TRUE;
  }

  @Override
  protected EvaluationResultImpl remainderError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl remainderValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        if (((BigInteger) value).equals(BigInteger.ZERO)) {
          return valueOf(Double.valueOf(((BigInteger) leftValue).doubleValue()
              % ((BigInteger) value).doubleValue()));
        }
        return valueOf(((BigInteger) leftValue).remainder((BigInteger) value));
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() % ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() % ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() % ((Double) value).doubleValue());
      }
    }
    return error(node);
  }

  @Override
  protected EvaluationResultImpl shiftLeftError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl shiftLeftValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).shiftLeft(((BigInteger) value).intValue()));
      }
      return error(node.getRightOperand());
    }
    if (value instanceof BigInteger) {
      return error(node.getLeftOperand());
    }
    return union(error(node.getLeftOperand()), error(node.getRightOperand()));
  }

  @Override
  protected EvaluationResultImpl shiftRightError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl shiftRightValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).shiftRight(((BigInteger) value).intValue()));
      }
      return error(node.getRightOperand());
    }
    if (value instanceof BigInteger) {
      return error(node.getLeftOperand());
    }
    return union(error(node.getLeftOperand()), error(node.getRightOperand()));
  }

  @Override
  protected EvaluationResultImpl timesError(BinaryExpression node, ErrorResult leftOperand) {
    return leftOperand;
  }

  @Override
  protected EvaluationResultImpl timesValid(BinaryExpression node, ValidResult leftOperand) {
    Object leftValue = leftOperand.getValue();
    if (leftValue == null) {
      return error(node.getLeftOperand());
    } else if (value == null) {
      return error(node.getRightOperand());
    } else if (leftValue instanceof BigInteger) {
      if (value instanceof BigInteger) {
        return valueOf(((BigInteger) leftValue).multiply((BigInteger) value));
      } else if (value instanceof Double) {
        return valueOf(((BigInteger) leftValue).doubleValue() * ((Double) value).doubleValue());
      }
    } else if (leftValue instanceof Double) {
      if (value instanceof BigInteger) {
        return valueOf(((Double) leftValue).doubleValue() * ((BigInteger) value).doubleValue());
      } else if (value instanceof Double) {
        return valueOf(((Double) leftValue).doubleValue() * ((Double) value).doubleValue());
      }
    }
    return error(node);
  }

  /**
   * Return the result of applying boolean conversion to the given value.
   * 
   * @param node the node against which errors should be reported
   * @param value the value to be converted to a boolean
   * @return the result of applying boolean conversion to the given value
   */
  private EvaluationResultImpl booleanConversion(ASTNode node, Object value) {
    if (value == null) {
      return error(node);
    } else if (value instanceof Boolean && ((Boolean) value).booleanValue()) {
      return RESULT_TRUE;
    }
    return RESULT_FALSE;
  }

  private ErrorResult error(ASTNode node) {
    // TODO(brianwilkerson) Remove this method
    return error(node, CompileTimeErrorCode.INVALID_CONSTANT);
  }

  /**
   * Return a result object representing an error associated with the given node.
   * 
   * @param node the AST node associated with the error
   * @param code the error code indicating the nature of the error
   * @return a result object representing an error associated with the given node
   */
  private ErrorResult error(ASTNode node, ErrorCode code) {
    return new ErrorResult(node, code);
  }

  private Double toDouble(BigInteger value) {
    return value.doubleValue();
  }

  /**
   * Return an error result that is the union of the two given error results.
   * 
   * @param firstError the first error to be combined
   * @param secondError the second error to be combined
   * @return an error result that is the union of the two given error results
   */
  private ErrorResult union(ErrorResult firstError, ErrorResult secondError) {
    return new ErrorResult(firstError, secondError);
  }

  /**
   * Return a result object representing the given value.
   * 
   * @param value the value to be represented as a result object
   * @return a result object representing the given value
   */
  private ValidResult valueOf(BigInteger value) {
    return new ValidResult(value);
  }

  /**
   * Return a result object representing the given value.
   * 
   * @param value the value to be represented as a result object
   * @return a result object representing the given value
   */
  private ValidResult valueOf(boolean value) {
    return value ? RESULT_TRUE : RESULT_FALSE;
  }

  /**
   * Return a result object representing the given value.
   * 
   * @param value the value to be represented as a result object
   * @return a result object representing the given value
   */
  private ValidResult valueOf(Double value) {
    return new ValidResult(value);
  }

  /**
   * Return a result object representing the given value.
   * 
   * @param value the value to be represented as a result object
   * @return a result object representing the given value
   */
  private ValidResult valueOf(String value) {
    return new ValidResult(value);
  }
}
