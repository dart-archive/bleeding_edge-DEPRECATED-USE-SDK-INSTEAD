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
 * Instances of the class <code>ImportShowCombinator</code> represent a combinator that restricts
 * the names being imported to those in a given list.
 * 
 * <pre>
 * importShowCombinator ::=
 *     'show:' {@link ListLiteral listLiteral}
 * </pre>
 */
public class ImportShowCombinator extends ImportCombinator {
  /**
   * The list of names that are imported from the library.
   */
  private ListLiteral visibleNames;

  /**
   * Initialize a newly created import show combinator.
   */
  public ImportShowCombinator() {
    super();
  }

  /**
   * Initialize a newly created import show combinator.
   * 
   * @param comma the comma introducing the combinator
   * @param keyword the comma introducing the combinator
   * @param colon the colon separating the keyword from the following literal
   * @param visibleNames the list of names that are imported from the library
   */
  public ImportShowCombinator(Token comma, Token keyword, Token colon, ListLiteral visibleNames) {
    super(comma, keyword, colon);
    this.visibleNames = visibleNames;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitImportShowCombinator(this);
  }

  @Override
  public Token getEndToken() {
    return visibleNames.getEndToken();
  }

  /**
   * Return the list of names that are imported from the library.
   * 
   * @return the list of names that are imported from the library
   */
  public ListLiteral getVisibleNames() {
    return visibleNames;
  }

  /**
   * Set the list of names that are imported from the library to the given list.
   * 
   * @param visibleNames the list of names that are imported from the library
   */
  public void setVisibleNames(ListLiteral visibleNames) {
    this.visibleNames = becomeParentOf(visibleNames);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(visibleNames, visitor);
  }
}
