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
 * Instances of the class {@code PartOfDirective} represent a part-of directive.
 * 
 * <pre>
 * partOfDirective ::=
 *     {@link Annotation metadata} 'part' 'of' {@link Identifier libraryName} ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class PartOfDirective extends Directive {
  /**
   * The token representing the 'part' token.
   */
  private Token partToken;

  /**
   * The token representing the 'of' token.
   */
  private Token ofToken;

  /**
   * The name of the library that the containing compilation unit is part of.
   */
  private LibraryIdentifier libraryName;

  /**
   * The semicolon terminating the directive.
   */
  private Token semicolon;

  /**
   * Initialize a newly created part-of directive.
   * 
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param partToken the token representing the 'part' token
   * @param ofToken the token representing the 'of' token
   * @param libraryName the name of the library that the containing compilation unit is part of
   * @param semicolon the semicolon terminating the directive
   */
  public PartOfDirective(Comment comment, List<Annotation> metadata, Token partToken,
      Token ofToken, LibraryIdentifier libraryName, Token semicolon) {
    super(comment, metadata);
    this.partToken = partToken;
    this.ofToken = ofToken;
    this.libraryName = becomeParentOf(libraryName);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitPartOfDirective(this);
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  @Override
  public Token getKeyword() {
    return partToken;
  }

  /**
   * Return the name of the library that the containing compilation unit is part of.
   * 
   * @return the name of the library that the containing compilation unit is part of
   */
  public LibraryIdentifier getLibraryName() {
    return libraryName;
  }

  /**
   * Return the token representing the 'of' token.
   * 
   * @return the token representing the 'of' token
   */
  public Token getOfToken() {
    return ofToken;
  }

  /**
   * Return the token representing the 'part' token.
   * 
   * @return the token representing the 'part' token
   */
  public Token getPartToken() {
    return partToken;
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
   * Set the name of the library that the containing compilation unit is part of to the given name.
   * 
   * @param libraryName the name of the library that the containing compilation unit is part of
   */
  public void setLibraryName(LibraryIdentifier libraryName) {
    this.libraryName = becomeParentOf(libraryName);
  }

  /**
   * Set the token representing the 'of' token to the given token.
   * 
   * @param ofToken the token representing the 'of' token
   */
  public void setOfToken(Token ofToken) {
    this.ofToken = ofToken;
  }

  /**
   * Set the token representing the 'part' token to the given token.
   * 
   * @param partToken the token representing the 'part' token
   */
  public void setPartToken(Token partToken) {
    this.partToken = partToken;
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
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(libraryName, visitor);
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    return partToken;
  }
}
