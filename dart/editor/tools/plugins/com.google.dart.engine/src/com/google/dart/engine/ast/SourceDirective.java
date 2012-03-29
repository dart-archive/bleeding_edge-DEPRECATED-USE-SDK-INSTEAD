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
 * Instances of the class <code>SourceDirective</code> represent a source directive.
 * 
 * <pre>
 * sourceDirective ::=
 *     '#' 'source' '(' {@link StringLiteral sourceUri} ')' ';'
 * </pre>
 */
public class SourceDirective extends Directive {
  /**
   * The hash mark introducing the directive.
   */
  private Token hash;

  /**
   * The token representing the 'source' keyword.
   */
  private Token keyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The URI of the source file being included.
   */
  private StringLiteral sourceUri;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created source directive.
   */
  public SourceDirective() {
  }

  /**
   * Initialize a newly created source directive.
   * 
   * @param hash the hash mark introducing the directive
   * @param keyword the token representing the 'source' keyword
   * @param leftParenthesis the left parenthesis
   * @param sourceUri the URI of the source file being included
   * @param rightParenthesis the right parenthesis
   * @param semicolon the semicolon terminating the statement
   */
  public SourceDirective(Token hash, Token keyword, Token leftParenthesis, StringLiteral sourceUri,
      Token rightParenthesis, Token semicolon) {
    this.hash = hash;
    this.keyword = keyword;
    this.leftParenthesis = leftParenthesis;
    this.sourceUri = becomeParentOf(sourceUri);
    this.rightParenthesis = rightParenthesis;
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSourceDirective(this);
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
   * Return the token representing the 'source' keyword.
   * 
   * @return the token representing the 'source' keyword
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
   * Return the URI of the source file being included.
   * 
   * @return the URI of the source file being included
   */
  public StringLiteral getSourceUri() {
    return sourceUri;
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
   * Set the token representing the 'source' keyword to the given token.
   * 
   * @param keyword the token representing the 'source' keyword
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

  /**
   * Set the URI of the source file being included to the given string.
   * 
   * @param string the URI of the source file being included
   */
  public void setSourceUri(StringLiteral string) {
    sourceUri = becomeParentOf(string);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(sourceUri, visitor);
  }
}
