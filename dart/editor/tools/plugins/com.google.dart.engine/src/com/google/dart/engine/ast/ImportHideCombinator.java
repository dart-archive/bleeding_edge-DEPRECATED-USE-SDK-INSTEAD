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
 * Instances of the class <code>ImportHideCombinator</code> represent a combinator that restricts
 * the names being imported to those that are not in a given list.
 * 
 * <pre>
 * importHideCombinator ::=
 *     'hide:' {@link ListLiteral listLiteral}
 * </pre>
 */
public class ImportHideCombinator extends ImportCombinator {
  /**
   * The list of names from the library that are hidden by this combinator.
   */
  private ListLiteral hiddenNames;

  /**
   * Initialize a newly created import show combinator.
   */
  public ImportHideCombinator() {
    super();
  }

  /**
   * Initialize a newly created import show combinator.
   * 
   * @param comma the comma introducing the combinator
   * @param keyword the comma introducing the combinator
   * @param colon the colon separating the keyword from the following literal
   * @param hiddenNames the list of names from the library that are hidden by this combinator
   */
  public ImportHideCombinator(Token comma, Token keyword, Token colon, ListLiteral hiddenNames) {
    super(comma, keyword, colon);
    this.hiddenNames = hiddenNames;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitImportHideCombinator(this);
  }

  @Override
  public Token getEndToken() {
    return hiddenNames.getEndToken();
  }

  /**
   * Return the list of names from the library that are hidden by this combinator.
   * 
   * @return the list of names from the library that are hidden by this combinator
   */
  public ListLiteral getHiddenNames() {
    return hiddenNames;
  }

  /**
   * Set the list of names from the library that are hidden by this combinator to the given list.
   * 
   * @param hiddenNames the list of names from the library that are hidden by this combinator
   */
  public void setHiddenNames(ListLiteral hiddenNames) {
    this.hiddenNames = becomeParentOf(hiddenNames);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(hiddenNames, visitor);
  }
}
