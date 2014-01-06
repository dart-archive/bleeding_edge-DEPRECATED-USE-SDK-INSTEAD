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
 * The enumeration {@code AngularCode} defines Angular specific problems.
 */
public enum AngularCode implements ErrorCode {
  CANNOT_PARSE_SELECTOR("The selector '%s' cannot be parsed"),
  EXPECTED_IDENTIFIER("Expected an identifier"),
  EXPECTED_IN("Expected 'in' keyword"),
  INVALID_PROPERTY_KIND(
      "Unknown property binding kind '%s', use one of the '@', '=>', '=>!' or '<=>'"),
  INVALID_PROPERTY_FIELD("Unknown property field '%s'"),
  INVALID_PROPERTY_MAP("Argument 'map' must be a constant map literal"),
  INVALID_PROPERTY_NAME("Property name must be a string literal"),
  INVALID_PROPERTY_SPEC("Property binding specification must be a string literal"),
  MISSING_CSS_URL("Argument 'cssUrl' must be provided"),
  MISSING_NAME("Argument 'name' must be provided"),
  MISSING_PUBLISH_AS("Argument 'publishAs' must be provided"),
  MISSING_TEMPLATE_URL("Argument 'templateUrl' must be provided"),
  MISSING_SELECTOR("Argument 'selector' must be provided");

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
  private AngularCode(String message) {
    this.message = message;
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private AngularCode(String message, String correction) {
    this.message = message;
    this.correction = correction;
  }

  @Override
  public String getCorrection() {
    return correction;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return ErrorType.TOOLKIT.getSeverity();
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return ErrorType.TOOLKIT;
  }
}
