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
package com.google.dart.engine.error;

/**
 * The enumeration {@code CompileTimeErrorCode} defines the error codes used for compile time errors
 * caused by constant evaluation that would throw an exception when run in checked mode. The client
 * of the analysis engine is responsible for determining how these errors should be presented to the
 * user (for example, a command-line compiler might elect to treat these errors differently
 * depending whether it is compiling it "checked" mode).
 * 
 * @coverage dart.engine.error
 */
public enum CheckedModeCompileTimeErrorCode implements ErrorCode {
  /**
   * 7.6.1 Generative Constructors: In checked mode, it is a dynamic type error if o is not
   * <b>null</b> and the interface of the class of <i>o</i> is not a subtype of the static type of
   * the field <i>v</i>.
   * <p>
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   * 
   * @param initializerType the name of the type of the initializer expression
   * @param fieldType the name of the type of the field
   */
  CONST_FIELD_INITIALIZER_NOT_ASSIGNABLE(
      "The initializer type '%s' cannot be assigned to the field type '%s'");

  /**
   * The template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * The template used to create the correction to be displayed for this error, or {@code null} if
   * there is no correction information for this error.
   */
  public String correction;

  /**
   * Initialize a newly created error code to have the given message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private CheckedModeCompileTimeErrorCode(String message) {
    this(message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private CheckedModeCompileTimeErrorCode(String message, String correction) {
    this.message = message;
    this.correction = correction;
  }

  @Override
  public String getCorrection() {
    return correction;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return ErrorType.CHECKED_MODE_COMPILE_TIME_ERROR.getSeverity();
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return ErrorType.CHECKED_MODE_COMPILE_TIME_ERROR;
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
