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
 * Instances of the class <code>KeywordToken</code> represent a keyword in the language.
 */
public class KeywordToken extends Token {
  /**
   * The keyword being represented by this token.
   */
  private final Keyword keyword;

  /**
   * Initialize a newly created token to represent the given keyword.
   * 
   * @param keyword the keyword being represented by this token
   * @param offset the offset from the beginning of the file to the first character in the token
   */
  public KeywordToken(Keyword keyword, int offset) {
    super(TokenType.KEYWORD, offset);
    this.keyword = keyword;
  }

  @Override
  public String getLexeme() {
    return keyword.getSyntax();
  }

  @Override
  public Keyword value() {
    return keyword;
  }
}
