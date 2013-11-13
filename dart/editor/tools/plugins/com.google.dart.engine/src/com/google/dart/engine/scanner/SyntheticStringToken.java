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
 * Synthetic {@code StringToken} represent a token whose value is independent of it's type.
 * 
 * @coverage dart.engine.parser
 */
public class SyntheticStringToken extends StringToken {
  /**
   * Initialize a newly created token to represent a token of the given type with the given value.
   * 
   * @param type the type of the token
   * @param value the lexeme represented by this token
   * @param offset the offset from the beginning of the file to the first character in the token
   */
  public SyntheticStringToken(TokenType type, String value, int offset) {
    super(type, value, offset);
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }
}
