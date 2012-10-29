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
 * Instances of the class {@code PartDirective} represent a part directive.
 * 
 * <pre>
 * partDirective ::=
 *     {@link Annotation metadata} 'part' {@link StringLiteral partUri} ';'
 * </pre>
 */
public class PartDirective extends Directive {
  /**
   * The token representing the 'part' token.
   */
  private Token partToken;

  /**
   * The URI of the part being included.
   */
  private StringLiteral partUri;

  /**
   * The semicolon terminating the directive.
   */
  private Token semicolon;

  /**
   * Initialize a newly created part directive.
   */
  public PartDirective() {
  }

  /**
   * Initialize a newly created part directive.
   * 
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param partToken the token representing the 'part' token
   * @param partUri the URI of the part being included
   * @param semicolon the semicolon terminating the directive
   */
  public PartDirective(Comment comment, List<Annotation> metadata, Token partToken,
      StringLiteral partUri, Token semicolon) {
    super(comment, metadata);
    this.partToken = partToken;
    this.partUri = becomeParentOf(partUri);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitPartDirective(this);
  }

  @Override
  public Token getBeginToken() {
    return partToken;
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
   * Return the token representing the 'part' token.
   * 
   * @return the token representing the 'part' token
   */
  public Token getPartToken() {
    return partToken;
  }

  /**
   * Return the URI of the part being included.
   * 
   * @return the URI of the part being included
   */
  public StringLiteral getPartUri() {
    return partUri;
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
   * Set the token representing the 'part' token to the given token.
   * 
   * @param partToken the token representing the 'part' token
   */
  public void setPartToken(Token partToken) {
    this.partToken = partToken;
  }

  /**
   * Set the URI of the part being included to the given string.
   * 
   * @param partUri the URI of the part being included
   */
  public void setPartUri(StringLiteral partUri) {
    this.partUri = becomeParentOf(partUri);
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
    safelyVisitChild(partUri, visitor);
  }
}
