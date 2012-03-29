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
 * Instances of the class <code>ImportCombinator</code> represent the combinator associated with an
 * import directive.
 * 
 * <pre>
 * importCombinator ::=
 *     {@link ImportPrefixCombinator importPrefixCombinator}
 *   | {@link ImportShowCombinator importShowCombinator}
 * </pre>
 */
public abstract class ImportCombinator extends ASTNode {
  /**
   * The comma introducing the combinator.
   */
  private Token comma;

  /**
   * The keyword specifying what kind of processing is to be done on the imported names.
   */
  private Token keyword;

  /**
   * The colon separating the keyword from the following literal.
   */
  private Token colon;

  /**
   * Initialize a newly created import combinator.
   */
  public ImportCombinator() {
    super();
  }

  /**
   * Initialize a newly created import combinator.
   * 
   * @param comma the comma introducing the combinator
   * @param keyword the keyword specifying what kind of processing is to be done on the imported
   *          names
   * @param colon the colon separating the keyword from the following literal
   */
  public ImportCombinator(Token comma, Token keyword, Token colon) {
    this.comma = comma;
    this.keyword = keyword;
    this.colon = colon;
  }

  @Override
  public Token getBeginToken() {
    return comma;
  }

  /**
   * Return the colon separating the keyword from the following literal.
   * 
   * @return the colon separating the keyword from the following literal
   */
  public Token getColon() {
    return colon;
  }

  /**
   * Return the comma introducing the combinator.
   * 
   * @return the comma introducing the combinator
   */
  public Token getComma() {
    return comma;
  }

  /**
   * Return the keyword specifying what kind of processing is to be done on the imported names.
   * 
   * @return the keyword specifying what kind of processing is to be done on the imported names
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Set the colon separating the keyword from the following literal to the given token.
   * 
   * @param colon the colon separating the keyword from the following literal
   */
  public void setColon(Token colon) {
    this.colon = colon;
  }

  /**
   * Set the comma introducing the combinator to the given token.
   * 
   * @param comma the comma introducing the combinator
   */
  public void setComma(Token comma) {
    this.comma = comma;
  }

  /**
   * Set the keyword specifying what kind of processing is to be done on the imported names to the
   * given token.
   * 
   * @param keyword the keyword specifying what kind of processing is to be done on the imported
   *          names
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }
}
