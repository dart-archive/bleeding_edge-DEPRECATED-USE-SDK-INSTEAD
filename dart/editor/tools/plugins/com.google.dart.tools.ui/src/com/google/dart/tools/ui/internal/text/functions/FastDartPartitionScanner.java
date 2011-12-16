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
  private enum ScannerState {
    //
    // Final states corresponding to partitions.
    //
    CODE(CODE_TOKEN), //
    SINGLE_LINE_COMMENT(SINGLE_LINE_COMMENT_TOKEN), //
    MULTI_LINE_COMMENT(MULTI_LINE_COMMENT_TOKEN), //
    DOC_COMMENT(DOC_COMMENT_TOKEN), //
    STRING(STRING_TOKEN), //
    MULTI_LINE_STRING(MULTI_LINE_STRING_TOKEN),
    //
    // Non-final states. The token type associated with these states is the type that will be
    // returned if we are in the state at the end of the file.
    //
    SINGLE_LINE_COMMENT_PREFIX(SINGLE_LINE_COMMENT_TOKEN), //
    MULTI_LINE_COMMENT_PREFIX(MULTI_LINE_COMMENT_TOKEN), //
    DOC_COMMENT_PREFIX(DOC_COMMENT_TOKEN), //
    RAW_STRING_PREFIX(STRING_TOKEN), //
    STRING_PREFIX(STRING_TOKEN), //
    RAW_MULTI_LINE_STRING_PREFIX(MULTI_LINE_STRING_TOKEN), //
    MULTI_LINE_STRING_PREFIX(MULTI_LINE_STRING_TOKEN), //
    SIMPLE_INTERPOLATION_PREFIX(CODE_TOKEN), //
    SIMPLE_INTERPOLATION(CODE_TOKEN), //
    BLOCK_INTERPOLATION_PREFIX(CODE_TOKEN), //
    BLOCK_INTERPOLATION(CODE_TOKEN);

    /**
     * The token that will be returned to represent the state as a partition.
     */
    private IToken token;

    /**
     * Initialize a newly created state to have the given token.
     * 
     * @param token the token that will be returned to represent the state as a partition
     */
    private ScannerState(IToken token) {
      this.token = token;
    }
  }

  /**
   * Instances of the class <code>StringState</code> represent the information about a string that
   * is needed when we return to scanning that string, such as after completing an interpolation
   * within a string.
   */
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

  private static IToken CODE_TOKEN = new Token(null);
  private static IToken SINGLE_LINE_COMMENT_TOKEN = new Token(DART_SINGLE_LINE_COMMENT);
  private static IToken MULTI_LINE_COMMENT_TOKEN = new Token(DART_MULTI_LINE_COMMENT);
  private static IToken DOC_COMMENT_TOKEN = new Token(DART_DOC);
  private static IToken STRING_TOKEN = new Token(DART_STRING);
  private static IToken MULTI_LINE_STRING_TOKEN = new Token(DART_MULTI_LINE_STRING);

  /**
   * Return the scanner state corresponding to the given partition type.
   * 
   * @param contentType the partition type being converted to a scanner state
   * @return the scanner state corresponding to the given partition type
   */
  private static ScannerState getState(String contentType) {
    if (contentType == null) {
      return ScannerState.CODE;
    } else if (contentType.equals(DART_SINGLE_LINE_COMMENT)) {
      return ScannerState.SINGLE_LINE_COMMENT;
    } else if (contentType.equals(DART_MULTI_LINE_COMMENT)) {
      return ScannerState.MULTI_LINE_COMMENT;
    } else if (contentType.equals(DART_DOC)) {
      return ScannerState.DOC_COMMENT;
    } else if (contentType.equals(DART_STRING)) {
      return ScannerState.STRING;
    } else if (contentType.equals(DART_MULTI_LINE_STRING)) {
      return ScannerState.MULTI_LINE_STRING;
    } else {
      return ScannerState.CODE;
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
  private ScannerState scannerState;

  /**
   * The state of the string that we are currently parsing, or <code>null</code> if we are not
   * inside a string.
   */
  private StringState stringState = null;

  /**
   * The current nesting depth for block comments.
   */
  private int commentDepth = 0;

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
    // Uncomment the code below for debugging output.
//    IToken result = nextToken_internal();
//    System.out.println(tokenOffset + " - " + (tokenOffset + tokenLength - 1) + " (" + tokenLength
//        + ") : " + result.getData());
//    return result;
//  }
//
//  public IToken nextToken_internal() {
    tokenOffset += tokenLength;
    tokenLength = prefixLength;
    prefixLength = 0;
    int currentChar = scanner.peek(0);
    while (currentChar != ICharacterScanner.EOF) {
      switch (scannerState) {
        case SINGLE_LINE_COMMENT_PREFIX:
          advance();
          advance();
          scannerState = ScannerState.SINGLE_LINE_COMMENT;
          break;
        case SINGLE_LINE_COMMENT:
          if (isEol(currentChar)) {
            scannerState = getCodeLikeState();
            return ScannerState.SINGLE_LINE_COMMENT.token;
          }
          advance();
          break;
        case MULTI_LINE_COMMENT_PREFIX:
          advance();
          advance();
          scannerState = ScannerState.MULTI_LINE_COMMENT;
          commentDepth++;
          break;
        case MULTI_LINE_COMMENT:
          if (currentChar == '*') {
            advance();
            if (scanner.peek(0) == '/') {
              advance();
              commentDepth--;
              if (commentDepth == 0) {
                scannerState = getCodeLikeState();
                return ScannerState.MULTI_LINE_COMMENT.token;
              }
            }
          } else if (currentChar == '/') {
            advance();
            if (scanner.peek(0) == '*') {
              advance();
              commentDepth++;
            }
          } else {
            advance();
          }
          break;
        case DOC_COMMENT_PREFIX:
          advance();
          advance();
          advance();
          scannerState = ScannerState.DOC_COMMENT;
          commentDepth++;
          break;
        case DOC_COMMENT:
          if (currentChar == '*') {
            advance();
            if (scanner.peek(0) == '/') {
              advance();
              commentDepth--;
              if (commentDepth == 0) {
                scannerState = getCodeLikeState();
                return ScannerState.DOC_COMMENT.token;
              }
            }
          } else if (currentChar == '/') {
            advance();
            if (scanner.peek(0) == '*') {
              advance();
              commentDepth++;
            }
          } else {
            advance();
          }
          break;
        case RAW_STRING_PREFIX:
          advance();
        case STRING_PREFIX:
          advance();
          scannerState = ScannerState.STRING;
          break;
        case STRING:
          if (isEol(currentChar)) {
            stringState = stringState.previous;
            scannerState = getCodeLikeState();
            return ScannerState.STRING.token;
          } else if (currentChar == stringState.quote) {
            advance();
            stringState = stringState.previous;
            scannerState = getCodeLikeState();
            return ScannerState.STRING.token;
          } else if (currentChar == '\\') {
            advance();
            if (scanner.peek(0) == stringState.quote) {
              advance();
            }
          } else if (!stringState.raw && currentChar == '$') {
            if (scanner.peek(1) == '{') {
              scannerState = ScannerState.BLOCK_INTERPOLATION_PREFIX;
            } else {
              scannerState = ScannerState.SIMPLE_INTERPOLATION_PREFIX;
            }
            return ScannerState.STRING.token;
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
          scannerState = ScannerState.MULTI_LINE_STRING;
          break;
        case MULTI_LINE_STRING:
          if (currentChar == stringState.quote) {
            advance();
            if (scanner.peek(0) == stringState.quote) {
              advance();
              if (scanner.peek(0) == stringState.quote) {
                advance();
                stringState = stringState.previous;
                scannerState = getCodeLikeState();
                return ScannerState.MULTI_LINE_STRING.token;
              }
            }
          } else if (currentChar == '\\') {
            advance();
            if (scanner.peek(0) == stringState.quote) {
              advance();
            }
          } else if (!stringState.raw && currentChar == '$') {
            if (scanner.peek(1) == '{') {
              scannerState = ScannerState.BLOCK_INTERPOLATION_PREFIX;
            } else {
              scannerState = ScannerState.SIMPLE_INTERPOLATION_PREFIX;
            }
            return ScannerState.MULTI_LINE_STRING.token;
          } else {
            advance();
          }
          break;
        case SIMPLE_INTERPOLATION_PREFIX:
          advance();
          scannerState = ScannerState.SIMPLE_INTERPOLATION;
          break;
        case SIMPLE_INTERPOLATION:
          if (!isIdentifierChar(currentChar)) {
            if (stringState.quoteCount == 1) {
              scannerState = ScannerState.STRING;
            } else {
              scannerState = ScannerState.MULTI_LINE_STRING;
            }
            return ScannerState.CODE.token;
          }
          advance();
          break;
        case BLOCK_INTERPOLATION_PREFIX:
          advance();
          advance();
          scannerState = ScannerState.BLOCK_INTERPOLATION;
          break;
        case BLOCK_INTERPOLATION:
          if (currentChar == '}') {
            if (stringState.braceCount == 0) {
              advance();
              if (stringState.quoteCount == 1) {
                scannerState = ScannerState.STRING;
              } else {
                scannerState = ScannerState.MULTI_LINE_STRING;
              }
              return ScannerState.CODE.token;
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
              scannerState = ScannerState.MULTI_LINE_COMMENT_PREFIX;
              if (scanner.peek(2) == '*' && scanner.peek(3) != '/') {
                scannerState = ScannerState.DOC_COMMENT_PREFIX;
              }
              return ScannerState.CODE.token;
            } else if (nextChar == '/') {
              scannerState = ScannerState.SINGLE_LINE_COMMENT_PREFIX;
              return ScannerState.CODE.token;
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
                scannerState = ScannerState.RAW_MULTI_LINE_STRING_PREFIX;
              } else {
                stringState = new StringState(stringState, true, secondChar, 1);
                scannerState = ScannerState.RAW_STRING_PREFIX;
              }
              return ScannerState.CODE.token;
            } else {
              advance();
            }
          } else if (currentChar == '\'' || currentChar == '"') {
            int secondChar = scanner.peek(1);
            int thirdChar = scanner.peek(2);
            if (secondChar == currentChar && thirdChar == currentChar) {
              stringState = new StringState(stringState, false, currentChar, 3);
              scannerState = ScannerState.MULTI_LINE_STRING_PREFIX;
            } else {
              stringState = new StringState(stringState, false, currentChar, 1);
              scannerState = ScannerState.STRING_PREFIX;
            }
            return ScannerState.CODE.token;
          } else {
            advance();
          }
          break;
      }
      currentChar = scanner.peek(0);
    }
    if (tokenLength > 0) {
      return scannerState.token;
    }
    return Token.EOF;
  }

  @Override
  public void setPartialRange(IDocument document, int offset, int length, String contentType,
      int partitionOffset) {
    // Scan a multi-line string from the beginning, so that the active string delimiter gets set.
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
      scannerState = ScannerState.CODE;
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
    scannerState = ScannerState.CODE;
  }

  private void advance() {
    tokenLength++;
    scanner.read();
  }

  /**
   * Return the code-like scanner state to which the scanner should return at the end of the current
   * state. This can either be {@link ScannerState#CODE} or {@link ScannerState#BLOCK_INTERPOLATION}
   * , depending on whether the scanner is currently within a multi-line string.
   * 
   * @return the code-like scanner state to which the scanner should return
   */
  private ScannerState getCodeLikeState() {
    if (stringState == null) {
      return ScannerState.CODE;
    } else {
      return ScannerState.BLOCK_INTERPOLATION;
    }
  }

  private boolean isEol(int character) {
    return character == '\r' || character == '\n' || character == '\u2028' || character == '\u2029';
  }

  private boolean isIdentifierChar(int character) {
    return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
        || (character >= '0' && character <= '9') || character == '_';
  }
}
