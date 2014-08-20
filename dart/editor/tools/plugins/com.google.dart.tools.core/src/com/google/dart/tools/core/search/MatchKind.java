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
package com.google.dart.tools.core.search;

import com.google.dart.engine.ast.CompilationUnit;

/**
 * Instances of the enum <code>MatchKind</code> represent the kind of reference that was found when
 * a match represents a reference to an element.
 * 
 * @coverage dart.tools.core.search
 */
public enum MatchKind {
  /**
   * The kind that is used when the match does not represent a reference to an element.
   */
  NOT_A_REFERENCE,

  /**
   * A reference to a field in which the field's value is being read.
   */
  FIELD_READ,

  /**
   * A reference to a field in which the field's value is being written.
   */
  FIELD_WRITE,

  /**
   * A reference to a function in which the function is being executed.
   */
  FUNCTION_EXECUTION,

  /**
   * A reference to a function type.
   */
  FUNCTION_TYPE_REFERENCE,

  /**
   * A reference to a {@link DartImport}.
   */
  IMPORT_REFERENCE,

  /**
   * A reference to a {@link CompilationUnit}.
   */
  FILE_REFERENCE,

  /**
   * A reference to a class that is implementing a specified type.
   */
  INTERFACE_IMPLEMENTED,

  /**
   * A reference to a method in which the method is being invoked.
   */
  METHOD_INVOCATION,

  /**
   * A reference to a method in which the method is being referenced.
   */
  METHOD_REFERENCE,

  /**
   * A reference to a named parameter in invocation.
   */
  NAMED_PARAMETER_REFERENCE,

  /**
   * A reference to a type.
   */
  TYPE_REFERENCE;
}
