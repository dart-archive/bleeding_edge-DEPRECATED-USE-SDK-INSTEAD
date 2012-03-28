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

import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class <code>CommentReference</code> represent a reference to a Dart element that
 * is found within a documentation comment.
 * 
 * <pre>
 * commentReference ::=
 *     '[' {@link SimpleIdentifier identifier} ']'
 * </pre>
 */
public class CommentReference extends ASTNode {
  /**
   * The identifier being referenced.
   */
  private SimpleIdentifier identifier;

  /**
   * Initialize a newly created reference to a Dart element.
   */
  public CommentReference() {
  }

  /**
   * Initialize a newly created reference to a Dart element.
   * 
   * @param identifier the identifier being referenced
   */
  public CommentReference(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitCommentReference(this);
  }

  @Override
  public Token getBeginToken() {
    return getParent().getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return getParent().getEndToken();
  }

  /**
   * Return the identifier being referenced.
   * 
   * @return the identifier being referenced
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * Set the identifier being referenced to the given identifier.
   * 
   * @param identifier the identifier being referenced
   */
  public void setIdentifier(SimpleIdentifier identifier) {
    identifier = becomeParentOf(identifier);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(identifier, visitor);
  }
}
