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
 * The abstract class {@code NormalFormalParameter} defines the behavior common to formal parameters
 * that are required (are not optional).
 * 
 * <pre>
 * normalFormalParameter ::=
 *     {@link FunctionTypedFormalParameter functionSignature}
 *   | {@link FieldFormalParameter fieldFormalParameter}
 *   | {@link SimpleFormalParameter simpleFormalParameter}
 * </pre>
 */
public abstract class NormalFormalParameter extends FormalParameter {
  /**
   * The documentation comment associated with this parameter, or {@code null} if this parameter
   * does not have a documentation comment associated with it.
   */
  private Comment comment;

  /**
   * The annotations associated with this parameter.
   */
  private NodeList<Annotation> metadata = new NodeList<Annotation>(this);

  /**
   * The name of the parameter being declared.
   */
  private SimpleIdentifier identifier;

  /**
   * Initialize a newly created formal parameter.
   */
  public NormalFormalParameter() {
  }

  /**
   * Initialize a newly created formal parameter.
   * 
   * @param comment the documentation comment associated with this parameter
   * @param metadata the annotations associated with this parameter
   * @param identifier the name of the parameter being declared
   */
  public NormalFormalParameter(Comment comment, List<Annotation> metadata,
      SimpleIdentifier identifier) {
    this.comment = becomeParentOf(comment);
    this.metadata.addAll(metadata);
    this.identifier = becomeParentOf(identifier);
  }

  /**
   * Return the documentation comment associated with this parameter, or {@code null} if this
   * parameter does not have a documentation comment associated with it.
   * 
   * @return the documentation comment associated with this parameter
   */
  public Comment getDocumentationComment() {
    return comment;
  }

  /**
   * Return the name of the parameter being declared.
   * 
   * @return the name of the parameter being declared
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  @Override
  public ParameterKind getKind() {
    ASTNode parent = getParent();
    if (parent instanceof DefaultFormalParameter) {
      return ((DefaultFormalParameter) parent).getKind();
    }
    return ParameterKind.REQUIRED;
  }

  /**
   * Return the annotations associated with this parameter.
   * 
   * @return the annotations associated with this parameter
   */
  public NodeList<Annotation> getMetadata() {
    return metadata;
  }

  /**
   * Set the documentation comment associated with this parameter to the given comment
   * 
   * @param comment the documentation comment to be associated with this parameter
   */
  public void setDocumentationComment(Comment comment) {
    this.comment = becomeParentOf(comment);
  }

  /**
   * Set the name of the parameter being declared to the given identifier.
   * 
   * @param identifier the name of the parameter being declared
   */
  public void setIdentifier(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    //
    // Note that subclasses are responsible for visiting the identifier because they often need to
    // visit other nodes before visiting the identifier.
    //
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
   * Return an array containing the comment and annotations associated with this parameter, sorted
   * in lexical order.
   * 
   * @return the comment and annotations associated with this parameter in the order in which they
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
