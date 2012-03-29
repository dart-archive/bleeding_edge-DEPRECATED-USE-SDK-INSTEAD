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
 * Instances of the class <code>ResourceDirective</code> represent a resource directive.
 * 
 * <pre>
 * resourceDirective ::=
 *     '#' 'resource' '(' {@link StringLiteral resourceUri} ')' ';'
 * </pre>
 */
public class ResourceDirective extends Directive {
  /**
   * The hash mark introducing the directive.
   */
  private Token hash;

  /**
   * The token representing the 'resource' keyword.
   */
  private Token keyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The URI of the resource being specified.
   */
  private StringLiteral resourceUri;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The semicolon terminating the statement.
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
   * @param hash the hash mark introducing the directive
   * @param keyword the token representing the 'resource' keyword
   * @param leftParenthesis the left parenthesis
   * @param resourceUri the URI of the resource being specified
   * @param rightParenthesis the right parenthesis
   * @param semicolon the semicolon terminating the statement
   */
  public ResourceDirective(Token hash, Token keyword, Token leftParenthesis,
      StringLiteral resourceUri, Token rightParenthesis, Token semicolon) {
    this.hash = hash;
    this.keyword = keyword;
    this.leftParenthesis = leftParenthesis;
    this.resourceUri = becomeParentOf(resourceUri);
    this.rightParenthesis = rightParenthesis;
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitResourceDirective(this);
  }

  @Override
  public Token getBeginToken() {
    return hash;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the hash mark introducing the directive.
   * 
   * @return the hash mark introducing the directive
   */
  public Token getHash() {
    return hash;
  }

  /**
   * Return the token representing the 'resource' keyword.
   * 
   * @return the token representing the 'resource' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the left parenthesis.
   * 
   * @return the left parenthesis
   */
  public Token getLeftParenthesis() {
    return leftParenthesis;
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
   * Return the right parenthesis.
   * 
   * @return the right parenthesis
   */
  public Token getRightParenthesis() {
    return rightParenthesis;
  }

  /**
   * Return the semicolon terminating the statement.
   * 
   * @return the semicolon terminating the statement
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the hash mark introducing the directive to the given token.
   * 
   * @param hash the hash mark introducing the directive
   */
  public void setHash(Token hash) {
    this.hash = hash;
  }

  /**
   * Set the token representing the 'resource' keyword to the given token.
   * 
   * @param keyword the token representing the 'resource' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the left parenthesis to the given token.
   * 
   * @param parenthesis the left parenthesis
   */
  public void setLeftParenthesis(Token parenthesis) {
    leftParenthesis = parenthesis;
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
   * Set the right parenthesis to the given token.
   * 
   * @param parenthesis the right parenthesis
   */
  public void setRightParenthesis(Token parenthesis) {
    rightParenthesis = parenthesis;
  }

  /**
   * Set the semicolon terminating the statement to the given token.
   * 
   * @param semicolon the semicolon terminating the statement
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(resourceUri, visitor);
  }
}
