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
package com.google.dart.server.error;

/**
 * The enumeration {@code PubSuggestionCode} defines the suggestions used for reporting deviations
 * from pub best practices. The convention for this class is for the name of the bad practice to
 * indicate the problem that caused the suggestion to be generated and for the message to explain
 * what is wrong and, when appropriate, how the situation can be corrected.
 * 
 * @coverage dart.server.error
 */
public enum PubSuggestionCode implements ErrorCode {
  /**
   * It is a bad practice for a source file in a package "lib" directory hierarchy to traverse
   * outside that directory hierarchy. For example, a source file in the "lib" directory should not
   * contain a directive such as {@code import '../web/some.dart'} which references a file outside
   * the lib directory.
   */
  FILE_IMPORT_INSIDE_LIB_REFERENCES_FILE_OUTSIDE(
      "A file in the 'lib' directory hierarchy should not reference a file outside that hierarchy"),

  /**
   * It is a bad practice for a source file ouside a package "lib" directory hierarchy to traverse
   * into that directory hierarchy. For example, a source file in the "web" directory should not
   * contain a directive such as {@code import '../lib/some.dart'} which references a file inside
   * the lib directory.
   */
  FILE_IMPORT_OUTSIDE_LIB_REFERENCES_FILE_INSIDE(
      "A file outside the 'lib' directory hierarchy should not reference a file inside that hierarchy. Use a package: reference instead."),

  /**
   * It is a bad practice for a package import to reference anything outside the given package, or
   * more generally, it is bad practice for a package import to contain a "..". For example, a
   * source file should not contain a directive such as {@code import 'package:foo/../some.dart'}.
   */
  PACKAGE_IMPORT_CONTAINS_DOT_DOT("A package import should not contain '..'");

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
  private PubSuggestionCode(String message) {
    this(message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private PubSuggestionCode(String message, String correction) {
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
