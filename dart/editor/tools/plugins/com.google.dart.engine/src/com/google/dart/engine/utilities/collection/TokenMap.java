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
package com.google.dart.engine.utilities.collection;

import com.google.dart.engine.scanner.Token;

import java.util.HashMap;

/**
 * Instances of the class {@code TokenMap} map one set of tokens to another set of tokens.
 */
public class TokenMap {
  /**
   * A table mapping tokens to tokens. This should be replaced by a more performant implementation.
   * One possibility is a pair of parallel arrays, with keys being sorted by their offset and a
   * cursor indicating where to start searching.
   */
  private HashMap<Token, Token> map = new HashMap<Token, Token>();

  /**
   * Initialize a newly created (and hence empty) map from one set of tokens to another set of
   * tokens.
   */
  public TokenMap() {
    super();
  }

  /**
   * Return the token that is mapped to the given token, or {@code null} if there is no token
   * corresponding to the given token.
   * 
   * @param key the token being mapped to another token
   * @return the token that is mapped to the given token
   */
  public Token get(Token key) {
    return map.get(key);
  }

  /**
   * Map the key to the value.
   * 
   * @param key the token being mapped to the value
   * @param value the token to which the key will be mapped
   */
  public void put(Token key, Token value) {
    map.put(key, value);
  }
}
