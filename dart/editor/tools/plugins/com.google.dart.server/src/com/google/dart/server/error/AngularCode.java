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
package com.google.dart.server.error;

/**
 * The enumeration {@code AngularCode} defines Angular specific problems.
 * 
 * @coverage dart.server.error
 */
public enum AngularCode implements ErrorCode {
  CANNOT_PARSE_SELECTOR("The selector '%s' cannot be parsed"),
  INVALID_FORMATTER_NAME("Formatter name must be a simple identifier"),
  INVALID_PROPERTY_KIND(
      "Unknown property binding kind '%s', use one of the '@', '=>', '=>!' or '<=>'"),
  INVALID_PROPERTY_FIELD("Unknown property field '%s'"),
  INVALID_PROPERTY_MAP("Argument 'map' must be a constant map literal"),
  INVALID_PROPERTY_NAME("Property name must be a string literal"),
  INVALID_PROPERTY_SPEC("Property binding specification must be a string literal"),
  INVALID_REPEAT_SYNTAX("Expected statement in form '_item_ in _collection_ [tracked by _id_]'"),
  INVALID_REPEAT_ITEM_SYNTAX("Item must by identifier or in '(_key_, _value_)' pair."),
  INVALID_URI("Invalid URI syntax: '%s'"),
  MISSING_FORMATTER_COLON("Missing ':' before formatter argument"),
  MISSING_NAME("Argument 'name' must be provided"),
  MISSING_PUBLISH_AS("Argument 'publishAs' must be provided"),
  MISSING_SELECTOR("Argument 'selector' must be provided"),
  URI_DOES_NOT_EXIST("Target of URI does not exist: '%s'");

  /**
   * Initialize a newly created error code to have the given message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private AngularCode(String message) {
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
