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
 * The enumeration {@code PolymerCode} defines Polymer specific problems.
 * 
 * @coverage dart.server.error
 */
public enum PolymerCode implements ErrorCode {
  ATTRIBUTE_FIELD_NOT_PUBLISHED("Field '%s' in '%s' must be @published"),
  DUPLICATE_ATTRIBUTE_DEFINITION("The attribute '%s' is already defined"),
  EMPTY_ATTRIBUTES("Empty 'attributes' attribute is useless"),
  INVALID_ATTRIBUTE_NAME("'%s' is not a valid name for a custom element attribute"),
  INVALID_TAG_NAME("'%s' is not a valid name for a custom element"),
  MISSING_TAG_NAME(
      "Missing tag name of the custom element. Please include an attribute like name='your-tag-name'"),
  UNDEFINED_ATTRIBUTE_FIELD("There is no such field '%s' in '%s'");

  /**
   * Initialize a newly created error code to have the given message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private PolymerCode(String message) {
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
