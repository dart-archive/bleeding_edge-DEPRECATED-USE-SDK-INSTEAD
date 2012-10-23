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
 * Instances of the class {@code Directive} defines the behavior common to nodes that represent a
 * directive.
 * 
 * <pre>
 * directive ::=
 *     {@link ImportDirective importDirective}
 *   | {@link LibraryDirective libraryDirective}
 *   | {@link PartDirective partDirective}
 *   | {@link PartOfDirective partOfDirective}
 * </pre>
 */
public abstract class Directive extends ASTNode {
  /**
   * The annotations associated with the directive.
   */
  private NodeList<Annotation> metadata = new NodeList<Annotation>(this);

  /**
   * Initialize a newly create directive.
   */
  public Directive() {
    super();
  }

  /**
   * Initialize a newly create directive.
   * 
   * @param metadata the annotations associated with the directive
   */
  public Directive(List<Annotation> metadata) {
    this.metadata.addAll(metadata);
  }

  /**
   * Return the annotations associated with the directive.
   * 
   * @return the annotations associated with the directive
   */
  public NodeList<Annotation> getMetadata() {
    return metadata;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    metadata.accept(visitor);
  }
}
