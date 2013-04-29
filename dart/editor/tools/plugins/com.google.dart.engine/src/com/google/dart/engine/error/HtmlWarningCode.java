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
package com.google.dart.engine.error;

/**
 * The enumeration {@code HtmlWarningCode} defines the error codes used for warnings in HTML files.
 * The convention for this class is for the name of the error code to indicate the problem that
 * caused the error to be generated and for the error message to explain what is wrong and, when
 * appropriate, how the problem can be corrected.
 * 
 * @coverage dart.engine.error
 */
public enum HtmlWarningCode implements ErrorCode {
  /**
   * An error code indicating that the value of the 'src' attribute of a Dart script tag is not a
   * valid URI.
   */
  INVALID_URI("Invalid URI"),

  /**
   * An error code indicating that the value of the 'src' attribute of a Dart script tag references
   * a file that does not exist.
   */
  URI_DOES_NOT_EXIST("The referenced URI does not exist");

  /**
   * The message template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * Initialize a newly created error code to have the given type and message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private HtmlWarningCode(String message) {
    this.message = message;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return ErrorSeverity.WARNING;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    // TODO(brianwilkerson) We should probably define a new error type for this class.
    return ErrorType.STATIC_WARNING;
  }

  @Override
  public boolean needsRecompilation() {
    return false;
  }
}
