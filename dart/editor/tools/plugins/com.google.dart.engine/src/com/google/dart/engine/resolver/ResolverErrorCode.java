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
import static com.google.dart.engine.error.ErrorType.STATIC_TYPE_WARNING;
import static com.google.dart.engine.error.ErrorType.STATIC_WARNING;

/**
 * The enumeration {@code ResolverErrorCode} defines the error codes used for errors detected by the
 * resolver. The convention for this class is for the name of the error code to indicate the problem
 * that caused the error to be generated and for the error message to explain what is wrong and,
 * when appropriate, how the problem can be corrected.
 */
public enum ResolverErrorCode implements ErrorCode {
  // TODO(brianwilkerson) Reword this message.
  BREAK_LABEL_ON_SWITCH_MEMBER(COMPILE_TIME_ERROR,
      "Break label resolves to case or default statement"),
  CANNOT_BE_RESOLVED(STATIC_WARNING, "Cannot resolve the name '%s'"),
  // TODO(brianwilkerson) Reword this message.
  CONTINUE_LABEL_ON_SWITCH(COMPILE_TIME_ERROR,
      "A continue label resolves to switch, must be loop or switch member"),

  /**
   * It is a compile-time error if [the URI] is not a compile-time constant, or if [the URI]
   * involves string interpolation.
   */
  INVALID_URI(COMPILE_TIME_ERROR,
      "URI's used in directives must be compile time constants without interpolation expressions"),

  LABEL_IN_OUTER_SCOPE(COMPILE_TIME_ERROR,
      "Cannot reference label '%s' declared in an outer method or function"),

  MISSING_LIBRARY_DIRECTIVE_IMPORTED(COMPILE_TIME_ERROR,
      "Libraries that are imported by other libraries must have a library directive"),
  MISSING_LIBRARY_DIRECTIVE_WITH_PART(COMPILE_TIME_ERROR,
      "Libraries that have parts must have a library directive"),
  MISSING_PART_OF_DIRECTIVE(COMPILE_TIME_ERROR, "The included part must have a part-of directive"),

  NON_BOOLEAN_CONDITION(STATIC_TYPE_WARNING, "Conditions must have a static type of 'bool'"),

  PART_WITH_WRONG_LIBRARY_NAME(STATIC_WARNING,
      "The included part appears to be part of the library '%s'"),
  UNDEFINED_LABEL(COMPILE_TIME_ERROR, "The label '%s' is not defined"),
  //
  // The following codes are temporary and should be deleted when more accurate reporting is implemented.
  //
  DUPLICATE_MEMBER_ERROR(COMPILE_TIME_ERROR, "Duplicate member '%s'"),
  DUPLICATE_MEMBER_WARNING(STATIC_WARNING, "Duplicate member '%s'");

  /**
   * The type of this error.
   */
  private final ErrorType type;

  /**
   * The message template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * Initialize a newly created error code to have the given type and message.
   * 
   * @param type the type of this error
   * @param message the message template used to create the message to be displayed for the error
   */
  private ResolverErrorCode(ErrorType type, String message) {
    this.type = type;
    this.message = message;
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

  @Override
  public boolean needsRecompilation() {
    return true;
  }
}
