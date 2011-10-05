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
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.compiler.parser.DartScanner;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Wraps a scanner and offers convenient methods for finding tokens
 */
public class TokenScanner {
  public static final int END_OF_FILE = 20001;
  public static final int LEXICAL_ERROR = 20002;
  public static final int DOCUMENT_ERROR = 20003;

  public static IStatus createError(int code, String message, Throwable throwable) {
    return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, code, message, throwable);
  }

  public static boolean isComment(Token token) {
    return token == Token.COMMENT;
  }

  public static boolean isModifier(Token token) {
    if (token == Token.CONST) {
      return true;
    }
    DartCore.notYetImplemented();
    return false;
  }

  private final DartScanner scanner;

  // private final int endPosition;

  /**
   * Creates a TokenScanner
   * 
   * @param scanner The scanner to be wrapped
   */
  public TokenScanner(DartScanner scanner) {
    this.scanner = scanner;
    // endPosition = scanner.getSource().length - 1;
  }

  /**
   * @return Returns the offset after the current token
   */
  public int getCurrentEndOffset() {
    return scanner.getTokenLocation().getEnd().getPos() + 1;
  }

  /**
   * @return Returns the length of the current token
   */
  public int getCurrentLength() {
    return getCurrentEndOffset() - getCurrentStartOffset();
  }

  /**
   * @return Returns the start offset of the current token
   */
  public int getCurrentStartOffset() {
    return scanner.getTokenLocation().getBegin().getPos();
  }

  /**
   * Reads the next token from the given offset and returns the offset after the token.
   * 
   * @param offset The offset to start reading from.
   * @param ignoreComments If set, comments will be overread
   * @return Returns the start position of the next token.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public int getNextEndOffset(int offset, boolean ignoreComments) throws CoreException {
    readNext(offset, ignoreComments);
    return getCurrentEndOffset();
  }

  /**
   * Reads the next token from the given offset and returns the start offset of the token.
   * 
   * @param offset The offset to start reading from.
   * @param ignoreComments If set, comments will be overread
   * @return Returns the start position of the next token.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public int getNextStartOffset(int offset, boolean ignoreComments) throws CoreException {
    readNext(offset, ignoreComments);
    return getCurrentStartOffset();
  }

  /**
   * Reads from the given offset until a token is reached and returns the offset after the previous
   * token.
   * 
   * @param token The token to be found.
   * @param startOffset The offset to start scanning from.
   * @return Returns the end offset of the token previous to the given token.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public int getPreviousTokenEndOffset(Token token, int startOffset) throws CoreException {
    setOffset(startOffset);
    int res = startOffset;
    Token curr = readNext(false);
    while (curr != token) {
      res = getCurrentEndOffset();
      curr = readNext(false);
    }
    return res;
  }

  /**
   * Returns the wrapped scanner
   * 
   * @return IScanner
   */
  public DartScanner getScanner() {
    return scanner;
  }

  /**
   * Reads from the given offset until a token is reached and returns the offset after the token.
   * 
   * @param token The token to be found.
   * @param startOffset Offset to start reading from
   * @return Returns the end position of the found token.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public int getTokenEndOffset(Token token, int startOffset) throws CoreException {
    readToToken(token, startOffset);
    return getCurrentEndOffset();
  }

  /**
   * Reads from the given offset until a token is reached and returns the start offset of the token.
   * 
   * @param token The token to be found.
   * @param startOffset The offset to start reading from.
   * @return Returns the start position of the found token.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public int getTokenStartOffset(Token token, int startOffset) throws CoreException {
    readToToken(token, startOffset);
    return getCurrentStartOffset();
  }

  /**
   * Reads the next token.
   * 
   * @param ignoreComments If set, comments will be overread
   * @return Return the token id.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public Token readNext(boolean ignoreComments) throws CoreException {
    Token curr = null;
    do {
      // try {
      curr = scanner.next();
      if (curr == Token.EOS) {
        throw new CoreException(createError(END_OF_FILE, "End Of File", null)); //$NON-NLS-1$
      }
      // } catch (InvalidInputException e) {
      // throw new CoreException(createError(LEXICAL_ERROR, e.getMessage(), e));
      // }
    } while (ignoreComments && isComment(curr));
    return curr;
  }

  /**
   * Reads the next token from the given offset.
   * 
   * @param offset The offset to start reading from.
   * @param ignoreComments If set, comments will be overread.
   * @return Returns the token id.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public Token readNext(int offset, boolean ignoreComments) throws CoreException {
    setOffset(offset);
    return readNext(ignoreComments);
  }

  /**
   * Reads until a token is reached.
   * 
   * @param tok The token to read to.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public void readToToken(Token tok) throws CoreException {
    Token curr = null;
    do {
      curr = readNext(false);
    } while (curr != tok);
  }

  /**
   * Reads until a token is reached, starting from the given offset.
   * 
   * @param tok The token to read to.
   * @param offset The offset to start reading from.
   * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE) or
   *              a lexical error was detected while scanning (code LEXICAL_ERROR)
   */
  public void readToToken(Token tok, int offset) throws CoreException {
    setOffset(offset);
    readToToken(tok);
  }

  /**
   * Sets the scanner offset to the given offset.
   * 
   * @param offset The offset to set
   */
  public void setOffset(int offset) {
    DartCore.notYetImplemented();
    // scanner.resetTo(offset, endPosition);
  }
}
