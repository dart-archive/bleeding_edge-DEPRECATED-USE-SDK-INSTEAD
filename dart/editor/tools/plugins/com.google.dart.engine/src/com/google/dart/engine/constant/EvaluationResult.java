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
package com.google.dart.engine.constant;

import com.google.dart.engine.error.AnalysisError;

/**
 * Instances of the class {@code EvaluationResult} represent the result of attempting to evaluate an
 * expression.
 */
public class EvaluationResult {
  /**
   * Return an evaluation result representing the result of evaluating an expression that is not a
   * compile-time constant because of the given errors.
   * 
   * @param errors the errors that should be reported for the expression(s) that were evaluated
   * @return the result of evaluating an expression that is not a compile-time constant
   */
  public static EvaluationResult forErrors(AnalysisError[] errors) {
    return new EvaluationResult(null, errors);
  }

  /**
   * Return an evaluation result representing the result of evaluating an expression that is a
   * compile-time constant that evaluates to the given value.
   * 
   * @param value the value of the expression
   * @return the result of evaluating an expression that is a compile-time constant
   */
  public static EvaluationResult forValue(DartObject value) {
    return new EvaluationResult(value, null);
  }

  /**
   * The value of the expression.
   */
  private DartObject value;

  /**
   * The errors that should be reported for the expression(s) that were evaluated.
   */
  private AnalysisError[] errors;

  /**
   * Initialize a newly created result object with the given state. Clients should use one of the
   * factory methods: {@link #forErrors(AnalysisError[])} and {@link #forValue(Object)}.
   * 
   * @param value the value of the expression
   * @param errors the errors that should be reported for the expression(s) that were evaluated
   */
  private EvaluationResult(DartObject value, AnalysisError[] errors) {
    this.value = value;
    this.errors = errors;
  }

  /**
   * Return an array containing the errors that should be reported for the expression(s) that were
   * evaluated. If there are no such errors, the array will be empty. The array can be empty even if
   * the expression is not a valid compile time constant if the errors would have been reported by
   * other parts of the analysis engine.
   */
  public AnalysisError[] getErrors() {
    return errors == null ? AnalysisError.NO_ERRORS : errors;
  }

  /**
   * Return the value of the expression, or {@code null} if the expression evaluated to {@code null}
   * or if the expression could not be evaluated, either because it was not a compile-time constant
   * expression or because it would throw an exception when evaluated.
   * 
   * @return the value of the expression
   */
  public DartObject getValue() {
    return value;
  }

  /**
   * Return {@code true} if the expression is a compile-time constant expression that would not
   * throw an exception when evaluated.
   * 
   * @return {@code true} if the expression is a valid compile-time constant expression
   */
  public boolean isValid() {
    return errors == null;
  }
}
