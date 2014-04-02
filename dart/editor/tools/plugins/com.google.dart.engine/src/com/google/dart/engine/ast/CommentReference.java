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
 * Instances of the class {@code CommentReference} represent a reference to a Dart element that is
 * found within a documentation comment.
 * 
 * <pre>
 * commentReference ::=
 *     '[' 'new'? {@link Identifier identifier} ']'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class CommentReference extends AstNode {
  /**
   * The token representing the 'new' keyword, or {@code null} if there was no 'new' keyword.
   */
  private Token newKeyword;

  /**
   * The identifier being referenced.
   */
  private Identifier identifier;

  /**
   * Initialize a newly created reference to a Dart element.
   * 
   * @param newKeyword the token representing the 'new' keyword
   * @param identifier the identifier being referenced
   */
  public CommentReference(Token newKeyword, Identifier identifier) {
    this.newKeyword = newKeyword;
    this.identifier = becomeParentOf(identifier);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitCommentReference(this);
  }

  @Override
  public Token getBeginToken() {
    return identifier.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return identifier.getEndToken();
  }

  /**
   * Return the identifier being referenced.
   * 
   * @return the identifier being referenced
   */
  public Identifier getIdentifier() {
    return identifier;
  }

  /**
   * Return the token representing the 'new' keyword, or {@code null} if there was no 'new' keyword.
   * 
   * @return the token representing the 'new' keyword
   */
  public Token getNewKeyword() {
    return newKeyword;
  }

  /**
   * Set the identifier being referenced to the given identifier.
   * 
   * @param identifier the identifier being referenced
   */
  public void setIdentifier(Identifier identifier) {
    identifier = becomeParentOf(identifier);
  }

  /**
   * Set the token representing the 'new' keyword to the given token.
   * 
   * @param newKeyword the token representing the 'new' keyword
   */
  public void setNewKeyword(Token newKeyword) {
    this.newKeyword = newKeyword;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(identifier, visitor);
  }
}
