/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server;


/**
 * Instances of the class {@code AnalysisServerError} represent an error reported by
 * {@link AnalysisServer}.
 * 
 * @coverage dart.server
 */
public class AnalysisServerError {
  /**
   * The error code associated with the error.
   */
  private final AnalysisServerErrorCode errorCode;

  /**
   * The error message.
   */
  private final String message;

  /**
   * Initialize a newly created analysis error for the specified source. The error has no location
   * information.
   * 
   * @param source the source for which the exception occurred
   * @param errorCode the error code to be associated with this error
   * @param arguments the arguments used to build the error message
   */
  public AnalysisServerError(AnalysisServerErrorCode errorCode, Object... arguments) {
    this.errorCode = errorCode;
    this.message = String.format(errorCode.getMessage(), arguments);
  }

  /**
   * Return the error code associated with the error.
   * 
   * @return the error code associated with the error
   */
  public AnalysisServerErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Return the message to be displayed for this error. The message should indicate what is wrong
   * and why it is wrong.
   * 
   * @return the message to be displayed for this error
   */
  public String getMessage() {
    return message;
  }
}
