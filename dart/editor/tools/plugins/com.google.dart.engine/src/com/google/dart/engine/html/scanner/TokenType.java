/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.html.scanner;

/**
 * The enumeration {@code TokenType} defines the types of tokens that can be returned by the
 * scanner.
 * 
 * @coverage dart.engine.html
 */
public enum TokenType {
  /**
   * The type of the token that marks the end of the input.
   */
  EOF("") {
    @Override
    public String toString() {
      return "-eof-";
    }
  },

  EQ("="),
  GT(">"),
  LT_SLASH("</"),
  LT("<"),
  SLASH_GT("/>"),

  COMMENT(null),
  DECLARATION(null), // e.g. <!DOCTYPE ...>
  DIRECTIVE(null), // e.g. <?xml ... ?>
  STRING(null),
  TAG(null),
  TEXT(null);

  /**
   * The lexeme that defines this type of token, or {@code null} if there is more than one possible
   * lexeme for this type of token.
   */
  private String lexeme;

  private TokenType(String lexeme) {
    this.lexeme = lexeme;
  }

  /**
   * Return the lexeme that defines this type of token, or {@code null} if there is more than one
   * possible lexeme for this type of token.
   * 
   * @return the lexeme that defines this type of token
   */
  public String getLexeme() {
    return lexeme;
  }
}
