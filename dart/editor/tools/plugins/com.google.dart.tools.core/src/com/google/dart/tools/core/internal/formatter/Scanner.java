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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.parser.DartScanner;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.DartCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Mimic some of the JDT Scanner protocol. Add functionality the Dart scanner was not designed to
 * support: tokenization of whitespace and comments, and the ability to scan arbitrary ranges of
 * text (within limits).
 * <p>
 * Instances can, for example, be initialized to start scanning just after a comment header, then be
 * used to tokenize a comment. (Scribe does that.)
 * <p>
 * TODO measure performance.<br>
 * Since the scanner needs to do things the Dart scanner is not designed to do, it frequently has to
 * re-initialize the Dart scanner. This could be a serious performance issue, so be sure to look
 * into that.
 */
public class Scanner {
  class CommentScanner extends DartScanner {

    CommentScanner(String str, int s) {
      super(str, s);
    }

    CommentScanner(String str, int s, int e) {
      super(str.substring(0, e), s);
    }

    @Override
    protected void recordCommentLocation(int begin, int stop, int line, int col) {
      int start = begin; // - 1;
      int size = commentLocs.size();
      if (size > 0) {
        // check for duplicates
        int[] loc = commentLocs.get(size - 1);
        // use <= to allow parser to back up more than one token
        if (start <= loc[0] && stop <= loc[1]) {
          return;
        }
      }
      commentLocs.add(new int[] {start, stop});
    }
  }

  public static final char[] TAG_PREFIX = "//$NON-NLS-".toCharArray(); //$NON-NLS-1$
  public static final char[] FALL_THROUGH_TAG = "$FALL-THROUGH$".toCharArray(); //$NON-NLS-1$
  private static final String LINE_END = System.getProperty("line.separator");

  public int currentCharacter;
  public int currentPosition;
  public int startPosition;
  public char[] source;
  public boolean skipComments;
  public List<int[]> commentLocs = new ArrayList<int[]>();

  private CommentScanner scanner;
  private String sourceString;
  private int startPos;
  private int endPos;
  private int chars;
  private boolean nextTokenIsWhitespace;

  public boolean atEnd() {
    return currentPosition >= endPos;
  }

  /**
   * Return chars following last line seen in countLinesBetween()<br>
   * Only relevant following a call to countLinesBetween().
   */
  public int charsAfterLastLineEnd() {
    return chars;
  }

  /**
   * We don't have visibility of scanner state when scanning string segments in a string
   * interpolation, and even if the API was accessible it would not help because of lookahead -- we
   * would not see the flag when we need to. Therefore, the formatter visitor has to tell us when it
   * is processing string interpolation so we can return the correct token.
   */
  public void continueStringInterpolation() {
    nextTokenIsWhitespace = false;
  }

  /**
   * Return the number of lines between <code>fromPos</code> and <code>toPos</code>.
   * <p>
   * TODO Re-implement using scanner tokenization.
   */
  public int countLinesBetween(int fromPos, int toPos) {
    // count lines between char positions fromPos, toPos
    // count characters after final line
    DartCore.notYetImplemented();
    String lineEnd = LINE_END;
    char lineEndCh1 = lineEnd.charAt(0);
    boolean lineEndSingleChar = lineEnd.length() == 1;
    int first = Math.max(fromPos, startPos);
    int last = Math.min(toPos, endPos);
    int lineCount = 0;
    int charCount = 0;
    for (int i = first; i <= last; i++) {
      char ch = source[i];
      charCount += 1;
      switch (ch) {
        case '\n':
        case '\r':
          if (ch == lineEndCh1) {
            if (lineEndSingleChar) {
              lineCount += 1;
              charCount = 0;
            } else if (lineEnd.charAt(1) == source[i + 1]) {
              i += 1;
              lineCount += 1;
              charCount = 0;
            }
          }
          break;
        case '"':
        case '\'':
          i = skipOverString(i, last, ch);
          break;
        case '/':
          if (source[i + 1] == '/') {
            i = skipOverSingleLineComment(i, last);
          } else if (source[i + 1] == '*') {
            i = skipOverMultiLineComment(i, last);
          }
          break;
        case '\\':
          i += 1;
          break;
        default:
          break;
      }
    }
    chars = charCount;
    return lineCount;
  }

  /**
   * Return the style of the current comment.
   */
  public DartComment.Style getCommentStyle() {
    return getCommentStyle(startPosition);
  }

