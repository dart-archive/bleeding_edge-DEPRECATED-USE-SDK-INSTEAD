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
 * The abstract class {@code NamespaceDirective} defines the behavior common to nodes that represent
 * a directive that impacts the namespace of a library.
 * 
 * <pre>
 * directive ::=
 *     {@link ExportDirective exportDirective}
 *   | {@link ImportDirective importDirective}
 * </pre>
 */
public abstract class NamespaceDirective extends Directive {
  /**
   * The token representing the 'import' or 'export' keyword.
   */
  private Token keyword;

  /**
   * The URI of the library being imported or exported.
   */
  private StringLiteral libraryUri;

  /**
   * The combinators used to control which names are imported or exported.
   */
  private NodeList<Combinator> combinators = new NodeList<Combinator>(this);

  /**
   * The semicolon terminating the directive.
   */
  private Token semicolon;

  /**
   * Initialize a newly created namespace directive.
   */
  public NamespaceDirective() {
  }

  /**
   * Initialize a newly created namespace directive.
   * 
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param keyword the token representing the 'import' or 'export' keyword
   * @param libraryUri the URI of the library being imported or exported
   * @param combinators the combinators used to control which names are imported or exported
   * @param semicolon the semicolon terminating the directive
   */
  public NamespaceDirective(Comment comment, List<Annotation> metadata, Token keyword,
      StringLiteral libraryUri, List<Combinator> combinators, Token semicolon) {
    super(comment, metadata);
    this.keyword = keyword;
    this.libraryUri = becomeParentOf(libraryUri);
    this.combinators.addAll(combinators);
    this.semicolon = semicolon;
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  /**
   * Return the combinators used to control how names are imported or exported.
   * 
   * @return the combinators used to control how names are imported or exported
   */
  public NodeList<Combinator> getCombinators() {
    return combinators;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the 'import' or 'export' keyword.
   * 
   * @return the token representing the 'import' or 'export' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the URI of the library being imported or exported.
   * 
   * @return the URI of the library being imported or exported
   */
  public StringLiteral getLibraryUri() {
    return libraryUri;
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
   * Set the token representing the 'import' or 'export' keyword to the given token.
   * 
   * @param exportToken the token representing the 'import' or 'export' keyword
   */
  public void setKeyword(Token exportToken) {
    this.keyword = exportToken;
  }

  /**
   * Set the URI of the library being imported or exported to the given literal.
   * 
   * @param literal the URI of the library being imported or exported
   */
  public void setLibraryUri(StringLiteral literal) {
    libraryUri = becomeParentOf(literal);
  }

  /**
   * Set the semicolon terminating the directive to the given token.
   * 
   * @param semicolon the semicolon terminating the directive
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }
}
