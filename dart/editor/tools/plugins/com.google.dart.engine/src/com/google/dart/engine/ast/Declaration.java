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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The abstract class {@code Declaration} defines the behavior common to nodes that represent the
 * declaration of a name. Each declared name is visible within a name scope.
 */
public abstract class Declaration extends ASTNode {
  /**
   * The documentation comment associated with this declaration, or {@code null} if this declaration
   * does not have a documentation comment associated with it.
   */
  private Comment comment;

  /**
   * The annotations associated with this declaration.
   */
  private NodeList<Annotation> metadata = new NodeList<Annotation>(this);

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
    this.comment = becomeParentOf(comment);
    this.metadata.addAll(metadata);
  }

  /**
   * Return the documentation comment associated with this declaration, or {@code null} if this
   * declaration does not have a documentation comment associated with it.
   * 
   * @return the documentation comment associated with this declaration
   */
  public Comment getDocumentationComment() {
    return comment;
  }

  /**
   * Return the annotations associated with this declaration.
   * 
   * @return the annotations associated with this declaration
   */
  public NodeList<Annotation> getMetadata() {
    return metadata;
  }

  /**
   * Set the documentation comment associated with this declaration to the given comment
   * 
   * @param comment the documentation comment to be associated with this declaration
   */
  public void setDocumentationComment(Comment comment) {
    this.comment = becomeParentOf(comment);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    if (commentIsBeforeAnnotations()) {
      safelyVisitChild(comment, visitor);
      metadata.accept(visitor);
    } else {
      for (ASTNode child : getSortedCommentAndAnnotations()) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Return {@code true} if the comment is lexically before any annotations.
   * 
   * @return {@code true} if the comment is lexically before any annotations
   */
  private boolean commentIsBeforeAnnotations() {
    if (comment == null || metadata.isEmpty()) {
      return true;
    }
    Annotation firstAnnotation = metadata.get(0);
    return comment.getOffset() < firstAnnotation.getOffset();
  }

  /**
   * Return an array containing all of the directives and declarations in this compilation unit,
   * sorted in lexical order.
   * 
   * @return the directives and declarations in this compilation unit in the order in which they
   *         appeared in the original source
   */
  private ASTNode[] getSortedCommentAndAnnotations() {
    ArrayList<ASTNode> childList = new ArrayList<ASTNode>();
    childList.add(comment);
    childList.addAll(metadata);
    ASTNode[] children = childList.toArray(new ASTNode[childList.size()]);
    Arrays.sort(children, ASTNode.LEXICAL_ORDER);
    return children;
  }
}
