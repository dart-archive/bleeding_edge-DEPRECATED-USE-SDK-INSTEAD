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
 * Instances of the class {@code Combinator} represent the combinator associated with an import
 * directive.
 * 
 * <pre>
 * combinator ::=
 *     {@link HideCombinator hideCombinator}
 *   | {@link ShowCombinator showCombinator}
 * </pre>
 */
public abstract class Combinator extends ASTNode {
  /**
   * The keyword specifying what kind of processing is to be done on the imported names.
   */
  private Token keyword;

  /**
   * Initialize a newly created import combinator.
   */
  public Combinator() {
    super();
  }

  /**
   * Initialize a newly created import combinator.
   * 
   * @param keyword the keyword specifying what kind of processing is to be done on the imported
   *          names
   */
  public Combinator(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public Token getBeginToken() {
    return keyword;
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
