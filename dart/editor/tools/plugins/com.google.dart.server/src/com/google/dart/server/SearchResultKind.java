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
   * A declaration of a class.
   */
  CLASS_DECLARATION,

  /**
   * A declaration of a class member.
   */
  CLASS_MEMBER_DECLARATION,

  /**
   * A reference to a constructor.
   */
  CONSTRUCTOR_REFERENCE,

  /**
   * A reference to a field (from field formal parameter).
   */
  FIELD_REFERENCE,

  /**
   * A reference to a field in which it is read.
   */
  FIELD_READ,

  /**
   * A reference to a field in which it is read and written.
   */
  FIELD_READ_WRITE,

  /**
   * A reference to a field in which it is written.
   */
  FIELD_WRITE,

  /**
   * A declaration of a function.
   */
  FUNCTION_DECLARATION,

  /**
   * A reference to a function in which it is invoked.
   */
  FUNCTION_INVOCATION,

  /**
   * A reference to a function in which it is referenced.
   */
  FUNCTION_REFERENCE,

  /**
   * A declaration of a function type.
   */
  FUNCTION_TYPE_DECLARATION,

  /**
   * A reference to a method in which it is invoked.
   */
  METHOD_INVOCATION,

  /**
   * A reference to a method in which it is referenced.
   */
  METHOD_REFERENCE,

  /**
   * A reference to a name, resolved.
   */
  NAME_REFERENCE_RESOLVED,

  /**
   * A reference to a name, unresolved.
   */
  NAME_REFERENCE_UNRESOLVED,

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
   * A reference to a variable in which it is read.
   */
  VARIABLE_READ,

  /**
   * A reference to a variable in which it is both read and written.
   */
  VARIABLE_READ_WRITE,

  /**
   * A reference to a variable in which it is written.
   */
  VARIABLE_WRITE;
}
