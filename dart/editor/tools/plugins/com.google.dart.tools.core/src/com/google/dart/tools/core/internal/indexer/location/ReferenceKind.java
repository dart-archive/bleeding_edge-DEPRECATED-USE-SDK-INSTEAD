/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.indexer.location;

/**
 * Instances of the class <code>ReferenceKind</code>
 */
public enum ReferenceKind {
  /**
   * A reference to a field in which the field's value is being read.
   */
  FIELD_READ('r'),

  /**
   * A reference to a field in which the field's value is being written.
   */
  FIELD_WRITE('w'),

  /**
   * A reference to a function in which the function is being executed.
   */
  FUNCTION_EXECUTION('e'),

  /**
   * A reference to a method in which the method is being invoked.
   */
  METHOD_INVOCATION('i');

  /**
   * Return the reference kind with the given code, or <code>null</code> if there is no such kind.
   * 
   * @param code the code of the kind to be returned
   * @return the reference kind with the given code
   */
  public static ReferenceKind fromCode(char code) {
    for (ReferenceKind kind : ReferenceKind.values()) {
      if (kind.getCode() == code) {
        return kind;
      }
    }
    return null;
  }

  /**
   * The code used to identify this kind in a unique identifier stored by the indexer.
   */
  private char code;

  /**
   * Initialize a newly created reference kind to have the given kind code.
   * 
   * @param code the code used to identify this kind in a unique identifier stored by the indexer
   */
  private ReferenceKind(char code) {
    this.code = code;
  }

  /**
   * Return the code used to identify this kind in a unique identifier stored by the indexer.
   * 
   * @return the code used to identify this kind
   */
  public char getCode() {
    return code;
  }
}
