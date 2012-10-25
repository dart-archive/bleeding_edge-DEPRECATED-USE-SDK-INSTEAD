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

import java.util.List;

/**
 * Instances of the class {@code LibraryDirective} represent a library directive.
 * 
 * <pre>
 * libraryDirective ::=
 *     {@link Annotation metadata} 'library' {@link Identifier name} ';'
 * </pre>
 */
public class LibraryDirective extends Directive {
  /**
   * The token representing the 'library' token.
   */
  private Token libraryToken;

  /**
   * The name of the library being defined.
   */
  private SimpleIdentifier name;

  /**
   * The semicolon terminating the directive.
   */
  private Token semicolon;

  /**
   * Initialize a newly created library directive.
   */
  public LibraryDirective() {
  }

  /**
   * Initialize a newly created library directive.
   * 
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param libraryToken the token representing the 'library' token
   * @param name the name of the library being defined
   * @param semicolon the semicolon terminating the directive
   */
  public LibraryDirective(Comment comment, List<Annotation> metadata, Token libraryToken,
      SimpleIdentifier name, Token semicolon) {
    super(comment, metadata);
    this.libraryToken = libraryToken;
    this.name = becomeParentOf(name);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitLibraryDirective(this);
  }

  @Override
  public Token getBeginToken() {
    return libraryToken;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the 'library' token.
   * 
   * @return the token representing the 'library' token
   */
  public Token getLibraryToken() {
    return libraryToken;
  }

  /**
   * Return the name of the library being defined.
   * 
   * @return the name of the library being defined
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Return the semicolon terminating the directive.
   * 
   * @return the semicolon terminating the directive
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the token representing the 'library' token to the given token.
   * 
   * @param libraryToken the token representing the 'library' token
   */
  public void setLibraryToken(Token libraryToken) {
    this.libraryToken = libraryToken;
  }

  /**
   * Set the name of the library being defined to the given string.
   * 
   * @param name the name of the library being defined
   */
  public void setName(SimpleIdentifier name) {
    this.name = becomeParentOf(name);
  }

  /**
   * Set the semicolon terminating the directive to the given token.
   * 
   * @param semicolon the semicolon terminating the directive
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(name, visitor);
  }
}
