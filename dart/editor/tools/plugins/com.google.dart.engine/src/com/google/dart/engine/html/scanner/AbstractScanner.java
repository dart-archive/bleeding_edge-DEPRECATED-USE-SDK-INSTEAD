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

import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.collection.IntList;
import com.google.dart.engine.utilities.general.StringUtilities;

import static com.google.dart.engine.html.scanner.TokenType.COMMENT;
import static com.google.dart.engine.html.scanner.TokenType.DECLARATION;
import static com.google.dart.engine.html.scanner.TokenType.DIRECTIVE;
import static com.google.dart.engine.html.scanner.TokenType.EOF;
import static com.google.dart.engine.html.scanner.TokenType.EQ;
import static com.google.dart.engine.html.scanner.TokenType.GT;
import static com.google.dart.engine.html.scanner.TokenType.LT;
import static com.google.dart.engine.html.scanner.TokenType.LT_SLASH;
import static com.google.dart.engine.html.scanner.TokenType.SLASH_GT;
import static com.google.dart.engine.html.scanner.TokenType.STRING;
import static com.google.dart.engine.html.scanner.TokenType.TAG;
import static com.google.dart.engine.html.scanner.TokenType.TEXT;

/**
 * The abstract class {@code AbstractScanner} implements a scanner for HTML code. Subclasses are
 * required to implement the interface used to access the characters being scanned.
 * 
 * @coverage dart.engine.html
 */
public abstract class AbstractScanner {
  private static final String[] NO_PASS_THROUGH_ELEMENTS = new String[] {};

  /**
   * The source being scanned.
   */
  private final Source source;

  /**
   * The token pointing to the head of the linked list of tokens.
   */
  private Token tokens;

  /**
   * The last token that was scanned.
   */
  private Token tail;

  /**
   * A list containing the offsets of the first character of each line in the source code.
   */
  private IntList lineStarts = new IntList();

  /**
   * An array of element tags for which the content between tags should be consider a single token.
   */
  private String[] passThroughElements = NO_PASS_THROUGH_ELEMENTS;

  /**
   * Initialize a newly created scanner.
   * 
   * @param source the source being scanned
   */
  public AbstractScanner(Source source) {
    this.source = source;
    tokens = new Token(EOF, -1);
    tokens.setNext(tokens);
    tail = tokens;
    recordStartOfLine();
  }

  /**
   * Return an array containing the offsets of the first character of each line in the source code.
   * 
   * @return an array containing the offsets of the first character of each line in the source code
   */
  public int[] getLineStarts() {
    return lineStarts.toArray();
  }

  /**
   * Return the current offset relative to the beginning of the file. Return the initial offset if
   * the scanner has not yet scanned the source code, and one (1) past the end of the source code if
   * the source code has been scanned.
   * 
   * @return the current offset of the scanner in the source
   */
  public abstract int getOffset();

  /**
   * Answer the source being scanned.
   * 
   * @return the source or {@code null} if undefined
   */
  public Source getSource() {
    return source;
  }

  /**
   * Set array of element tags for which the content between tags should be consider a single token.
   */
  public void setPassThroughElements(String[] passThroughElements) {
    this.passThroughElements = passThroughElements != null ? passThroughElements
        : NO_PASS_THROUGH_ELEMENTS;
  }

  /**
   * Scan the source code to produce a list of tokens representing the source.
   * 
   * @return the first token in the list of tokens that were produced
   */
  public Token tokenize() {
    scan();
    appendEofToken();
    return firstToken();
  }

  /**
   * Advance the current position and return the character at the new current position.
   * 
   * @return the character at the new current position
   */
  protected abstract int advance();

  /**
   * Return the substring of the source code between the start offset and the modified current
   * position. The current position is modified by adding the end delta.
   * 
   * @param start the offset to the beginning of the string, relative to the start of the file
   * @param endDelta the number of character after the current location to be included in the
   *          string, or the number of characters before the current location to be excluded if the
   *          offset is negative
   * @return the specified substring of the source code
   */
  protected abstract String getString(int start, int endDelta);

  /**
   * Return the character at the current position without changing the current position.
   * 
   * @return the character at the current position
   */
  protected abstract int peek();

  /**
   * Record the fact that we are at the beginning of a new line in the source.
   */
  protected void recordStartOfLine() {
    lineStarts.add(getOffset());
  }

  private void appendEofToken() {
    Token eofToken = new Token(EOF, getOffset());
    // The EOF token points to itself so that there is always infinite look-ahead.
    eofToken.setNext(eofToken);
    tail = tail.setNext(eofToken);
  }

  private Token emit(Token token) {
    tail.setNext(token);
    tail = token;
    return token;
  }

  private Token emitWithOffset(TokenType type, int start) {
    return emit(new Token(type, start));
  }

  private Token emitWithOffsetAndLength(TokenType type, int start, int count) {
    return emit(new Token(type, start, getString(start, count)));
  }

  private Token firstToken() {
    return tokens.getNext();
  }

  private int recordStartOfLineAndAdvance(int c) {
    if (c == '\r') {
      c = advance();
      if (c == '\n') {
        c = advance();
      }
      recordStartOfLine();
    } else if (c == '\n') {
      c = advance();
      recordStartOfLine();
    } else {
      c = advance();
    }
    return c;
  }

