/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * This scanner recognizes doc comments, multi-line comments, single-line comments, strings, and
 * multi-line strings, in addition to the default.
 */
public class FastDartPartitionScanner implements IPartitionTokenScanner, DartPartitions {
  private static class StringState {
    /**
     * The state that was current before this state.
     */
    private StringState previous;

    /**
     * A flag indicating whether this string is a raw string.
     */
    private boolean raw;

    /**
     * The quote character used to start this string.
     */
    private int quote;

    /**
     * The number of quote characters (1 or 3) used to start this string.
     */
    private int quoteCount;

    /**
     * The number of unclosed braces that have been encountered in the current string interpolation.
     */
    private int braceCount;

    /**
     * Initialize a newly created string state to supersede the previous state.
     * 
     * @param previous the state that was current before this state
     * @param raw a flag indicating whether this string is a raw string
     * @param quote the quote character used to start this string
     * @param quoteCount the number of quote characters (1 or 3) used to start this string
     */
    public StringState(StringState previous, boolean raw, int quote, int quoteCount) {
      this.previous = previous;
      this.raw = raw;
      this.quote = quote;
      this.quoteCount = quoteCount;
      this.braceCount = 0;
    }
  }

  // states corresponding to partitions (used to do lookup in tokens)
  private static final int CODE = 0;
  private static final int SINGLE_LINE_COMMENT = 1;
  private static final int MULTI_LINE_COMMENT = 2;
  private static final int DOC_COMMENT = 3;
  private static final int STRING = 4;
  private static final int MULTI_LINE_STRING = 5;
  // other states
  private static final int SINGLE_LINE_COMMENT_PREFIX = 6;
  private static final int MULTI_LINE_COMMENT_PREFIX = 7;
  private static final int DOC_COMMENT_PREFIX = 8;
  private static final int RAW_STRING_PREFIX = 9;
  private static final int STRING_PREFIX = 10;
  private static final int RAW_MULTI_LINE_STRING_PREFIX = 11;
  private static final int MULTI_LINE_STRING_PREFIX = 12;
  private static final int SIMPLE_INTERPOLATION_PREFIX = 13;
  private static final int SIMPLE_INTERPOLATION = 14;
  private static final int BLOCK_INTERPOLATION_PREFIX = 15;
  private static final int BLOCK_INTERPOLATION = 16;

  private static int getState(String contentType) {

    if (contentType == null) {
      return CODE;
    } else if (contentType.equals(DART_SINGLE_LINE_COMMENT)) {
      return SINGLE_LINE_COMMENT;
    } else if (contentType.equals(DART_MULTI_LINE_COMMENT)) {
      return MULTI_LINE_COMMENT;
    } else if (contentType.equals(DART_DOC)) {
      return DOC_COMMENT;
    } else if (contentType.equals(DART_STRING)) {
      return STRING;
    } else if (contentType.equals(DART_MULTI_LINE_STRING)) {
      return MULTI_LINE_STRING;
    } else {
      return CODE;
    }
  }

  /**
   * The scanner used to read characters from the document.
   */
  private final BufferedDocumentScanner scanner = new BufferedDocumentScanner(1000); // faster implementation

  /**
   * The offset of the last returned token.
   */
  private int tokenOffset;

  /**
   * The length of the last returned token.
   */
  private int tokenLength;

  /**
   * At the beginning of a scan, the number of characters between the beginning of the partition and
   * the beginning of the range being scanned. At other times, zero (0).
   */
  private int prefixLength;

  /**
   * The state of the scanner.
   */
  private int scannerState;

  /**
   * The state of the string that we are currently parsing, or <code>null</code> if we are not
   * inside a string.
   */
  private StringState stringState = null;

  private final IToken[] tokens = new IToken[] {
      new Token(null), new Token(DART_SINGLE_LINE_COMMENT), new Token(DART_MULTI_LINE_COMMENT),
      new Token(DART_DOC), new Token(DART_STRING), new Token(DART_MULTI_LINE_STRING)};

  public FastDartPartitionScanner() {
    // create the scanner
  }

