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

/**
 * Instances of the class <code>SuperExpression</code> represent a super expression.
 * 
 * <pre>
 * superExpression ::=
 *     'super'
 * </pre>
 */
public class SuperExpression extends Expression {
  /**
   * The token representing the keyword.
   */
  private Token keyword;

  /**
   * Initialize a newly created super expression.
   */
  public SuperExpression() {
  }

  /**
   * Initialize a newly created super expression.
   * 
   * @param keyword the token representing the keyword
   */
  public SuperExpression(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSuperExpression(this);
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
   * Return the token representing the keyword.
   * 
   * @return the token representing the keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Set the token representing the keyword to the given token.
   * 
   * @param keyword the token representing the keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    // There are no children to visit.
  }
}
