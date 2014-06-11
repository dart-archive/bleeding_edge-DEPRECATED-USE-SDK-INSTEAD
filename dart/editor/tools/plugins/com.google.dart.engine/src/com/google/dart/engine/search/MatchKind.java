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
package com.google.dart.engine.search;

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;

/**
 * Instances of the enum <code>MatchKind</code> represent the kind of reference that was found when
 * a match represents a reference to an element.
 * 
 * @coverage dart.engine.search
 */
public enum MatchKind {
  /**
   * A reference to an Angular element.
   */
  ANGULAR_REFERENCE,

  /**
   * A reference to an Angular element.
   */
  ANGULAR_CLOSING_TAG_REFERENCE,

  /**
   * A declaration of a class.
   */
  CLASS_DECLARATION,

  /**
   * A declaration of a class alias.
   */
  CLASS_ALIAS_DECLARATION,

  /**
   * A declaration of a constructor.
   */
  CONSTRUCTOR_DECLARATION,

  /**
   * A reference to a constructor in which the constructor is being referenced.
   */
  CONSTRUCTOR_REFERENCE,

  /**
   * A reference to a type in which the type was extended.
   */
  EXTENDS_REFERENCE,

  /**
   * A reference to a field in which the field's value is being invoked.
   */
  FIELD_INVOCATION,

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
   * A declaration of a function.
   */
  FUNCTION_DECLARATION,

  /**
   * A reference to a function in which the function is being executed.
   */
  FUNCTION_EXECUTION,

  /**
   * A reference to a function in which the function is being referenced.
   */
  FUNCTION_REFERENCE,

  /**
   * A declaration of a function type.
   */
  FUNCTION_TYPE_DECLARATION,

  /**
   * A reference to a function type.
   */
  FUNCTION_TYPE_REFERENCE,
  /**
   * A reference to a type in which the type was implemented.
   */
  IMPLEMENTS_REFERENCE,

  /**
   * A reference to a {@link ImportElement}.
   */
  IMPORT_REFERENCE,

  /**
   * A reference to a class that is implementing a specified type.
   */
  INTERFACE_IMPLEMENTED,

  /**
   * A reference to a {@link LibraryElement}.
   */
  LIBRARY_REFERENCE,

  /**
   * A reference to a method in which the method is being invoked.
   */
  METHOD_INVOCATION,

  /**
   * A reference to a method in which the method is being referenced.
   */
  METHOD_REFERENCE,

  /**
   * A declaration of a name.
   */
  NAME_DECLARATION,

  /**
   * A reference to a name, resolved.
   */
  NAME_REFERENCE_RESOLVED,

  /**
   * An invocation of a name, resolved.
   */
  NAME_INVOCATION_RESOLVED,

  /**
   * A reference to a name in which the name's value is being read.
   */
  NAME_READ_RESOLVED,

  /**
   * A reference to a name in which the name's value is being read and written.
   */
  NAME_READ_WRITE_RESOLVED,

  /**
   * A reference to a name in which the name's value is being written.
   */
  NAME_WRITE_RESOLVED,

  /**
   * An invocation of a name, unresolved.
   */
  NAME_INVOCATION_UNRESOLVED,

  /**
   * A reference to a name in which the name's value is being read.
   */
  NAME_READ_UNRESOLVED,

  /**
   * A reference to a name in which the name's value is being read and written.
   */
  NAME_READ_WRITE_UNRESOLVED,

  /**
   * A reference to a name in which the name's value is being written.
   */
  NAME_WRITE_UNRESOLVED,

  /**
   * A reference to a name, unresolved.
   */
  NAME_REFERENCE_UNRESOLVED,

  /**
   * A reference to a named parameter in invocation.
   */
  NAMED_PARAMETER_REFERENCE,

  /**
   * A reference to a property accessor.
   */
  PROPERTY_ACCESSOR_REFERENCE,

  /**
   * A reference to a type.
   */
  TYPE_REFERENCE,

  /**
   * A reference to a type parameter.
   */
  TYPE_PARAMETER_REFERENCE,

  /**
   * A reference to a {@link CompilationUnitElement}.
   */
  UNIT_REFERENCE,

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
  VARIABLE_WRITE,

  /**
   * A reference to a type in which the type was mixed in.
   */
  WITH_REFERENCE;
}
