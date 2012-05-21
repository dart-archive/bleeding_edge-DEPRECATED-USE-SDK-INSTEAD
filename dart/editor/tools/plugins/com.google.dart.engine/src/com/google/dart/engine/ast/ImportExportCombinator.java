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
 * Instances of the class <code>ImportExportCombinator</code> represent a combinator that restricts
 * the names being imported to those in a given list.
 * 
 * <pre>
 * importExportCombinator ::=
 *     'export:' {@link BooleanLiteral shouldExport}
 * </pre>
 */
public class ImportExportCombinator extends ImportCombinator {
  /**
   * The boolean literal indicating whether the imported names should be re-exported.
   */
  private BooleanLiteral shouldExport;

  /**
   * Initialize a newly created import show combinator.
   */
  public ImportExportCombinator() {
    super();
  }

  /**
   * Initialize a newly created import show combinator.
   * 
   * @param comma the comma introducing the combinator
   * @param keyword the comma introducing the combinator
   * @param colon the colon separating the keyword from the following literal
   * @param shouldExport the boolean literal indicating whether the imported names should be
   *          re-exported
   */
  public ImportExportCombinator(Token comma, Token keyword, Token colon, BooleanLiteral shouldExport) {
    super(comma, keyword, colon);
    this.shouldExport = shouldExport;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitImportExportCombinator(this);
  }

  @Override
  public Token getEndToken() {
    return shouldExport.getEndToken();
  }

  /**
   * Return the boolean literal indicating whether the imported names should be re-exported.
   * 
   * @return the boolean literal indicating whether the imported names should be re-exported
   */
  public BooleanLiteral getShownNames() {
    return shouldExport;
  }

  /**
   * Set the boolean literal indicating whether the imported names should be re-exported to the
   * given literal.
   * 
   * @param shouldExport the boolean literal indicating whether the imported names should be
   *          re-exported
   */
  public void setShownNames(BooleanLiteral shouldExport) {
    this.shouldExport = becomeParentOf(shouldExport);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(shouldExport, visitor);
  }
}
