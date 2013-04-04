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

import com.google.dart.engine.utilities.general.StringUtilities;

/**
 * Instances of the class {@code Token} represent a token that was scanned from the input. Each
 * token knows which token follows it, acting as the head of a linked list of tokens.
 * 
 * @coverage dart.engine.html
 */
public class Token {
  /**
   * The offset from the beginning of the file to the first character in the token.
   */
  private int offset;

  /**
   * The previous token in the token stream.
   */
  private Token previous;

  /**
   * The next token in the token stream.
   */
  private Token next;

  /**
   * The type of the token.
   */
  private final TokenType type;

  /**
   * The lexeme represented by this token.
   */
  private final String value;

  /**
   * Initialize a newly created token.
   * 
   * @param type the token type (not {@code null})
   * @param offset the offset from the beginning of the file to the first character in the token
   */
  public Token(TokenType type, int offset) {
    this(type, offset, type.getLexeme());
  }

  /**
   * Initialize a newly created token.
   * 
   * @param type the token type (not {@code null})
   * @param offset the offset from the beginning of the file to the first character in the token
   * @param value the lexeme represented by this token (not {@code null})
   */
  public Token(TokenType type, int offset, String value) {
    this.type = type;
    this.value = StringUtilities.intern(value);
    this.offset = offset;
  }

  /**
   * Return the offset from the beginning of the file to the character after last character of the
   * token.
   * 
   * @return the offset from the beginning of the file to the first character after last character
   *         of the token
   */
  public int getEnd() {
    return offset + getLength();
  }

  /**
   * Return the number of characters in the node's source range.
   * 
   * @return the number of characters in the node's source range
   */
  public int getLength() {
    return getLexeme().length();
  }

  /**
   * Return the lexeme that represents this token.
   * 
   * @return the lexeme (not {@code null})
   */
  public String getLexeme() {
    return value;
  }

  /**
   * Return the next token in the token stream.
   * 
   * @return the next token in the token stream
   */
  public Token getNext() {
    return next;
  }

  /**
   * Return the offset from the beginning of the file to the first character in the token.
   * 
   * @return the offset from the beginning of the file to the first character in the token
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Return the previous token in the token stream.
   * 
   * @return the previous token in the token stream
   */
  public Token getPrevious() {
    return previous;
  }

  /**
   * Answer the token type for the receiver.
   * 
   * @return the token type (not {@code null})
   */
  public TokenType getType() {
    return type;
  }

  /**
   * Return {@code true} if this token is a synthetic token. A synthetic token is a token that was
   * introduced by the parser in order to recover from an error in the code. Synthetic tokens always
   * have a length of zero ({@code 0}).
   * 
   * @return {@code true} if this token is a synthetic token
   */
  public boolean isSynthetic() {
    return getLength() == 0;
  }

  /**
   * Set the next token in the token stream to the given token. This has the side-effect of setting
   * this token to be the previous token for the given token.
   * 
   * @param token the next token in the token stream
   * @return the token that was passed in
   */
  public Token setNext(Token token) {
    next = token;
    token.setPrevious(this);
    return token;
  }

  @Override
  public String toString() {
    return getLexeme();
  }

  /**
   * Set the previous token in the token stream to the given token.
   * 
   * @param previous the previous token in the token stream
   */
  private void setPrevious(Token previous) {
    this.previous = previous;
  }
}
