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
 * The abstract class <code>Declaration</code> defines the behavior common to nodes that represent
 * the declaration of a name. Each declared name is visible within a name scope.
 */
public abstract class Declaration extends ASTNode {
  /**
   * The documentation comment associated with this declaration, or <code>null</code> if this
   * declaration does not have a documentation comment associated with it.
   */
  private Comment comment;

  /**
   * Initialize a newly created declaration.
   */
  public Declaration() {
  }

  /**
   * Initialize a newly created declaration.
   * 
   * @param comment the documentation comment associated with this declaration
   */
  public Declaration(Comment comment) {
    this.comment = becomeParentOf(comment);
  }

  /**
   * Return the documentation comment associated with this declaration, or <code>null</code> if this
   * declaration does not have a documentation comment associated with it.
   * 
   * @return the documentation comment associated with this declaration
   */
  public Comment getDocumentationComment() {
    return comment;
  }

  /**
   * Set the documentation comment associated with this declaration to the given comment
   * 
   * @param comment the documentation comment associated with this declaration
   */
  public void setDocumentationComment(Comment comment) {
    this.comment = becomeParentOf(comment);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(comment, visitor);
  }
}
