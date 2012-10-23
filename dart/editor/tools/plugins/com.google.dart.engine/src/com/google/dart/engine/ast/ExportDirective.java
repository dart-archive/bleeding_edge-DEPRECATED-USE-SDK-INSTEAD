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
 * Instances of the class {@code ExportDirective} represent an export directive.
 * 
 * <pre>
 * exportDirective ::=
 *     {@link Annotation metadata} 'export' {@link StringLiteral libraryUri} {@link ImportCombinator combinator}* ';'
 * </pre>
 */
public class ExportDirective extends Directive {
  /**
   * The token representing the 'export' token.
   */
  private Token exportToken;

  /**
   * The URI of the library being exported.
   */
  private StringLiteral libraryUri;

  /**
   * The combinators used to control which names are exported.
   */
  private NodeList<ImportCombinator> combinators = new NodeList<ImportCombinator>(this);

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created export directive.
   */
  public ExportDirective() {
  }

  /**
   * Initialize a newly created export directive.
   * 
   * @param metadata the annotations associated with the directive
   * @param exportToken the token representing the 'export' token
   * @param libraryUri the URI of the library being exported
   * @param combinators the combinators used to control which names are exported
   * @param semicolon the semicolon terminating the statement
   */
  public ExportDirective(List<Annotation> metadata, Token exportToken, StringLiteral libraryUri,
      List<ImportCombinator> combinators, Token semicolon) {
    super(metadata);
    this.exportToken = exportToken;
    this.libraryUri = becomeParentOf(libraryUri);
    this.combinators.addAll(combinators);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitExportDirective(this);
  }

  @Override
  public Token getBeginToken() {
    return exportToken;
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
   * Return the token representing the 'export' token.
   * 
   * @return the token representing the 'export' token
   */
  public Token getExportToken() {
    return exportToken;
  }

  /**
   * Return the URI of the library being exported.
   * 
   * @return the URI of the library being exported
   */
  public StringLiteral getLibraryUri() {
    return libraryUri;
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
   * Set the token representing the 'export' token to the given token.
   * 
   * @param exportToken the token representing the 'export' token
   */
  public void setExportToken(Token exportToken) {
    this.exportToken = exportToken;
  }

  /**
   * Set the URI of the library being exported to the given literal.
   * 
   * @param literal the URI of the library being exported
   */
  public void setLibraryUri(StringLiteral literal) {
    libraryUri = becomeParentOf(literal);
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
    combinators.accept(visitor);
  }
}
