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
 * Instances of the class {@code NullLiteral} represent a null literal expression.
 * 
 * <pre>
 * nullLiteral ::=
 *     'null'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class NullLiteral extends Literal {
  /**
   * The token representing the literal.
   */
  private Token literal;

  /**
   * Initialize a newly created null literal.
   * 
   * @param token the token representing the literal
   */
  public NullLiteral(Token token) {
    this.literal = token;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitNullLiteral(this);
  }

  @Override
  public Token getBeginToken() {
    return literal;
  }

  @Override
  public Token getEndToken() {
    return literal;
  }

  /**
   * Return the token representing the literal.
   * 
   * @return the token representing the literal
   */
  public Token getLiteral() {
    return literal;
  }

  /**
   * Set the token representing the literal to the given token.
   * 
   * @param literal the token representing the literal
   */
  public void setLiteral(Token literal) {
    this.literal = literal;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    // There are no children to visit.
  }
}
