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
import com.google.dart.engine.scanner.TokenType;

import static com.google.dart.engine.scanner.TokenFactory.tokenFromType;

import junit.framework.TestCase;

public class TokenMapTest extends TestCase {
  public void test_creation() {
    assertNotNull(new TokenMap());
  }

  public void test_get_absent() {
    TokenMap tokenMap = new TokenMap();
    assertNull(tokenMap.get(tokenFromType(TokenType.AT)));
  }

  public void test_get_added() {
    TokenMap tokenMap = new TokenMap();
    Token key = tokenFromType(TokenType.AT);
    Token value = tokenFromType(TokenType.AT);
    tokenMap.put(key, value);
    assertSame(value, tokenMap.get(key));
  }
}
