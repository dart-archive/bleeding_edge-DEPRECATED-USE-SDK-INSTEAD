/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
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
 * <p>
 * TODO Do string partitions include their delimiters?
 */
public class FastDartPartitionScanner implements IPartitionTokenScanner, DartPartitions {

  // states
  private static final int CODE = 0;
  private static final int SINGLE_LINE_COMMENT = 1;
  private static final int MULTI_LINE_COMMENT = 2;
  private static final int DOC_COMMENT = 3;
  private static final int STRING = 4;
  private static final int MULTI_LINE_STRING = 5;

  // beginning of prefixes and postfixes
  private static final int NONE = 0;
  // postfix for STRING and CHARACTER
  private static final int BACKSLASH = 1;
  // prefix for SINGLE_LINE or MULTI_LINE or DOC_COMMENT
  private static final int SLASH = 2;
  // prefix for MULTI_LINE_COMMENT or DOC_COMMENT
  private static final int SLASH_STAR = 3;
  // prefix for MULTI_LINE_COMMENT or DOC_COMMENT
  private static final int SLASH_STAR_STAR = 4;
  // postfix for MULTI_LINE_COMMENT or DOC_COMMENT
  private static final int STAR = 5;
  // postfix for STRING, CHARACTER and SINGLE_LINE_COMMENT
  private static final int CARRIAGE_RETURN = 6;
  // anti-postfix for STRING, CHARACTER
  private static final int BACKSLASH_CARRIAGE_RETURN = 7;
  private static final int DOUBLE_QUOTE = 8;
  private static final int DOUBLE_QUOTE_QUOTE = 9;
  private static final int DOUBLE_QUOTE_QUOTE_QUOTE = 10;
  private static final int SINGLE_QUOTE = 11;
  private static final int SINGLE_QUOTE_QUOTE = 12;
  private static final int SINGLE_QUOTE_QUOTE_QUOTE = 13;

  private static final int getLastLength(int last) {
    switch (last) {
      default:
        return -1;

      case NONE:
        return 0;

      case CARRIAGE_RETURN:
      case BACKSLASH:
      case SLASH:
      case STAR:
      case DOUBLE_QUOTE:
      case SINGLE_QUOTE:
        return 1;

      case SLASH_STAR:
      case DOUBLE_QUOTE_QUOTE:
      case SINGLE_QUOTE_QUOTE:
        return 2;

      case SLASH_STAR_STAR:
      case DOUBLE_QUOTE_QUOTE_QUOTE:
      case SINGLE_QUOTE_QUOTE_QUOTE:
        return 3;
    }
  }

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

  /** The scanner. */
  private final BufferedDocumentScanner scanner = new BufferedDocumentScanner(1000); // faster implementation
  /** The offset of the last returned token. */
  private int tokenOffset;
  /** The length of the last returned token. */
  private int tokenLength;
  /** The state of the scanner. */
  private int scannerState;
  /** The last significant characters read. */
  private int lastChar;
  /** The amount of characters already read on first call to nextToken(). */
  private int prefixLength;
  /** The active string delimiter while scanning a STRING or MULTI_LINE_STRING */
  private int activeStringDelimiter;

  private final IToken[] tokens = new IToken[] {
      new Token(null), new Token(DART_SINGLE_LINE_COMMENT), new Token(DART_MULTI_LINE_COMMENT),
      new Token(DART_DOC), new Token(DART_STRING), new Token(DART_MULTI_LINE_STRING)};

  public FastDartPartitionScanner() {
    // create the scanner
  }

  /*
   * @see ITokenScanner#getTokenLength()
   */
  @Override
  public int getTokenLength() {
    return tokenLength;
  }

  /*
   * @see ITokenScanner#getTokenOffset()
   */
  @Override
  public int getTokenOffset() {
    return tokenOffset;
  }

