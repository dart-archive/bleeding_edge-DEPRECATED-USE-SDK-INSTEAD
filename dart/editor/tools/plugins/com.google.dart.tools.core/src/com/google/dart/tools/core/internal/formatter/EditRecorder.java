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

import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.formatter.align.Alignment;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Given a source string and a list of tokens in that string, record a series of edits to the string
 * that will cause the string to be formatted when applied.
 */
public class EditRecorder {

  private static final String NEWLINE = System.getProperty("line.separator");
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

  @SuppressWarnings("unused")
  private int column;
  @SuppressWarnings("unused")
  private Alignment alignment;
  private Token token;
  private OptimizedReplaceEdit[] edits;
  private int editsIndex;
  private int indentLevel;
  private String source;
  private int sourceIndex;
  private boolean isIndentNeeded;
  private IRegion[] regions;
  @SuppressWarnings("unused")
  private DefaultCodeFormatterOptions preferences;

  public EditRecorder(String source, Token token, IRegion[] regions,
      DefaultCodeFormatterOptions preferences) {
    this.token = token;
    this.source = source;
    this.sourceIndex = 0;
    this.column = 0;
    this.edits = new OptimizedReplaceEdit[source.length() / 10 + 1];
    this.editsIndex = 0;
    this.indentLevel = 0;
    this.isIndentNeeded = false;
    this.regions = regions;
    this.preferences = preferences;
//    this.alignment = new Alignment(Alignment.MESSAGE_SEND, 0, 0, null, 0, 0, 0); // TODO(messick): need new Alignment class.
  }

  public void advance(Token token) {
    if (this.token.equals(token)) {
      // TODO(messick) emit comments
      if (isIndentNeeded) {
        advanceIndent();
        isIndentNeeded = false;
      }
      // record writing a token at the current edit location
      int len = token.getLength();
      advance(len);
      this.token = this.token.getNext();
    } else {
      throw new IllegalStateException("unexpected token: " + token.getLexeme());
    }
  }

  public TextEdit buildEditCommands() {
    MultiTextEdit edit = null;
    IRegion[] adaptedRegions = adaptRegions(regions);
    adaptedRegions[0] = new Region(0, source.length());
    int regionsLength = adaptedRegions.length;
    int textRegionStart;
    int textRegionEnd;
    if (regionsLength == 1) {
      IRegion lastRegion = adaptedRegions[0];
      textRegionStart = lastRegion.getOffset();
      textRegionEnd = textRegionStart + lastRegion.getLength();
    } else {
      textRegionStart = adaptedRegions[0].getOffset();
      IRegion lastRegion = adaptedRegions[regionsLength - 1];
      textRegionEnd = lastRegion.getOffset() + lastRegion.getLength();
    }

    int length = textRegionEnd - textRegionStart + 1;
    if (textRegionStart <= 0) {
      if (length <= 0) {
        edit = new MultiTextEdit(0, 0);
      } else {
        edit = new MultiTextEdit(0, textRegionEnd);
      }
    } else {
      edit = new MultiTextEdit(textRegionStart, length - 1);
    }
    int endPosition = source.length() - 1;
    for (int i = 0, max = editsIndex; i < max; i++) {
      OptimizedReplaceEdit currentEdit = edits[i];
      if (currentEdit.offset >= 0 && currentEdit.offset <= endPosition) {
        if (currentEdit.length == 0 || currentEdit.offset != endPosition
            && isMeaningfulEdit(currentEdit)) {
          try {
            edit.addChild(new ReplaceEdit(
                currentEdit.offset,
                currentEdit.length,
                currentEdit.replacement));
          } catch (MalformedTreeException ex) {
            DartCore.logError(ex);
            throw ex;
          }
        }
      }
    }
    // TODO(messick) free up edits to help gc, after things work reasonably well
    return edit;
  }

  public void indent() {
    // Increase indent level by 1.
    indentLevel++;
  }

  public boolean isEmpty() {
    return sourceIndex == 0;
  }

