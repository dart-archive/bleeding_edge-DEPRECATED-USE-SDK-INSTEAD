/*
 * Copyright (c) 2012, the Dart project authors.
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
 * Instances of the class <code>ImportPrefixCombinator</code> represent a combinator that adds a
 * prefix to imported names.
 * 
 * <pre>
 * importPrefixCombinator ::=
 *     'prefix:' {@link StringLiteral prefix}
 * </pre>
 */
public class ImportPrefixCombinator extends ImportCombinator {
  /**
   * The prefix used when referencing top-level elements of the imported library.
   */
  private StringLiteral prefix;

  /**
   * Initialize a newly created import prefix combinator.
   */
  public ImportPrefixCombinator() {
    super();
  }

  /**
   * Initialize a newly created import prefix combinator.
   * 
   * @param comma the comma introducing the combinator
   * @param keyword the comma introducing the combinator
   * @param colon the colon separating the keyword from the following literal
   * @param prefix the prefix used when referencing top-level elements of the imported library
   */
  public ImportPrefixCombinator(Token comma, Token keyword, Token colon, StringLiteral prefix) {
    super(comma, keyword, colon);
    this.prefix = prefix;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitImportPrefixCombinator(this);
  }

  @Override
  public Token getEndToken() {
    return prefix.getEndToken();
  }

  /**
   * Return the prefix used when referencing top-level elements of the imported library.
   * 
   * @return the prefix used when referencing top-level elements of the imported library
   */
  public StringLiteral getPrefix() {
    return prefix;
  }

  /**
   * Set the prefix used when referencing top-level elements of the imported library to the given
   * literal.
   * 
   * @param literal the prefix used when referencing top-level elements of the imported library
   */
  public void setPrefix(StringLiteral literal) {
    prefix = becomeParentOf(literal);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(prefix, visitor);
  }
}
