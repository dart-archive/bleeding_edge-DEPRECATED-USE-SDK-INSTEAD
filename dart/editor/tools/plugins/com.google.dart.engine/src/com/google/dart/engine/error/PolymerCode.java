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
 * The enumeration {@code PolymerCode} defines Polymer specific problems.
 */
public enum PolymerCode implements ErrorCode {
  INVALID_ATTRIBUTE("Invalid attribute name '%s'");

  /**
   * The template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * Initialize a newly created error code to have the given message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private PolymerCode(String message) {
    this.message = message;
  }

  @Override
  public String getCorrection() {
    return null;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return ErrorSeverity.INFO;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return ErrorType.POLYMER;
  }
}
