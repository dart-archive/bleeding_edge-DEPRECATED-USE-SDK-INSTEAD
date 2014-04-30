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
package com.google.dart.server;

/**
 * The enumeration {@code SearchResultKind} defines the various kinds of {@link SearchResult}.
 * 
 * @coverage dart.server
 */
public enum SearchResultKind {
  /**
   * A reference to a constructor.
   */
  CONSTRUCTOR_REFERENCE,

  /**
   * A reference to a field (from field formal parameter).
   */
  FIELD_REFERENCE,

  /**
   * A reference to a field in which the field's value is being read.
   */
  FIELD_READ,

  /**
   * A reference to a field in which the field's value is being written.
   */
  FIELD_WRITE,

  /**
   * A reference to a function in which the function is being invoked.
   */
  FUNCTION_INVOCATION,

  /**
   * A reference to a function in which the function is being referenced.
   */
  FUNCTION_REFERENCE,

  /**
   * A reference to a method in which the method is being invoked.
   */
  METHOD_INVOCATION,

  /**
   * A reference to a method in which the method is being referenced.
   */
  METHOD_REFERENCE,

  /**
   * A reference to a property accessor.
   */
  PROPERTY_ACCESSOR_REFERENCE,

  /**
   * A reference to a type.
   */
  TYPE_REFERENCE,

  /**
   * A declaration of a variable.
   */
  VARIABLE_DECLARATION,

  /**
   * A reference to a variable in which the variable's value is being read.
   */
  VARIABLE_READ,

  /**
   * A reference to a variable in which the variable's value is being both read and write.
   */
  VARIABLE_READ_WRITE,

  /**
   * A reference to a variable in which the variables's value is being written.
   */
  VARIABLE_WRITE;
}
