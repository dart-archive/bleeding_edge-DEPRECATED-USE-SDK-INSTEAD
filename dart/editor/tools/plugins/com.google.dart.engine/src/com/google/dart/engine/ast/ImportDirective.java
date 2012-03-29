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
 * Instances of the class <code>ImportDirective</code> represent an import directive.
 * 
 * <pre>
 * importDirective ::=
 *     '#' 'import' '(' {@link StringLiteral libraryUri} (',' {@link ImportCombinator importCombinator})? ')' ';'
 * </pre>
 */
public class ImportDirective extends Directive {
  /**
   * The hash mark introducing the directive.
   */
  private Token hash;

  /**
   * The token representing the 'import' keyword.
   */
  private Token keyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The URI of the library being imported.
   */
  private StringLiteral libraryUri;

  /**
   * The combinator used to control how names are imported, or <code>null</code> if there is no
   * combinator.
   */
  private ImportCombinator combinator;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created import directive.
   */
  public ImportDirective() {
  }

  /**
   * Initialize a newly created import directive.
   * 
   * @param hash the hash mark introducing the directive
   * @param keyword the token representing the 'import' keyword
   * @param leftParenthesis the left parenthesis
   * @param libraryUri the URI of the library being imported
   * @param combinator the combinator used to control how names are imported
   * @param rightParenthesis the right parenthesis
   * @param semicolon the semicolon terminating the statement
   */
  public ImportDirective(Token hash, Token keyword, Token leftParenthesis,
      StringLiteral libraryUri, ImportCombinator combinator, Token rightParenthesis, Token semicolon) {
    this.hash = hash;
    this.keyword = keyword;
    this.leftParenthesis = leftParenthesis;
    this.libraryUri = becomeParentOf(libraryUri);
    this.combinator = becomeParentOf(combinator);
    this.rightParenthesis = rightParenthesis;
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitImportDirective(this);
  }

  @Override
  public Token getBeginToken() {
    return hash;
  }

  /**
   * Return the combinator used to control how names are imported, or <code>null</code> if there is
   * no combinator.
   * 
   * @return the combinator used to control how names are imported
   */
  public ImportCombinator getCombinator() {
    return combinator;
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
   * Return the token representing the 'import' keyword.
   * 
   * @return the token representing the 'import' keyword
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
   * Return the URI of the library being imported.
   * 
   * @return the URI of the library being imported
   */
  public StringLiteral getLibraryUri() {
    return libraryUri;
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
   * Set the combinator used to control how names are imported to the given combinator.
   * 
   * @param combinator the combinator used to control how names are imported
   */
  public void setCombinator(ImportCombinator combinator) {
    this.combinator = combinator;
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
   * Set the token representing the 'import' keyword to the given token.
   * 
   * @param keyword the token representing the 'import' keyword
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
   * Set the URI of the library being imported to the given literal.
   * 
   * @param literal the URI of the library being imported
   */
  public void setLibraryUri(StringLiteral literal) {
    libraryUri = becomeParentOf(literal);
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
    safelyVisitChild(libraryUri, visitor);
    safelyVisitChild(combinator, visitor);
  }
}
