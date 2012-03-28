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
 * Instances of the class <code>SimpleIdentifier</code> represent a simple identifier.
 * 
 * <pre>
 * simpleIdentifier ::=
 *     initialCharacter internalCharacter*
 *
 * initialCharacter ::= '_' | '$' | letter
 *
 * internalCharacter ::= '_' | '$' | letter | digit
 * </pre>
 */
public class SimpleIdentifier extends Identifier {
  /**
   * The token representing the identifier.
   */
  private Token token;

  /**
   * Initialize a newly created identifier.
   */
  public SimpleIdentifier() {
  }

  /**
   * Initialize a newly created identifier.
   * 
   * @param token the token representing the identifier
   */
  public SimpleIdentifier(Token token) {
    this.token = token;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSimpleIdentifier(this);
  }

  @Override
  public Token getBeginToken() {
    return token;
  }

  @Override
  public Token getEndToken() {
    return token;
  }

  /**
   * Return the lexical representation of the identifier.
   * 
   * @return the lexical representation of the identifier
   */
  public String getIdentifier() {
    // TODO(brianwilkerson) Token needs a method specifically for this purpose.
    return token.toString();
  }

  /**
   * Return the token representing the identifier.
   * 
   * @return the token representing the identifier
   */
  public Token getToken() {
    return token;
  }

  /**
   * Set the token representing the identifier to the given token.
   * 
   * @param token the token representing the literal
   */
  public void setToken(Token token) {
    this.token = token;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    // There are no children to visit.
  }
}
