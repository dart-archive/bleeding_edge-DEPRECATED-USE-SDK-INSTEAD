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
package com.google.dart.engine.scanner;

/**
 * Instances of the class <code>BeginToken</code> represent the opening half of a grouping pair of
 * tokens. This is used for curly brackets ('{'), parentheses ('('), and square brackets ('[').
 */
public class BeginToken extends Token {
  /**
   * The token that corresponds to this token.
   */
  private Token endToken;

  /**
   * Initialize a newly created token representing the opening half of a grouping pair of tokens.
   * 
   * @param type the type of the token
   * @param offset the offset from the beginning of the file to the first character in the token
   */
  public BeginToken(TokenType type, int offset) {
    super(type, offset);
    assert (type == TokenType.OPEN_CURLY_BRACKET || type == TokenType.OPEN_PAREN || type == TokenType.OPEN_SQUARE_BRACKET);
  }

  /**
   * Return the token that corresponds to this token.
   * 
   * @return the token that corresponds to this token
   */
  public Token getEndToken() {
    return endToken;
  }

  /**
   * Set the token that corresponds to this token to the given token.
   * 
   * @param token the token that corresponds to this token
   */
  public void setEndToken(Token token) {
    this.endToken = token;
  }
}
