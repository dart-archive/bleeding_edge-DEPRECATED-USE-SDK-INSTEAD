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

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.resolver.TypeProvider;

/**
 * Instances of the class {@code InternalResult} represent the result of attempting to evaluate a
 * expression.
 */
public class EvaluationResultImpl {
  /**
   * The errors encountered while trying to evaluate the compile time constant. These errors may or
   * may not have prevented the expression from being a valid compile time constant.
   */
  private AnalysisError[] errors;

  /**
   * The value of the expression, or null if the value couldn't be computed due to errors.
   */
  private final DartObjectImpl value;

  public EvaluationResultImpl(DartObjectImpl value) {
    this.value = value;
    this.errors = new AnalysisError[0];
  }

  public EvaluationResultImpl(DartObjectImpl value, AnalysisError[] errors) {
    this.value = value;
    this.errors = errors;
  }

  public boolean equalValues(TypeProvider typeProvider, EvaluationResultImpl result) {
    if (this.value != null) {
      if (result.getValue() == null) {
        return false;
      }
      return value.equals(result.value);
    } else {
      return false;
    }
  }

  public AnalysisError[] getErrors() {
    return errors;
  }

  public DartObjectImpl getValue() {
    return value;
  }

  @Override
  public String toString() {
    if (value == null) {
      return "error";
    }
    return value.toString();
  }
}
