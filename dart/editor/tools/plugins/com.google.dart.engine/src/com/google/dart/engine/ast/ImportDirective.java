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
 *     {@link Annotation metadata} 'import' {@link StringLiteral libraryUri} ('as' identifier)? {@link Combinator combinator}* ';'
 * </pre>
 */
public class ImportDirective extends NamespaceDirective {
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
   * Initialize a newly created import directive.
   */
  public ImportDirective() {
  }

  /**
   * Initialize a newly created import directive.
   * 
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param keyword the token representing the 'import' keyword
   * @param libraryUri the URI of the library being imported
   * @param asToken the token representing the 'as' token
   * @param prefix the prefix to be used with the imported names
   * @param combinators the combinators used to control how names are imported
   * @param semicolon the semicolon terminating the directive
   */
  public ImportDirective(Comment comment, List<Annotation> metadata, Token keyword,
      StringLiteral libraryUri, Token asToken, SimpleIdentifier prefix,
      List<Combinator> combinators, Token semicolon) {
    super(comment, metadata, keyword, libraryUri, combinators, semicolon);
    this.asToken = asToken;
    this.prefix = becomeParentOf(prefix);
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
   * Set the token representing the 'as' token to the given token.
   * 
   * @param asToken the token representing the 'as' token
   */
  public void setAsToken(Token asToken) {
    this.asToken = asToken;
  }

  /**
   * Set the prefix to be used with the imported names to the given identifier.
   * 
   * @param prefix the prefix to be used with the imported names
   */
  public void setPrefix(SimpleIdentifier prefix) {
    this.prefix = becomeParentOf(prefix);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(getLibraryUri(), visitor);
    safelyVisitChild(prefix, visitor);
    getCombinators().accept(visitor);
  }
}