  public void newline() {
    // TODO(messick) emit comments
    // If there is a newline before the edit location, do nothing.
    isIndentNeeded = true;
    if (isNewlineAt(sourceIndex - NEWLINE.length())) {
      return;
    }
    // If there is a newline after the edit location, advance over it.
    if (isNewlineAt(sourceIndex)) {
      advance(NEWLINE.length());
      return;
    }
    // Otherwise, replace whitespace with a newline.
    int charsToReplace = countWhitespace();
    addEdit(sourceIndex, charsToReplace, NEWLINE);
    column += NEWLINE.length();
    sourceIndex += charsToReplace;
  }

  public void space() {
    // TODO(messick) emit comments
    // If there is a space before the edit location, do nothing.
    if (isSpaceAt(sourceIndex - 1)) {
      return;
    }
    // If there is a space after the edit location, advance over it.
    if (isSpaceAt(sourceIndex)) {
      advance(1);
      return;
    }
    // Otherwise, replace spaces with a single space.
    int charsToReplace = countWhitespace();
    addEdit(sourceIndex, charsToReplace, SPACES[0]);
    column += 1;
    sourceIndex += charsToReplace;
  }

  public void unindent() {
    // Decrease indent level by 1.
    indentLevel--;
    assert (indentLevel >= 0);
  }

  private IRegion[] adaptRegions(IRegion[] regions) {
    // TODO(messick) regions are not yet supported -- currently formatting entire source string
    if (regions.length == 1) {
      // It's not necessary to adapt the single region which covers all the source
      if (regions[0].getOffset() == 0 && regions[0].getLength() == source.length()) {
        return regions;
      }
    }
    // TODO(messick) adapt regions around comments
    // If a region begins within a comment move the beginning to the start of the comment.
    // If a regions ends within a comment move the end to the end location of the comment.
    return regions;
  }

  private void addEdit(int index, int count, String string) {
    // Add an edit record to change count chars beginning at index to string.
    OptimizedReplaceEdit edit = new OptimizedReplaceEdit(index, count, string);
    edits[editsIndex++] = edit;
    if (editsIndex == edits.length) {
      OptimizedReplaceEdit[] newEdits = new OptimizedReplaceEdit[edits.length * 2];
      System.arraycopy(edits, 0, newEdits, 0, edits.length);
      edits = newEdits;
    }
  }

  private void advance(int len) {
    // Move indices by len characters.
    column += len;
    sourceIndex += len;
  }

  private void advanceIndent() {
    // Move indices past indent, adding edit record if needed to adjust indentation
    int indentPerLevel = getIndentPerLevel();
    int indentWidth = indentPerLevel * indentLevel;
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

  private int countWhitespace() {
    // Count the number of whitespace chars beginning at sourceIndex
    int count = 0;
    for (int i = sourceIndex; i < source.length(); i++) {
      if (isIndentChar(source.charAt(i))) {
        count += 1;
      } else {
        break;
      }
    }
    return count;
  }

  private int getIndentPerLevel() {
    return 2; // fetch value from preferences
  }

  private boolean isIndentChar(char ch) {
    return ch == SPACE; // TODO(messick) also check tab
  }

  private boolean isMeaningfulEdit(OptimizedReplaceEdit edit) {
    final int editLength = edit.length;
    final int editReplacementLength = edit.replacement.length();
    final int editOffset = edit.offset;
    if (editReplacementLength != 0 && editLength == editReplacementLength) {
      for (int i = editOffset, max = editOffset + editLength; i < max; i++) {
        if (source.charAt(i) != edit.replacement.charAt(i - editOffset)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private boolean isNewlineAt(int index) {
    // Return true if the chars beginning at index represent a newline
    if (index < 0 || index + NEWLINE.length() > source.length()) {
      return false;
    }
    for (int i = 0; i < NEWLINE.length(); i++) {
      if (source.charAt(index + i) != NEWLINE.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private boolean isSpaceAt(int index) {
    return source.charAt(index) == ' ';
  }
}
