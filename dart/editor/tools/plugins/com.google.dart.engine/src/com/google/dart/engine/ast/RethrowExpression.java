/*
 * Copyright 2013, the Dart project authors.
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
 * Instances of the class {@code RethrowExpression} represent a rethrow expression.
 * 
 * <pre>
 * rethrowExpression ::=
 *     'rethrow'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class RethrowExpression extends Expression {
  /**
   * The token representing the 'rethrow' keyword.
   */
  private Token keyword;

  /**
   * Initialize a newly created rethrow expression.
   * 
   * @param keyword the token representing the 'rethrow' keyword
   */
  public RethrowExpression(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitRethrowExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return keyword;
  }

  /**
   * Return the token representing the 'rethrow' keyword.
   * 
   * @return the token representing the 'rethrow' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  @Override
  public int getPrecedence() {
    return 0;
  }

  /**
   * Set the token representing the 'rethrow' keyword to the given token.
   * 
   * @param keyword the token representing the 'rethrow' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    // There are no children to visit.
  }
}
