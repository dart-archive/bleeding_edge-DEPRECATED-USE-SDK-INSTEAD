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
 * Instances of the class {@code ResourceDirective} represent a resource directive.
 * 
 * <pre>
 * resourceDirective ::=
 *     'resource' {@link StringLiteral resourceUri} ';'
 * </pre>
 */
public class ResourceDirective extends Directive {
  /**
   * The token representing the 'resource' token.
   */
  private Token resourceToken;

  /**
   * The URI of the resource being specified.
   */
  private StringLiteral resourceUri;

  /**
   * The semicolon terminating the directive.
   */
  private Token semicolon;

  /**
   * Initialize a newly created resource directive.
   */
  public ResourceDirective() {
  }

  /**
   * Initialize a newly created resource directive.
   * 
   * @param resourceToken the token representing the 'resource' token
   * @param resourceUri the URI of the resource being specified
   * @param semicolon the semicolon terminating the directive
   */
  public ResourceDirective(Token resourceToken, StringLiteral resourceUri, Token semicolon) {
    this.resourceToken = resourceToken;
    this.resourceUri = becomeParentOf(resourceUri);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitResourceDirective(this);
  }

  @Override
  public Token getBeginToken() {
    return resourceToken;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the 'resource' token.
   * 
   * @return the token representing the 'resource' token
   */
  public Token getResourceToken() {
    return resourceToken;
  }

  /**
   * Return the URI of the resource being specified.
   * 
   * @return the URI of the resource being specified
   */
  public StringLiteral getResourceUri() {
    return resourceUri;
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
   * Set the token representing the 'resource' token to the given token.
   * 
   * @param resourceToken the token representing the 'resource' token
   */
  public void setResourceToken(Token resourceToken) {
    this.resourceToken = resourceToken;
  }

  /**
   * Set the URI of the resource being specified to the given string.
   * 
   * @param string the URI of the resource being specified
   */
  public void setResourceUri(StringLiteral string) {
    resourceUri = becomeParentOf(string);
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
    safelyVisitChild(resourceUri, visitor);
  }
}