  /*
   * @see org.eclipse.jface.text.rules.ITokenScanner#nextToken()
   */
  @Override
  public IToken nextToken() {
    tokenOffset += tokenLength;
    tokenLength = prefixLength;

    // int lastNonWhitespaceChar = NONE;
    int currentChar = NONE;

    while (true) {
      // if (!Character.isWhitespace((char) currentChar))
      // lastNonWhitespaceChar = currentChar;

      // read in the next char
      currentChar = scanner.read();

      // characters
      switch (currentChar) {
        case ICharacterScanner.EOF:
          if (tokenLength > 0) {
            lastChar = NONE; // ignore last
            return preFix(scannerState, CODE, NONE, 0);

          } else {
            lastChar = NONE;
            prefixLength = 0;
            return Token.EOF;
          }

        case '\r':
          if (scannerState == STRING && lastChar == BACKSLASH) {
            lastChar = BACKSLASH_CARRIAGE_RETURN;
            tokenLength++;
            continue;
          }
          if (lastChar != CARRIAGE_RETURN) {
            lastChar = CARRIAGE_RETURN;
            tokenLength++;
            continue;

          } else {
            switch (scannerState) {
              case SINGLE_LINE_COMMENT:
              case STRING:
                if (tokenLength > 0) {
                  IToken token = tokens[scannerState];

                  lastChar = CARRIAGE_RETURN;
                  prefixLength = 1;

                  scannerState = CODE;
                  return token;

                } else {
                  consume();
                  continue;
                }

              default:
                consume();
                continue;
            }
          }

        case '\n':
        case '\u2028':
        case '\u2029':
          switch (scannerState) {
            case STRING:
              if (lastChar == BACKSLASH || lastChar == BACKSLASH_CARRIAGE_RETURN) {
                consume();
                continue;
              }
            case SINGLE_LINE_COMMENT:
              return postFix(scannerState);

            default:
              consume();
              continue;
          }

        default:
          if (lastChar == CARRIAGE_RETURN) {
            switch (scannerState) {
              case SINGLE_LINE_COMMENT:
              case STRING:

                int last;
                int newState;
                switch (currentChar) {
                  case '/':
                    last = SLASH;
                    newState = CODE;
                    break;

                  case '*':
                    last = STAR;
                    newState = CODE;
                    break;

                  case '\'':
                  case '"':
                    last = NONE;
                    newState = STRING;
                    break;

                  case '\r':
                    last = CARRIAGE_RETURN;
                    newState = CODE;
                    break;

                  case '\\':
                    last = BACKSLASH;
                    newState = CODE;
                    break;

                  default:
                    last = NONE;
                    newState = CODE;
                    break;
                }

                lastChar = NONE; // ignore lastChar
                return preFix(scannerState, newState, last, 1);

              default:
                break;
            }
          }
      }

      // states
      switch (scannerState) {
        case CODE:
          switch (currentChar) {
            case '/':
              if (lastChar == SLASH) {
                if (tokenLength - getLastLength(lastChar) > 0) {
                  return preFix(CODE, SINGLE_LINE_COMMENT, NONE, 2);
                } else {
                  preFix(CODE, SINGLE_LINE_COMMENT, NONE, 2);
                  tokenOffset += tokenLength;
                  tokenLength = prefixLength;
                  break;
                }

              } else {
                tokenLength++;
                lastChar = SLASH;
                break;
              }

            case '*':
              if (lastChar == SLASH) {
                if (tokenLength - getLastLength(lastChar) > 0) {
                  return preFix(CODE, MULTI_LINE_COMMENT, SLASH_STAR, 2);
                } else {
                  preFix(CODE, MULTI_LINE_COMMENT, SLASH_STAR, 2);
                  tokenOffset += tokenLength;
                  tokenLength = prefixLength;
                  break;
                }

              } else {
                consume();
                break;
              }

            case '\'':
            case '\"':
              activeStringDelimiter = currentChar;
              lastChar = NONE; // ignore lastChar
              if (tokenLength > 0) {
                // set lastChar = currentChar
                return preFix(CODE, STRING, currentChar, 1);
              } else {
                // set lastChar = currentChar
                preFix(CODE, STRING, currentChar, 1);
                tokenOffset += tokenLength;
                tokenLength = prefixLength;
                break;
              }

            default:
              consume();
              break;
          }
          break;

        case SINGLE_LINE_COMMENT:
          consume();
          break;

        case DOC_COMMENT:
          switch (currentChar) {
            case '/':
              switch (lastChar) {
                case SLASH_STAR_STAR:
                  return postFix(MULTI_LINE_COMMENT);

                case STAR:
                  return postFix(DOC_COMMENT);

                default:
                  consume();
                  break;
              }
              break;

            case '*':
              tokenLength++;
              lastChar = STAR;
              break;

            default:
              consume();
              break;
          }
          break;

        case MULTI_LINE_COMMENT:
          switch (currentChar) {
            case '*':
              if (lastChar == SLASH_STAR) {
                lastChar = SLASH_STAR_STAR;
                tokenLength++;
                scannerState = DOC_COMMENT;
              } else {
                tokenLength++;
                lastChar = STAR;
              }
              break;

            case '/':
              if (lastChar == STAR) {
                return postFix(MULTI_LINE_COMMENT);
              } else {
                consume();
                break;
              }

            default:
              consume();
              break;
          }
          break;

        case STRING:
          switch (currentChar) {
            case '\\':
              lastChar = (lastChar == BACKSLASH) ? NONE : BACKSLASH;
              tokenLength++;
              break;

            case '\'':
            case '\"':
              if (currentChar != activeStringDelimiter) {
                consume();
                break;
              }
              if (lastChar == currentChar) {
                int ch = scanner.read();
                if (ch == currentChar) {
                  tokenLength++;
                  lastChar = activeStringDelimiter == '\"' ? DOUBLE_QUOTE_QUOTE
                      : SINGLE_QUOTE_QUOTE;
                  return preFix(CODE, MULTI_LINE_STRING, NONE, 3);
                }
                scanner.unread();
              }
              if (lastChar != BACKSLASH) {
                return postFix(STRING);

              } else {
                consume();
                break;
              }

            default:
              consume();
              break;
          }
          break;

        case MULTI_LINE_STRING:
          switch (currentChar) {
            case '\"':
            case '\'':
              if (currentChar != activeStringDelimiter) {
                consume();
                break;
              }
              if (lastChar == DOUBLE_QUOTE_QUOTE) {
                lastChar = DOUBLE_QUOTE_QUOTE_QUOTE;
                return postFix(MULTI_LINE_STRING);
              } else if (lastChar == DOUBLE_QUOTE) {
                lastChar = DOUBLE_QUOTE_QUOTE;
              } else if (lastChar == SINGLE_QUOTE_QUOTE) {
                lastChar = SINGLE_QUOTE_QUOTE_QUOTE;
                return postFix(MULTI_LINE_STRING);
              } else if (lastChar == SINGLE_QUOTE) {
                lastChar = SINGLE_QUOTE_QUOTE;
              } else {
                lastChar = activeStringDelimiter == '\"' ? DOUBLE_QUOTE : SINGLE_QUOTE;
              }
              tokenLength++;
              break;
            default:
              consume();
              break;
          }
      }
    }
  }

  /*
   * @see IPartitionTokenScanner#setPartialRange(IDocument, int, int, String, int)
   */
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
    lastChar = NONE;

    if (offset == partitionOffset) {
      // restart at beginning of partition
      scannerState = CODE;
    } else {
      scannerState = getState(contentType);
    }
  }

  /*
   * @see ITokenScanner#setRange(IDocument, int, int)
   */
  @Override
  public void setRange(IDocument document, int offset, int length) {
    scanner.setRange(document, offset, length);
    tokenOffset = offset;
    tokenLength = 0;
    prefixLength = 0;
    lastChar = NONE;
    scannerState = CODE;
  }

  private final void consume() {
    tokenLength++;
    lastChar = NONE;
  }

  private final IToken postFix(int state) {
    tokenLength++;
    lastChar = NONE;
    scannerState = CODE;
    prefixLength = 0;
    return tokens[state];
  }

  private final IToken preFix(int state, int newState, int last, int prefLength) {
    tokenLength -= getLastLength(lastChar);
    lastChar = last;
    prefixLength = prefLength;
    IToken token = tokens[state];
    scannerState = newState;
    return token;
  }

}
