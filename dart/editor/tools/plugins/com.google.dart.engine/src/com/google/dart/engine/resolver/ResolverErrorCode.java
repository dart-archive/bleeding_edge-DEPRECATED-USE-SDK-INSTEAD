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
package com.google.dart.engine.resolver;

import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.error.ErrorType;

import static com.google.dart.engine.error.ErrorType.COMPILE_TIME_ERROR;

/**
 * The enumeration {@code ResolverErrorCode} defines the error codes used for errors detected by the
 * resolver. The convention for this class is for the name of the error code to indicate the problem
 * that caused the error to be generated and for the error message to explain what is wrong and,
 * when appropriate, how the problem can be corrected.
 * 
 * @coverage dart.engine.resolver
 */
public enum ResolverErrorCode implements ErrorCode {
  // TODO(brianwilkerson) Reword this message.
  BREAK_LABEL_ON_SWITCH_MEMBER(COMPILE_TIME_ERROR,
      "Break label resolves to case or default statement"),
  // TODO(brianwilkerson) Reword this message.
  CONTINUE_LABEL_ON_SWITCH(COMPILE_TIME_ERROR,
      "A continue label resolves to switch, must be loop or switch member"),

  MISSING_LIBRARY_DIRECTIVE_WITH_PART(COMPILE_TIME_ERROR,
      "Libraries that have parts must have a library directive");

  /**
   * The type of this error.
   */
  private final ErrorType type;

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
   * Initialize a newly created error code to have the given type and message.
   * 
   * @param type the type of this error
   * @param message the message template used to create the message to be displayed for the error
   */
  private ResolverErrorCode(ErrorType type, String message) {
    this(type, message, null);
  }

  /**
   * Initialize a newly created error code to have the given type, message and correction.
   * 
   * @param type the type of this error
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private ResolverErrorCode(ErrorType type, String message, String correction) {
    this.type = type;
    this.message = message;
    this.correction = correction;
  }

  @Override
  public String getCorrection() {
    return correction;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return type.getSeverity();
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return type;
  }
}