  /**
   * Return the style of the comment that begins at <code>start</code>.
   */
  public DartComment.Style getCommentStyle(int start) {
    // assume the scanner has already recognized a legal comment, so no bounds
    // checks
    if (source[start + 1] == '/') {
      return DartComment.Style.END_OF_LINE;
    } else if (source[start + 2] == '*') {
      if (source[start + 3] == '/') {
        return DartComment.Style.BLOCK;
      } else {
        return DartComment.Style.DART_DOC;
      }
    }
    return DartComment.Style.BLOCK;
  }

  public int getCurrentTokenEndPosition() {
    return currentPosition - 1;
  }

  public int getCurrentTokenStartPosition() {
    return startPosition;
  }

  public String getCurrentTokenString() {
    return sourceString.substring(startPosition, currentPosition);
  }

  public int getEndPos() {
    return endPos;
  }

  public String getLastTokenString() {
    return sourceString.substring(startPosition, source.length);
  }

  public Token getNextToken() throws InvalidInputException {
    if (nextTokenIsWhitespace) {
      nextTokenIsWhitespace = false;
      return getNextWhitespace();
    } else if (currentCharacter == '/' && isAtComment()) {
      if (skipComments) {
        Token token = getNextNonComment();
        nextTokenIsWhitespace = Character.isWhitespace(currentCharacter);
        return token;
      } else {
        Token token = getNextComment();
        nextTokenIsWhitespace = Character.isWhitespace(currentCharacter);
        return token;
      }
    } else {
      Token token = getNextFromScanner();
      nextTokenIsWhitespace = Character.isWhitespace(currentCharacter);
      return token;
    }
  }

  public DartScanner.State getState() {
    return getScanner().getState();
  }

  public Token peekNextToken() {
    return getScanner().peek(0);
  }

  public String peekWhitespace() {
    // only meaningful immediately after reseting scanner state
    // return the next whitespace, no state change
    for (int i = startPos; i <= endPos; i++) {
      char ch = source[i];
      if (ch == ' ' || ch == '\t') {
        continue;
      } else {
        return new String(source, startPos, i - startPos);
      }
    }
    return null;
  }

  /**
   * Reset the scanner to scan text in the range <code>newStart</code> to <code>newEnd</code>.
   * <p>
   * NOTE: This destroys all internal scanner state. Do not use this method, for example, while
   * scanning string interpolation unless it is followed by restoreState().
   * 
   * @param newStart the beginning index into <code>source</code>
   * @param newEnd the terminating index into <code>source</code>
   */
  public void resetTo(int newStart, int newEnd) {
    startPos = newStart;
    scanner = null;
    skipComments = false;
    startPosition = newStart;
    currentPosition = newStart;

    if (source != null && source.length < newEnd) {
      endPos = source.length;
    } else {
      endPos = newEnd < Integer.MAX_VALUE ? newEnd + 1 : newEnd;
    }

    if (newStart < source.length) {
      currentCharacter = source[newStart];
      nextTokenIsWhitespace = Character.isWhitespace(currentCharacter);
    } else {
      currentCharacter = -1;
      nextTokenIsWhitespace = false;
    }
    if (startPos == 0 && commentLocs.isEmpty()) {
      // prime comment detection
      // only prime at beginning, assuming scanner only reset backward
      getScanner().peek(0);
    }
//    System.out.print("reset to: startPos = " + startPos);
//    System.out.print(" endpos = " + endPos);
//    System.out.print(" skipComments = " + skipComments);
//    System.out.print(" startPosition = " + startPosition);
//    System.out.print(" currentPosition = " + currentPosition);
//    System.out.println(" currentCharacter = " + (char)currentCharacter);

  }

  /**
   * Restore scanner state. This does not cause the state of this wrapper instance to be update, so
   * it should generally be used following a call to resetTo().
   * <p>
   * TODO Unify resetTo() and restoreState() someday.
   * 
   * @param state a previously saved State object for the DartScanner
   */
  public void restoreState(DartScanner.State state) {
    getScanner().restoreState(state);
  }

  public void setSource(char[] sourceArray) {
    sourceString = new String(sourceArray);
    source = sourceArray;
  }

  public void setSource(String sourceString) {
    this.sourceString = sourceString;
    source = sourceString.toCharArray();
    resetTo(0, source.length);
  }

  private Token getNextComment() {
    int[] cl = null;
    for (int[] loc : commentLocs) {
      if (loc[0] == currentPosition) {
        cl = loc;
        break;
      }
    }
    if (cl == null) {
      throw new NullPointerException(); // not reached
    }
    startPosition = cl[0];
    currentPosition = cl[1];
    currentCharacter = source[currentPosition];
    return Token.COMMENT;
  }