  @Override
  public int getTokenLength() {
    return tokenLength;
  }

  @Override
  public int getTokenOffset() {
    return tokenOffset;
  }

  @Override
  public IToken nextToken() {
    tokenOffset += tokenLength;
    tokenLength = prefixLength;
    prefixLength = 0;
    int currentChar = scanner.peek(0);
    while (currentChar != ICharacterScanner.EOF) {
      switch (scannerState) {
        case SINGLE_LINE_COMMENT_PREFIX:
          advance();
          advance();
          scannerState = SINGLE_LINE_COMMENT;
          break;
        case SINGLE_LINE_COMMENT:
          if (isEol(currentChar)) {
            scannerState = CODE;
            return tokens[SINGLE_LINE_COMMENT];
          }
          advance();
          break;
        case MULTI_LINE_COMMENT_PREFIX:
          advance();
          advance();
          scannerState = MULTI_LINE_COMMENT;
          break;
        case MULTI_LINE_COMMENT:
          if (currentChar == '*') {
            advance();
            if (scanner.peek(0) == '/') {
              advance();
              scannerState = CODE;
              return tokens[MULTI_LINE_COMMENT];
            }
          } else {
            advance();
          }
          break;
        case DOC_COMMENT_PREFIX:
          advance();
          advance();
          advance();
          scannerState = DOC_COMMENT;
          break;
        case DOC_COMMENT:
          if (currentChar == '*') {
            advance();
            if (scanner.peek(0) == '/') {
              advance();
              scannerState = CODE;
              return tokens[DOC_COMMENT];
            }
          } else {
            advance();
          }
          break;
        case RAW_STRING_PREFIX:
          advance();
        case STRING_PREFIX:
          advance();
          scannerState = STRING;
          break;
        case STRING:
          if (isEol(currentChar)) {
            stringState = stringState.previous;
            if (stringState == null) {
              scannerState = CODE;
            } else {
              scannerState = BLOCK_INTERPOLATION;
            }
            return tokens[STRING];
          } else if (currentChar == stringState.quote) {
            advance();
            stringState = stringState.previous;
            if (stringState == null) {
              scannerState = CODE;
            } else {
              scannerState = BLOCK_INTERPOLATION;
            }
            return tokens[STRING];
          } else if (currentChar == '\\') {
            advance();
            if (scanner.peek(0) == stringState.quote) {
              advance();
            }
          } else if (!stringState.raw && currentChar == '$') {
            if (scanner.peek(1) == '{') {
              scannerState = BLOCK_INTERPOLATION_PREFIX;
            } else {
              scannerState = SIMPLE_INTERPOLATION_PREFIX;
            }
            return tokens[STRING];
          } else {
            advance();
          }
          break;
        case RAW_MULTI_LINE_STRING_PREFIX:
          advance();
        case MULTI_LINE_STRING_PREFIX:
          advance();
          advance();
          advance();
          scannerState = MULTI_LINE_STRING;
          break;
        case MULTI_LINE_STRING:
          if (currentChar == stringState.quote) {
            advance();
            if (scanner.peek(0) == stringState.quote) {
              advance();
              if (scanner.peek(0) == stringState.quote) {
                advance();
                stringState = stringState.previous;
                if (stringState == null) {
                  scannerState = CODE;
                } else {
                  scannerState = BLOCK_INTERPOLATION;
                }
                return tokens[MULTI_LINE_STRING];
              }
            }
          } else if (currentChar == '\\') {
            advance();
            if (scanner.peek(0) == stringState.quote) {
              advance();
            }
          } else if (!stringState.raw && currentChar == '$') {
            if (scanner.peek(1) == '{') {
              scannerState = BLOCK_INTERPOLATION_PREFIX;
            } else {
              scannerState = SIMPLE_INTERPOLATION_PREFIX;
            }
            return tokens[MULTI_LINE_STRING];
          } else {
            advance();
          }
          break;
        case SIMPLE_INTERPOLATION_PREFIX:
          advance();
          scannerState = SIMPLE_INTERPOLATION;
          break;
        case SIMPLE_INTERPOLATION:
          if (!isIdentifierChar(currentChar)) {
            if (stringState.quoteCount == 1) {
              scannerState = STRING;
            } else {
              scannerState = MULTI_LINE_STRING;
            }
            return tokens[CODE];
          }
          advance();
          break;
        case BLOCK_INTERPOLATION_PREFIX:
          advance();
          advance();
          scannerState = BLOCK_INTERPOLATION;
          break;
        case BLOCK_INTERPOLATION:
          if (currentChar == '}') {
            if (stringState.braceCount == 0) {
              advance();
              if (stringState.quoteCount == 1) {
                scannerState = STRING;
              } else {
                scannerState = MULTI_LINE_STRING;
              }
              return tokens[CODE];
            } else {
              stringState.braceCount--;
            }
          } else if (currentChar == '{') {
            stringState.braceCount++;
          }
          // Intentional fall-through
        case CODE:
          if (currentChar == '/') {
            int nextChar = scanner.peek(1);
            if (nextChar == '*') {
              scannerState = MULTI_LINE_COMMENT_PREFIX;
              if (scanner.peek(2) == '*' && scanner.peek(3) != '/') {
                scannerState = DOC_COMMENT_PREFIX;
              }
              return tokens[CODE];
            } else if (nextChar == '/') {
              scannerState = SINGLE_LINE_COMMENT_PREFIX;
              return tokens[CODE];
            } else {
              advance();
            }
          } else if (currentChar == '@') {
            int secondChar = scanner.peek(1);
            if (secondChar == '\'' || secondChar == '"') {
              int thirdChar = scanner.peek(2);
              int fourthChar = scanner.peek(3);
              if (thirdChar == secondChar && fourthChar == secondChar) {
                stringState = new StringState(stringState, true, secondChar, 3);
                scannerState = RAW_MULTI_LINE_STRING_PREFIX;
              } else {
                stringState = new StringState(stringState, true, secondChar, 1);
                scannerState = RAW_STRING_PREFIX;
              }
              return tokens[CODE];
            } else {
              advance();
            }
          } else if (currentChar == '\'' || currentChar == '"') {
            int secondChar = scanner.peek(1);
            int thirdChar = scanner.peek(2);
            if (secondChar == currentChar && thirdChar == currentChar) {
              stringState = new StringState(stringState, false, currentChar, 3);
              scannerState = MULTI_LINE_STRING_PREFIX;
            } else {
              stringState = new StringState(stringState, false, currentChar, 1);
              scannerState = STRING_PREFIX;
            }
            return tokens[CODE];
          } else {
            advance();
          }
          break;
      }
      currentChar = scanner.peek(0);
    }
    if (tokenLength > 0) {
      if (scannerState >= tokens.length) {
        scannerState = CODE;
      }
      return tokens[scannerState];
    }
    return Token.EOF;
  }

  @Override
  public void setPartialRange(IDocument document, int offset, int length, String contentType,
      int partitionOffset) {
    // scan multi line string from the beginning, so that the activeStringDelimiter 
    // gets set
    if (contentType != null && contentType.equals(DART_MULTI_LINE_STRING)) {
      length += offset - partitionOffset;
      offset = partitionOffset;
    }
    scanner.setRange(document, offset, length);
    tokenOffset = partitionOffset;
    tokenLength = 0;
    prefixLength = offset - partitionOffset;

    if (offset == partitionOffset) {
      // restart at beginning of partition
      scannerState = CODE;
    } else {
      scannerState = getState(contentType);
    }
  }

  @Override
  public void setRange(IDocument document, int offset, int length) {
    scanner.setRange(document, offset, length);
    tokenOffset = offset;
    tokenLength = 0;
    prefixLength = 0;
    scannerState = CODE;
  }

  private void advance() {
    tokenLength++;
    scanner.read();
  }

  private boolean isEol(int character) {
    return character == '\r' || character == '\n' || character == '\u2028' || character == '\u2029';
  }

  private boolean isIdentifierChar(int character) {
    return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
        || (character >= '0' && character <= '9') || character == '_';
  }
}
