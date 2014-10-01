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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.TypeProvider;

/**
 * Instances of the class {@code DartObjectComputer} contain methods for manipulating instances of a
 * Dart class and for collecting errors during evaluation.
 */
public class DartObjectComputer {
  /**
   * The error reporter that we are using to collect errors.
   */
  private ErrorReporter errorReporter;

  /**
   * The type provider. Used to create objects of the appropriate types, and to identify when an
   * object is of a built-in type.
   */
  private TypeProvider typeProvider;

  public DartObjectComputer(ErrorReporter errorReporter, TypeProvider typeProvider) {
    this.errorReporter = errorReporter;
    this.typeProvider = typeProvider;
  }

  public DartObjectImpl add(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.add(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
        return null;
      }
    }
    return null;
  }

  /**
   * Return the result of applying boolean conversion to this result.
   * 
   * @param node the node against which errors should be reported
   * @return the result of applying boolean conversion to the given value
   */
  public DartObjectImpl applyBooleanConversion(AstNode node, DartObjectImpl evaluationResult) {
    if (evaluationResult != null) {
      try {
        return evaluationResult.convertToBool(typeProvider);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl bitAnd(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.bitAnd(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl bitNot(Expression node, DartObjectImpl evaluationResult) {
    if (evaluationResult != null) {
      try {
        return evaluationResult.bitNot(typeProvider);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl bitOr(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.bitOr(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl bitXor(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.bitXor(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl concatenate(Expression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.concatenate(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl divide(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.divide(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl equalEqual(Expression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.equalEqual(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl greaterThan(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.greaterThan(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl greaterThanOrEqual(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.greaterThanOrEqual(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl integerDivide(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.integerDivide(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl lessThan(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.lessThan(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl lessThanOrEqual(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.lessThanOrEqual(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl logicalAnd(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.logicalAnd(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl logicalNot(Expression node, DartObjectImpl evaluationResult) {
    if (evaluationResult != null) {
      try {
        return evaluationResult.logicalNot(typeProvider);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl logicalOr(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.logicalOr(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl minus(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.minus(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl negated(Expression node, DartObjectImpl evaluationResult) {
    if (evaluationResult != null) {
      try {
        return evaluationResult.negated(typeProvider);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl notEqual(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.notEqual(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl performToString(AstNode node, DartObjectImpl evaluationResult) {
    if (evaluationResult != null) {
      try {
        return evaluationResult.performToString(typeProvider);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl remainder(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.remainder(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl shiftLeft(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.shiftLeft(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  public DartObjectImpl shiftRight(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.shiftRight(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }

  /**
   * Return the result of invoking the 'length' getter on this result.
   * 
   * @param node the node against which errors should be reported
   * @return the result of invoking the 'length' getter on this result
   */
  public EvaluationResultImpl stringLength(Expression node, EvaluationResultImpl evaluationResult) {
    if (evaluationResult.getValue() != null) {
      try {
        return new EvaluationResultImpl(evaluationResult.getValue().stringLength(typeProvider));
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return new EvaluationResultImpl(null);
  }

  public DartObjectImpl times(BinaryExpression node, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) {
    if (leftOperand != null && rightOperand != null) {
      try {
        return leftOperand.times(typeProvider, rightOperand);
      } catch (EvaluationException exception) {
        errorReporter.reportErrorForNode(exception.getErrorCode(), node);
      }
    }
    return null;
  }
}
