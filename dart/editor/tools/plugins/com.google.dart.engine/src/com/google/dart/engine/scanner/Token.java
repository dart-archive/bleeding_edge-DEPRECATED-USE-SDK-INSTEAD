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
 * Instances of the class <code>Token</code> represent a token that was scanned from the input. Each
 * token knows which token follows it, acting as the head of a linked list of tokens.
 */
public class Token {
  // TODO(brianwilkerson) Implement this class.

  /**
   * Return the number of characters in the node's source range.
   * 
   * @return the number of characters in the node's source range
   */
  public int getLength() {
    return 0;
  }

  /**
   * Return the offset from the beginning of the file to the first character in the token.
   * 
   * @return the offset from the beginning of the file to the first character in the token
   */
  public int getOffset() {
    return 0;
  }
}
