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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The abstract class {@code AnnotatedNode} defines the behavior of nodes that can be annotated with
 * both a comment and metadata.
 * 
 * @coverage dart.engine.ast
 */
public abstract class AnnotatedNode extends AstNode {
  /**
   * The documentation comment associated with this node, or {@code null} if this node does not have
   * a documentation comment associated with it.
   */
  private Comment comment;

  /**
   * The annotations associated with this node.
   */
  private NodeList<Annotation> metadata = new NodeList<Annotation>(this);

  /**
   * Initialize a newly created node.
   * 
   * @param comment the documentation comment associated with this node
   * @param metadata the annotations associated with this node
   */
  public AnnotatedNode(Comment comment, List<Annotation> metadata) {
    this.comment = becomeParentOf(comment);
    this.metadata.addAll(metadata);
  }

  @Override
  public final Token getBeginToken() {
    if (comment == null) {
      if (metadata.isEmpty()) {
        return getFirstTokenAfterCommentAndMetadata();
      } else {
        return metadata.getBeginToken();
      }
    } else if (metadata.isEmpty()) {
      return comment.getBeginToken();
    }
    Token commentToken = comment.getBeginToken();
    Token metadataToken = metadata.getBeginToken();
    if (commentToken.getOffset() < metadataToken.getOffset()) {
      return commentToken;
    }
    return metadataToken;
  }

  /**
   * Return the documentation comment associated with this node, or {@code null} if this node does
   * not have a documentation comment associated with it.
   * 
   * @return the documentation comment associated with this node
   */
  public Comment getDocumentationComment() {
    return comment;
  }

  /**
   * Return the annotations associated with this node.
   * 
   * @return the annotations associated with this node
   */
  public NodeList<Annotation> getMetadata() {
    return metadata;
  }

  /**
   * Set the documentation comment associated with this node to the given comment.
   * 
   * @param comment the documentation comment to be associated with this node
   */
  public void setDocumentationComment(Comment comment) {
    this.comment = becomeParentOf(comment);
  }

  /**
   * Set the metadata associated with this node to the given metadata.
   * 
   * @param metadata the metadata to be associated with this node
   */
  public void setMetadata(List<Annotation> metadata) {
    this.metadata.clear();
    this.metadata.addAll(metadata);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    if (commentIsBeforeAnnotations()) {
      safelyVisitChild(comment, visitor);
      metadata.accept(visitor);
    } else {
      for (AstNode child : getSortedCommentAndAnnotations()) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Return the first token following the comment and metadata.
   * 
   * @return the first token following the comment and metadata
   */
  protected abstract Token getFirstTokenAfterCommentAndMetadata();

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
   * Return an array containing the comment and annotations associated with this node, sorted in
   * lexical order.
   * 
   * @return the comment and annotations associated with this node in the order in which they
   *         appeared in the original source
   */
  private AstNode[] getSortedCommentAndAnnotations() {
    ArrayList<AstNode> childList = new ArrayList<AstNode>();
    childList.add(comment);
    childList.addAll(metadata);
    AstNode[] children = childList.toArray(new AstNode[childList.size()]);
    Arrays.sort(children, AstNode.LEXICAL_ORDER);
    return children;
  }
}
