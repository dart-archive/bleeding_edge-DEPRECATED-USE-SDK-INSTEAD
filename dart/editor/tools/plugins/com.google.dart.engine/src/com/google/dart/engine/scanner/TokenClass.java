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
 * The enumeration <code>TokenClass</code> represents classes (or groups) of tokens with a similar
 * use.
 */
public enum TokenClass {
  /**
   * A value used to indicate that the token type is not part of any specific class of token.
   */
  NO_CLASS,

  /**
   * A value used to indicate that the token type is an additive operator.
   */
  ADDITIVE_OPERATOR,

  /**
   * A value used to indicate that the token type is an assignment operator.
   */
  ASSIGNMENT_OPERATOR,

  /**
   * A value used to indicate that the token type is an equality operator.
   */
  EQUALITY_OPERATOR,

  /**
   * A value used to indicate that the token type is an increment operator.
   */
  INCREMENT_OPERATOR,

  /**
   * A value used to indicate that the token type is a multiplicative operator.
   */
  MULTIPLICATIVE_OPERATOR,

  /**
   * A value used to indicate that the token type is a relational operator.
   */
  RELATIONAL_OPERATOR,

  /**
   * A value used to indicate that the token type is a shift operator.
   */
  SHIFT_OPERATOR;
}
