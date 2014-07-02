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
package com.google.dart.server.error;

/**
 * The enumeration {@code ScannerErrorCode} defines the error codes used for errors detected by the
 * scanner.
 * 
 * @coverage dart.server.error
 */
public enum ScannerErrorCode implements ErrorCode {
  ILLEGAL_CHARACTER("Illegal character %x"),
  MISSING_DIGIT("Decimal digit expected"),
  MISSING_HEX_DIGIT("Hexidecimal digit expected"),
  MISSING_QUOTE("Expected quote (' or \")"),
  UNTERMINATED_MULTI_LINE_COMMENT("Unterminated multi-line comment"),
  UNTERMINATED_STRING_LITERAL("Unterminated string literal");

  /**
   * The template used to create the correction to be displayed for this error, or {@code null} if
   * there is no correction information for this error.
   */
  public String correction;

  /**
   * Initialize a newly created error code to have the given message.
   * 
   * @param message the message template used to create the message to be displayed for this error
   */
  private ScannerErrorCode(String message) {
    this(message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private ScannerErrorCode(String message, String correction) {
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