  private void scan() {
    boolean inBrackets = false;
    String endPassThrough = null;

    // <--, -->, <?, <, >, =, "***", '***', in brackets, normal

    int c = advance();
    while (c >= 0) {
      final int start = getOffset();

      if (c == '<') {
        c = advance();

        if (c == '!') {
          c = advance();

          if (c == '-' && peek() == '-') {
            // handle a comment
            c = advance();
            int dashCount = 1;
            while (c >= 0) {
              if (c == '-') {
                dashCount++;
              } else if (c == '>' && dashCount >= 2) {
                c = advance();
                break;
              } else {
                dashCount = 0;
              }
              c = recordStartOfLineAndAdvance(c);
            }
            emitWithOffsetAndLength(COMMENT, start, -1);
            // Capture <!--> and <!---> as tokens but report an error
            if (tail.getLength() < 7) {
              // TODO (danrubel): Report invalid HTML comment
            }

          } else {
            // handle a declaration
            while (c >= 0) {
              if (c == '>') {
                c = advance();
                break;
              }
              c = recordStartOfLineAndAdvance(c);
            }
            emitWithOffsetAndLength(DECLARATION, start, -1);
            if (!StringUtilities.endsWithChar(tail.getLexeme(), '>')) {
              // TODO (danrubel): Report missing '>' in directive
            }
          }

        } else if (c == '?') {
          // handle a directive
          while (c >= 0) {
            if (c == '?') {
              c = advance();
              if (c == '>') {
                c = advance();
                break;
              }
            } else {
              c = recordStartOfLineAndAdvance(c);
            }
          }
          emitWithOffsetAndLength(DIRECTIVE, start, -1);
          if (tail.getLength() < 4) {
            // TODO (danrubel): Report invalid directive
          }

        } else if (c == '/') {
          emitWithOffset(LT_SLASH, start);
          inBrackets = true;
          c = advance();

        } else {
          inBrackets = true;
          emitWithOffset(LT, start);
          // ignore whitespace in braces
          while (Character.isWhitespace(c)) {
            c = recordStartOfLineAndAdvance(c);
          }
          // get tag
          if (Character.isLetterOrDigit(c)) {
            int tagStart = getOffset();
            c = advance();
            while (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
              c = advance();
            }
            emitWithOffsetAndLength(TAG, tagStart, -1);
            // check tag against passThrough elements
            String tag = tail.getLexeme();
            for (String str : passThroughElements) {
              if (str.equals(tag)) {
                endPassThrough = "</" + str + ">";
                break;
              }
            }
          }

        }

      } else if (c == '>') {
        emitWithOffset(GT, start);
        inBrackets = false;
        c = advance();

        // if passThrough != null, read until we match it
        if (endPassThrough != null) {
          boolean endFound = false;
          int len = endPassThrough.length();
          int firstC = endPassThrough.charAt(0);
          int index = 0;
          int nextC = firstC;
          while (c >= 0) {
            if (c == nextC) {
              index++;
              if (index == len) {
                endFound = true;
                break;
              }
              nextC = endPassThrough.charAt(index);
            } else if (c == firstC) {
              index = 1;
              nextC = endPassThrough.charAt(1);
            } else {
              index = 0;
              nextC = firstC;
            }
            c = recordStartOfLineAndAdvance(c);
          }
          if (start + 1 < getOffset()) {
            if (endFound) {
              emitWithOffsetAndLength(TEXT, start + 1, -len);
              emitWithOffset(LT_SLASH, getOffset() - len + 1);
              emitWithOffsetAndLength(TAG, getOffset() - len + 3, -1);
            } else {
              emitWithOffsetAndLength(TEXT, start + 1, -1);
            }
          }
          endPassThrough = null;
        }

      } else if (c == '/' && peek() == '>') {
        advance();
        emitWithOffset(SLASH_GT, start);
        inBrackets = false;
        c = advance();

      } else if (!inBrackets) {
        c = recordStartOfLineAndAdvance(c);
        while (c != '<' && c >= 0) {
          c = recordStartOfLineAndAdvance(c);
        }
        emitWithOffsetAndLength(TEXT, start, -1);

      } else if (c == '"' || c == '\'') {
        // read a string
        int endQuote = c;
        c = advance();
        while (c >= 0) {
          if (c == endQuote) {
            c = advance();
            break;
          }
          c = recordStartOfLineAndAdvance(c);
        }
        emitWithOffsetAndLength(STRING, start, -1);

      } else if (c == '=') {
        // a non-char token
        emitWithOffset(EQ, start);
        c = advance();

      } else if (Character.isWhitespace(c)) {
        // ignore whitespace in braces
        do {
          c = recordStartOfLineAndAdvance(c);
        } while (Character.isWhitespace(c));

      } else if (Character.isLetterOrDigit(c)) {
        c = advance();
        while (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
          c = advance();
        }
        emitWithOffsetAndLength(TAG, start, -1);

      } else {
        // a non-char token
        emitWithOffsetAndLength(TEXT, start, 0);
        c = advance();

      }
    }
  }
}