  private Token getNextFromScanner() throws InvalidInputException {
    Token tok = getScanner().next();
    startPosition = getScanner().getTokenLocation().getBegin().getPos();
    currentPosition = getScanner().getTokenLocation().getEnd().getPos();
//    System.out.println("Scanner.getNextFromScanner {");
//    System.out.print(" " + tok + "(" + getScanner().getTokenValue() + ")");
//    System.out.println(" current loc " + getScanner().getTokenLocation().getBegin().toString());
//    System.out.println(" next token = " + getScanner().peek(0));
//    System.out.println(" startPos = " + startPosition);
//    System.out.println(" currPos = " + currentPosition);
//    System.out.println("}");
    if (tok == Token.EOS) {
      if (currentPosition < source.length) {
        throw new InvalidInputException();
      }
      return tok;
    }
    if (tok == Token.ILLEGAL) {
      throw new InvalidInputException();
    }
    if (currentPosition < source.length) {
      currentCharacter = source[currentPosition];
    } else {
      currentCharacter = -1;
    }
    return tok;
  }

  private Token getNextNonComment() {
    // The Dart scanner won't skip comments so we need to toss it and
    // create a new one that starts scanning after the first comment char.
    getNextChar();
    return Token.DIV;
  }

  private Token getNextWhitespace() {
    startPosition = currentPosition;
    while (++currentPosition < source.length) {
      currentCharacter = source[currentPosition];
      if (!Character.isWhitespace(currentCharacter)) {
        break;
      }
    }
    if (currentPosition == source.length) {
      currentPosition -= 1;
      currentCharacter = -1;
    }
    return Token.WHITESPACE;
  }

  private DartScanner getScanner() {
    if (scanner == null) {
      scanner = new CommentScanner(sourceString, startPos, endPos);
//      System.out.println("creating new comment scanner (" + startPos + "," +
//          + endPos + ") hash: 0x" +
//          Integer.toHexString(scanner.hashCode()));
    }
    return scanner;
  }

  private boolean isAtComment() {
    // TODO binary search
    for (int[] loc : commentLocs) {
      if (loc[0] == currentPosition) {
        return true;
      }
    }
    if (skipComments) {
      if (currentPosition + 1 >= source.length) {
        return false;
      }
      int ch = source[currentPosition + 1];
      if (currentCharacter == '/' && ch == '/' || ch == '*') {
        return true;
      }
    }
    return false;
  }

  private int skipOverMultiLineComment(int startOffset, int endOffset) {
    int i = startOffset;
    while (i <= endOffset) {
      if (source[i] == '*' && source[i + 1] == '/') {
        return i + 1;
      }
      i += 1;
    }
    return endOffset;
  }

  private int skipOverSingleLineComment(int startOffset, int endOffset) {
    int i = startOffset;
    while (i <= endOffset) {
      if (source[i] == '\n' || source[i] == '\r') {
        return i - 1;
      }
      i += 1;
    }
    return endOffset;
  }

  private int skipOverString(int startOffset, int endOffset, char ch) {
    int offset = startOffset;
    boolean multiline = false;
    if (offset + 2 <= endOffset) {
      if (source[offset] == ch && source[offset + 1] == ch) {
        offset += 2;
        multiline = true;
      }
    }
    while (offset < endOffset) {
      char curr = source[offset];
      offset++;
      if (curr == '\\') {
        // ignore escaped characters
        offset++;
      } else if (curr == ch) {
        if (!multiline) {
          return offset;
        } else if (offset + 2 <= endOffset) {
          if (source[offset] == ch && source[offset + 1] == ch) {
            return offset + 2;
          }
        }
      }
    }
    return endOffset;
  }

  int getNextChar() {
    // only called while formatting comments
    try {
      startPosition = currentPosition;
      currentCharacter = source[currentPosition++];
      nextTokenIsWhitespace = Character.isWhitespace(source[currentPosition]);
      // The Dart scanner won't skip chars so we need to toss it and
      // create a new one that starts scanning at the next char.
      scanner = null;
      startPos = currentPosition;
    } catch (IndexOutOfBoundsException ex) {
      currentCharacter = -1;
      nextTokenIsWhitespace = false;
    }
    return currentCharacter;
  }

  boolean getNextChar(int ch) {
    // no state change on failure
    if (startPosition >= source.length) {
      return false;
    }
    if (source[currentPosition] == ch) {
      getNextChar();
      return true;
    }
    return false;
  }

  void skipNewline() {
    if (nextTokenIsWhitespace && currentCharacter == '\n') {
      if (++currentPosition < source.length) {
        currentCharacter = source[currentPosition];
        nextTokenIsWhitespace = Character.isWhitespace(currentCharacter);
      } else {
        currentPosition -= 1;
        currentCharacter = -1;
        nextTokenIsWhitespace = false;
      }
      startPos = startPosition = currentPosition;
      scanner = null;
    }
  }
}
