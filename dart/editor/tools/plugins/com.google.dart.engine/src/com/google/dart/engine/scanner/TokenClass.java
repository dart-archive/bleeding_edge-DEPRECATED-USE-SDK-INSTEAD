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
  ADDITIVE_OPERATOR(12),

  /**
   * A value used to indicate that the token type is an assignment operator.
   */
  ASSIGNMENT_OPERATOR(2),

  /**
   * A value used to indicate that the token type is a bitwise-and operator.
   */
  BITWISE_AND_OPERATOR(8),

  /**
   * A value used to indicate that the token type is a bitwise-or operator.
   */
  BITWISE_OR_OPERATOR(6),

  /**
   * A value used to indicate that the token type is a bitwise-xor operator.
   */
  BITWISE_XOR_OPERATOR(7),

  /**
   * A value used to indicate that the token type is a cascade operator.
   */
  CASCADE_OPERATOR(1),

  /**
   * A value used to indicate that the token type is a conditional operator.
   */
  CONDITIONAL_OPERATOR(3),

  /**
   * A value used to indicate that the token type is an equality operator.
   */
  EQUALITY_OPERATOR(9),

  /**
   * A value used to indicate that the token type is an increment operator.
   */
  INCREMENT_OPERATOR(14), // POSTFIX_OPERATOR

  /**
   * A value used to indicate that the token type is a logical-and operator.
   */
  LOGICAL_AND_OPERATOR(5),

  /**
   * A value used to indicate that the token type is a logical-or operator.
   */
  LOGICAL_OR_OPERATOR(4),

  /**
   * A value used to indicate that the token type is a member access operator.
   */
  MEMBER_ACCESS_OPERATOR(14),

  /**
   * A value used to indicate that the token type is a multiplicative operator.
   */
  MULTIPLICATIVE_OPERATOR(13),

  /**
   * A value used to indicate that the token type is a relational operator.
   */
  RELATIONAL_OPERATOR(10),

  /**
   * A value used to indicate that the token type is a shift operator.
   */
  SHIFT_OPERATOR(11);

  /**
   * The precedence of tokens of this class, or <code>0</code> if the such tokens do not represent
   * an operator.
   */
  private int precedence;

  private TokenClass() {
    this(0);
  }

  private TokenClass(int precedence) {
    this.precedence = precedence;
  }

  /**
   * Return the precedence of tokens of this class, or <code>0</code> if the such tokens do not
   * represent an operator.
   * 
   * @return the precedence of tokens of this class
   */
  public int getPrecedence() {
    return precedence;
  }
}
