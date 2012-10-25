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
 * The abstract class {@code Declaration} defines the behavior common to nodes that represent the
 * declaration of a name. Each declared name is visible within a name scope.
 */
public abstract class Declaration extends AnnotatedNode {
  /**
   * Initialize a newly created declaration.
   */
  public Declaration() {
  }

  /**
   * Initialize a newly created declaration.
   * 
   * @param comment the documentation comment associated with this declaration
   * @param metadata the annotations associated with this declaration
   */
  public Declaration(Comment comment, List<Annotation> metadata) {
    super(comment, metadata);
  }
}
