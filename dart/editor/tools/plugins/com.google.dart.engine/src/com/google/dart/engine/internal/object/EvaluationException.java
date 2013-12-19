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

import com.google.dart.engine.error.ErrorCode;

/**
 * Instances of the class {@code EvaluationException} represent a run-time exception that would be
 * thrown during the evaluation of Dart code.
 */
public class EvaluationException extends Exception {
  /**
   * The error code associated with the exception.
   */
  private ErrorCode errorCode;

  /**
   * Initialize a newly created exception to have the given error code.
   * 
   * @param errorCode the error code associated with the exception
   */
  public EvaluationException(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Return the error code associated with the exception, or {@code null} if there is no associated
   * error code.
   * 
   * @return the error code associated with the exception
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
