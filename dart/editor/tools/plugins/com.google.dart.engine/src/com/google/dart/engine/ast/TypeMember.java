/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

/**
 * The abstract class <code>TypeMember</code> defines the behavior common to nodes that declare a
 * name within the scope of a type.
 */
public abstract class TypeMember extends Declaration {
  /**
   * Initialize a newly created member of a type.
   */
  public TypeMember() {
  }

  /**
   * Initialize a newly created member of a type.
   * 
   * @param comment the documentation comment associated with this member
   */
  public TypeMember(Comment comment) {
    super(comment);
  }
}
