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
package com.google.dart.engine.formatter.edit;

import com.google.dart.engine.formatter.CodeFormatterOptions;
import com.google.dart.engine.formatter.Scanner;
import com.google.dart.engine.scanner.Token;

import static com.google.dart.engine.formatter.CodeFormatterOptions.OS_LINE_SEPARATOR;

import java.util.List;

/**
 * Records a sequence of edits to a source string that will cause the string to be formatted when
 * applied via an {@link EditOperation}.
 * 
 * @param <D> the document type
 * @param <R> an (optional) return result type
 */
public abstract class EditRecorder<D, R> {

  //indentation
  protected int indentationLevel;
  protected final int indentPerLevel;
  protected int numberOfIndentations;
  private boolean isIndentNeeded;

  protected int tabLength;

  //spaces
  protected boolean pendingSpace;

  protected int lastNumberOfNewLines;

  protected int column; //NOTE: one-based
  protected int line;
  protected int[] lineEnds;

  private int sourceIndex;

  protected int scannerEndPosition;

  @SuppressWarnings("unused")
  private final Scanner scanner;
  private final EditStore editStore;

  private String source;

  private Token currentToken;

  private static final char SPACE = ' ';
  private static final String[] SPACES = {" ",//
      "  ",//
      "   ",//
      "    ",//
      "     ",//
      "      ",//
      "       ",//
      "        ",//
      "         ",//
      "          ",//
      "           ",//
      "            ",//
      "             ",//
      "              ",//
      "               ",//
      "                ",//
  };

  private static final String NEW_LINE = OS_LINE_SEPARATOR;

  /**
   * Create an instance with the given set of options.
   * 
   * @param options formatting options
   * @param scanner the scanner
   * @param editStore the edit store
   */
  protected EditRecorder(CodeFormatterOptions options, Scanner scanner, EditStore editStore) {
    this.scanner = scanner;
    this.editStore = editStore;
    this.indentPerLevel = options.indent_per_level;
    this.tabLength = options.tab_size;
    reset();
  }

  /**
   * Create an instance using the default formatting options.
   * 
   * @param scanner the scanner
   * @param editStore the edit store
   */
  protected EditRecorder(Scanner scanner, EditStore editStore) {
    this(CodeFormatterOptions.getDefaults(), scanner, editStore);
  }

  /**
   * Advance past the given expected token (or fail if not matched).
   * 
   * @param token the token to match
   */
  public void advance(String token) {
    if (currentToken.getLexeme().equals(token)) {
      advance(currentToken);
    } else {
      wrongToken(token);
    }
  }

  /**
   * Advance past the given expected token (or fail if not matched).
   * 
   * @param token the token to match
   */
  public void advance(Token token) {
    if (currentToken.getType() == token.getType()) {
      // TODO(pquitslund) emit comments
      if (isIndentNeeded) {
        advanceIndent();
        isIndentNeeded = false;
      }
      // record writing a token at the current edit location
      int len = token.getLength();
      advance(len);
      currentToken = currentToken.getNext();
    } else {
      wrongToken(token.getLexeme());
    }
  }

  /**
   * Get the underlying sequence of edits.
   * 
   * @return the sequence of edits
   */
  public List<Edit> getEdits() {
    return editStore.getEdits();
  }

  /**
   * Get the current source.
   * 
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * Get the current source index.
   * 
   * @return the sourceIndex
   */
  public int getSourceIndex() {
    return sourceIndex;
  }

  /**
   * Indent.
   */
  public void indent() {
    indentationLevel += indentPerLevel;
    numberOfIndentations++;
  }

  /**
   * Add a newline (if needed).
   */
  public void newline() {
    // TODO(pquitslund) emit comments
    // If there is a newline before the edit location, do nothing.
    isIndentNeeded = true;
    if (isNewlineAt(sourceIndex - NEW_LINE.length())) {
      return;
    }
    // If there is a newline after the edit location, advance over it.
    if (isNewlineAt(sourceIndex)) {
      advance(NEW_LINE.length());
      return;
    }
    // Otherwise, replace whitespace with a newline.
    int charsToReplace = countWhitespace();
    if (isNewlineAt(sourceIndex + charsToReplace)) {
      ++charsToReplace;
    }
    addEdit(sourceIndex, charsToReplace, NEW_LINE);
    column += NEW_LINE.length();
    sourceIndex += charsToReplace;
  }

  /**
   * Reset.
   */
  public void reset() {
    line = 0;
    column = 1;
    editStore.reset();
  }

