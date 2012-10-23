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

import java.util.List;

/**
 * The abstract class {@code ClassMember} defines the behavior common to nodes that declare a name
 * within the scope of a class.
 */
public abstract class ClassMember extends Declaration {
  /**
   * Initialize a newly created member of a class.
   */
  public ClassMember() {
  }

  /**
   * Initialize a newly created member of a class.
   * 
   * @param comment the documentation comment associated with this member
   * @param metadata the annotations associated with this member
   */
  public ClassMember(Comment comment, List<Annotation> metadata) {
    super(comment, metadata);
  }
}
