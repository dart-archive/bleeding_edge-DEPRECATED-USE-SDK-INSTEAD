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
 * Instances of the class <code>EmptyStatement</code> represent an empty statement.
 * 
 * <pre>
 * emptyStatement ::=
 *     ';'
 * </pre>
 */
public class EmptyStatement extends Statement {
  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created empty statement.
   */
  public EmptyStatement() {
  }

  /**
   * Initialize a newly created empty statement.
   * 
   * @param semicolon the semicolon terminating the statement
   */
  public EmptyStatement(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitEmptyStatement(this);
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
   * Return the semicolon terminating the statement.
   * 
   * @return the semicolon terminating the statement
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the semicolon terminating the statement to the given token.
   * 
   * @param semicolon the semicolon terminating the statement
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    // There are no children to visit.
  }
}