  /**
   * Set the input source.
   * 
   * @param source the source being formatted
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Set the start token.
   * 
   * @param start the start token
   */
  public void setStart(Token start) {
    this.currentToken = start;
  }

  /**
   * Space.
   */
  public void space() {
    // TODO(pquitslund) emit comments
//    // If there is a space before the edit location, do nothing.
//    if (isSpaceAt(sourceIndex - 1)) {
//      return;
//    }
//    // If there is a space after the edit location, advance over it.
//    if (isSpaceAt(sourceIndex)) {
//      advance(1);
//      return;
//    }
    // Otherwise, replace spaces with a single space.
    int charsToReplace = countWhitespace();
    addEdit(sourceIndex, charsToReplace, SPACES[0]);
    advance(charsToReplace);
  }

  /**
   * Un-indent.
   */
  public void unIndent() {
    indentationLevel -= indentPerLevel;
    numberOfIndentations--;
  }

  /**
   * Add an {@link Edit} that describes a textual replacement of a text interval starting at the
   * given offset spanning the given length.
   * 
   * @param offset the offset at which to begin the edit
   * @param length the length of the text interval to replace
   * @param replacement the replacement text
   * @see EditStore#addEdit(int, int, String)
   */
  protected void addEdit(int offset, int length, String replacement) {
    editStore.addEdit(offset, length, replacement);
  }

  /**
   * Advance column and sourceIndex indices by the given number of characters.
   * 
   * @param len
   */
  protected void advance(int len) {
    column += len;
    sourceIndex += len;
  }

  protected void advanceIndent() {
    // Move indices past indent, adding edit record if needed to adjust indentation
    int indentWidth = indentPerLevel * indentationLevel;
    String indentString;
    // TODO(messick) allow indent with tab chars
    if (indentWidth < SPACES.length) {
      indentString = SPACES[indentWidth];
    } else {
      indentString = new String(new char[indentWidth]);
    }
    int sourceIndentWidth = 0;
    for (int i = 0; i < source.length(); i++) {
      if (isIndentChar(source.charAt(sourceIndex + i))) {
        sourceIndentWidth += 1;
      } else {
        break;
      }
    }
    boolean hasSameIndent = sourceIndentWidth == indentWidth;
    if (hasSameIndent) {
      for (int i = 0; i < indentWidth; i++) {
        if (source.charAt(sourceIndex + i) != indentString.charAt(i)) {
          hasSameIndent = false;
          break;
        }
      }
      if (hasSameIndent) {
        advance(indentWidth);
        return;
      }
    }
    addEdit(sourceIndex, sourceIndentWidth, indentString);
    column += indentWidth;
    sourceIndex += sourceIndentWidth;
  }

  /**
   * Count the number of whitespace chars beginning at the current {@link #sourceIndex}.
   * 
   * @return the number of whitespaces
   */
  protected int countWhitespace() {
    int count = 0;
    for (int i = sourceIndex; i < source.length(); ++i) {
      if (isIndentChar(source.charAt(i))) {
        count += 1;
      } else {
        break;
      }
    }
    return count;
  }

  /**
   * Get the last edit.
   * 
   * @return the last edit (or {@code null} if there is none).
   * @see EditStore#getLastEdit()
   */
  protected Edit getLastEdit() {
    return editStore.getLastEdit();
  }

  /**
   * Add an {@link Edit} that describes an insertion of text starting at the given offset.
   * 
   * @param offset the offset at which to begin the edit
   * @param insertedString the text to insert
   */
  protected void insert(int insertPosition, String insertedString) {
    editStore.insert(insertPosition, insertedString);
  }

  /**
   * Test if there is a newline at the given source index.
   * 
   * @param index the index to test
   * @return <code>true</code> if there is a newline at the given source index, <code>false</code>
   *         otherwise
   */
  protected boolean isNewlineAt(int index) {
    if (index < 0 || index + NEW_LINE.length() > source.length()) {
      return false;
    }
    for (int i = 0; i < NEW_LINE.length(); i++) {
      if (source.charAt(index) != NEW_LINE.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Test if there is a space at the given source index.
   * 
   * @param index the index to test
   * @return <code>true</code> if there is a space at the given source index, <code>false</code>
   *         otherwise
   */
  protected boolean isSpaceAt(int index) {
    if (index < 0 || index + 1 > source.length()) {
      return false;
    }
    return source.charAt(index) == ' ';
  }

  private boolean isIndentChar(char ch) {
    return ch == SPACE; // TODO(pquitslund) also check tab
  }

  private void wrongToken(String token) {
    throw new IllegalStateException("expected token: '" + token + "', actual: '" + currentToken
        + "'");
  }

}
