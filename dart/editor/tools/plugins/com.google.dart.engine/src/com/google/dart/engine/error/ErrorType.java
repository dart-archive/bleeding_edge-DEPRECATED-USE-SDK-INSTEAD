/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.error;

/**
 * Instances of the enumeration {@code ErrorType} represent the type of an {@link ErrorCode}.
 * 
 * @coverage dart.engine.error
 */
public enum ErrorType {
  /**
   * Compile-time errors are errors that preclude execution. A compile time error must be reported
   * by a Dart compiler before the erroneous code is executed.
   */
  COMPILE_TIME_ERROR(ErrorSeverity.ERROR),

  /**
   * Static warnings are those warnings reported by the static checker. They have no effect on
   * execution. Static warnings must be provided by Dart compilers used during development.
   */
  STATIC_WARNING(ErrorSeverity.WARNING),

  /**
   * Many, but not all, static warnings relate to types, in which case they are known as static type
   * warnings.
   */
  STATIC_TYPE_WARNING(ErrorSeverity.WARNING),

  /**
   * Syntactic errors are errors produced as a result of input that does not conform to the grammar.
   */
  SYNTACTIC_ERROR(ErrorSeverity.ERROR);

  /**
   * The severity of this type of error.
   */
  private final ErrorSeverity severity;

  /**
   * Initialize a newly created error type to have the given severity.
   * 
   * @param severity the severity of this type of error
   */
  private ErrorType(ErrorSeverity severity) {
    this.severity = severity;
  }

  /**
   * Return the severity of this type of error.
   * 
   * @return the severity of this type of error
   */
  public ErrorSeverity getSeverity() {
    return severity;
  }
}
