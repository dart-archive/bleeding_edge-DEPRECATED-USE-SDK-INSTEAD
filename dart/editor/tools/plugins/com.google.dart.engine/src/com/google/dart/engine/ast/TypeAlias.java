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
 * The abstract class {@code TypeAlias} defines the behavior common to declarations of type aliases.
 * 
 * <pre>
 * typeAlias ::=
 *     'typedef' typeAliasBody
 * 
 * typeAliasBody ::=
 *     classTypeAlias
 *   | functionTypeAlias
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public abstract class TypeAlias extends CompilationUnitMember {
  /**
   * The token representing the 'typedef' keyword.
   */
  private Token keyword;

  /**
   * The semicolon terminating the declaration.
   */
  private Token semicolon;

  /**
   * Initialize a newly created type alias.
   * 
   * @param comment the documentation comment associated with this type alias
   * @param metadata the annotations associated with this type alias
   * @param keyword the token representing the 'typedef' keyword
   * @param semicolon the semicolon terminating the declaration
   */
  public TypeAlias(Comment comment, List<Annotation> metadata, Token keyword, Token semicolon) {
    super(comment, metadata);
    this.keyword = keyword;
    this.semicolon = semicolon;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the 'typedef' keyword.
   * 
   * @return the token representing the 'typedef' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the semicolon terminating the declaration.
   * 
   * @return the semicolon terminating the declaration
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the token representing the 'typedef' keyword to the given token.
   * 
   * @param keyword the token representing the 'typedef' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the semicolon terminating the declaration to the given token.
   * 
   * @param semicolon the semicolon terminating the declaration
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    return keyword;
  }
}
