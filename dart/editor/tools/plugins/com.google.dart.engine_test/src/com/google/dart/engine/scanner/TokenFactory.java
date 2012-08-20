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
 * The class {@code TokenFactory} defines utility methods that can be used to create tokens.
 */
public final class TokenFactory {
  public static Token token(Keyword keyword) {
    return new KeywordToken(keyword, 0);
  }

  public static Token token(String lexeme) {
    return new StringToken(TokenType.STRING, lexeme, 0);
  }

  public static Token token(TokenType type) {
    return new Token(type, 0);
  }

  public static Token token(TokenType type, String lexeme) {
    return new StringToken(type, lexeme, 0);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private TokenFactory() {
  }
}
