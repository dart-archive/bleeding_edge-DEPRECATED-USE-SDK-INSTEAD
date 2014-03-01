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
 * Instances of the class {@code EmptyFunctionBody} represent an empty function body, which can only
 * appear in constructors or abstract methods.
 * 
 * <pre>
 * emptyFunctionBody ::=
 *     ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class EmptyFunctionBody extends FunctionBody {
  /**
   * The token representing the semicolon that marks the end of the function body.
   */
  private Token semicolon;

  /**
   * Initialize a newly created function body.
   * 
   * @param semicolon the token representing the semicolon that marks the end of the function body
   */
  public EmptyFunctionBody(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitEmptyFunctionBody(this);
  }

  @Override
  public Token getBeginToken() {
    return semicolon;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the semicolon that marks the end of the function body.
   * 
   * @return the token representing the semicolon that marks the end of the function body
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the token representing the semicolon that marks the end of the function body to the given
   * token.
   * 
   * @param semicolon the token representing the semicolon that marks the end of the function body
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    // Empty function bodies have no children.
  }
}
