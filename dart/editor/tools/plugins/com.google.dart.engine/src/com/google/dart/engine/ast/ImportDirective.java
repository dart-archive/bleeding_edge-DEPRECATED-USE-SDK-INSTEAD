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
 * Instances of the class {@code ImportDirective} represent an import directive.
 * 
 * <pre>
 * importDirective ::=
 *     {@link Annotation metadata} 'import' {@link StringLiteral libraryUri} ('as' identifier)? {@link ImportCombinator combinator}* ';'
 * </pre>
 */
public class ImportDirective extends Directive {
  /**
   * The token representing the 'import' token.
   */
  private Token importToken;

  /**
   * The URI of the library being imported.
   */
  private StringLiteral libraryUri;

  /**
   * The token representing the 'as' token, or {@code null} if the imported names are not prefixed.
   */
  private Token asToken;

  /**
   * The prefix to be used with the imported names, or {@code null} if the imported names are not
   * prefixed.
   */
  private SimpleIdentifier prefix;

  /**
   * The combinators used to control how names are imported.
   */
  private NodeList<ImportCombinator> combinators = new NodeList<ImportCombinator>(this);

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
   * @param metadata the annotations associated with the directive
   * @param importToken the token representing the 'import' token
   * @param libraryUri the URI of the library being imported
   * @param asToken the token representing the 'as' token
   * @param prefix the prefix to be used with the imported names
   * @param combinators the combinators used to control how names are imported
   * @param semicolon the semicolon terminating the statement
   */
  public ImportDirective(List<Annotation> metadata, Token importToken, StringLiteral libraryUri,
      Token asToken, SimpleIdentifier prefix, List<ImportCombinator> combinators, Token semicolon) {
    super(metadata);
    this.importToken = importToken;
    this.libraryUri = becomeParentOf(libraryUri);
    this.asToken = asToken;
    this.prefix = becomeParentOf(prefix);
    this.combinators.addAll(combinators);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitImportDirective(this);
  }

  /**
   * Return the token representing the 'as' token, or {@code null} if the imported names are not
   * prefixed.
   * 
   * @return the token representing the 'as' token
   */
  public Token getAsToken() {
    return asToken;
  }

  @Override
  public Token getBeginToken() {
    return importToken;
  }

  /**
   * Return the combinators used to control how names are imported.
   * 
   * @return the combinators used to control how names are imported
   */
  public NodeList<ImportCombinator> getCombinators() {
    return combinators;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the 'import' token.
   * 
   * @return the token representing the 'import' token
   */
  public Token getImportToken() {
    return importToken;
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
   * Return the prefix to be used with the imported names, or {@code null} if the imported names are
   * not prefixed.
   * 
   * @return the prefix to be used with the imported names
   */
  public SimpleIdentifier getPrefix() {
    return prefix;
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
   * Set the token representing the 'as' token to the given token.
   * 
   * @param asToken the token representing the 'as' token
   */
  public void setAsToken(Token asToken) {
    this.asToken = asToken;
  }

  /**
   * Set the token representing the 'import' token to the given token.
   * 
   * @param importToken the token representing the 'import' token
   */
  public void setImportToken(Token importToken) {
    this.importToken = importToken;
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
   * Set the prefix to be used with the imported names to the given identifier.
   * 
   * @param prefix the prefix to be used with the imported names
   */
  public void setPrefix(SimpleIdentifier prefix) {
    this.prefix = becomeParentOf(prefix);
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
    super.visitChildren(visitor);
    safelyVisitChild(libraryUri, visitor);
    safelyVisitChild(prefix, visitor);
    combinators.accept(visitor);
  }
}
