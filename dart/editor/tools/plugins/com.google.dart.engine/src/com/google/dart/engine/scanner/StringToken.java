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

import com.google.dart.engine.utilities.general.StringUtilities;

/**
 * Instances of the class {@code StringToken} represent a token whose value is independent of it's
 * type.
 * 
 * @coverage dart.engine.parser
 */
public class StringToken extends Token {
  /**
   * The lexeme represented by this token.
   */
  final String value;

  /**
   * Initialize a newly created token to represent a token of the given type with the given value.
   * 
   * @param type the type of the token
   * @param value the lexeme represented by this token
   * @param offset the offset from the beginning of the file to the first character in the token
   */
  public StringToken(TokenType type, String value, int offset) {
    super(type, offset);
    this.value = StringUtilities.intern(value);
  }

  @Override
  public Token copy() {
    return new StringToken(getType(), value, getOffset());
  }

  @Override
  public String getLexeme() {
    return value;
  }

  @Override
  public String value() {
    return value;
  }
}
