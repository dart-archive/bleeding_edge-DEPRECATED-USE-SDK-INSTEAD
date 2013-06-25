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
package com.google.dart.tools.internal.corext.refactoring.code;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.ReplaceEdit;

import java.util.List;

/**
 * Extract Local Variable (from selected expression inside method or initializer).
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractUtils {

  /**
   * Describes where to insert new directive or top-level declaration at the top of file.
   */
  public class TopInsertDesc {
    public int offset;
    public boolean insertEmptyLineBefore;
    public boolean insertEmptyLineAfter;
  }

  /**
   * The default end-of-line marker for the current platform. This value should (almost) never be
   * used directly. The end-of-line marker should always be queried from {@link Buffer} because it
   * can differ from the platform default in some situations.
   */
  public static final String DEFAULT_END_OF_LINE = System.getProperty("line.separator", "\n");

  /**
   * Sorts given {@link ReplaceEdit}s and returns updated {@link String} with applied replaces.
   */
  private final Buffer buffer;

  private final DartUnit unitNode;
  private String endOfLine;

  public ExtractUtils(CompilationUnit unit) throws DartModelException {
    this.buffer = unit.getBuffer();
    this.unitNode = null;
  }

  /**
   * @return the EOL to use for this {@link CompilationUnit}.
   */
  public String getEndOfLine() {
    if (endOfLine == null) {
      endOfLine = ExecutionUtils.runObjectIgnore(new RunnableObjectEx<String>() {
        @Override
        public String runObject() throws Exception {
          // find first EOL
          IDocument document = new Document(buffer.getContents());
          int numberOfLines = document.getNumberOfLines();
          for (int i = 0; i < numberOfLines; i++) {
            String delimiter = document.getLineDelimiter(i);
            if (delimiter != null) {
              return delimiter;
            }
          }
          // no EOL, use default
          return DEFAULT_END_OF_LINE;
        }
      }, DEFAULT_END_OF_LINE);
    }
    return endOfLine;
  }

  /**
   * @return the index of the first not space or tab on the right from the given one, if form
   *         statement or method end, then this is in most cases start of the next line.
   */
  public int getLineContentEnd(int index) {
    int length = buffer.getLength();
    // skip whitespace characters
    while (index < length) {
      char c = buffer.getChar(index);
      if (!Character.isWhitespace(c) || c == '\r' || c == '\n') {
        break;
      }
      index++;
    }
    // skip single \r
    if (index < length && buffer.getChar(index) == '\r') {
      index++;
    }
    // skip single \n
    if (index < length && buffer.getChar(index) == '\n') {
      index++;
    }
    // done
    return index;
  }

  /**
   * @return the index of the last space or tab on the left from the given one, if from statement or
   *         method start, then this is in most cases start of the line.
   */
  public int getLineContentStart(int index) {
    while (index > 0) {
      char c = buffer.getChar(index - 1);
      if (c != ' ' && c != '\t') {
        break;
      }
      index--;
    }
    return index;
  }

  /**
   * @return the full text from {@link Buffer}.
   */
  public String getText() {
    return buffer.getContents();
  }

  /**
   * @return the given range of text from {@link Buffer}.
   */
  public String getText(int offset, int length) {
    return buffer.getText(offset, length);
  }

  /**
   * @return the offset of the token on the right from given "offset" on the same line or offset of
   *         the next line.
   */
  public int getTokenOrNextLineOffset(int offset) {
    int nextOffset = getLineContentEnd(offset);
    String sourceToNext = getText(offset, nextOffset - offset);
    List<com.google.dart.engine.scanner.Token> tokens = TokenUtils.getTokens(sourceToNext);
    if (tokens.isEmpty()) {
      return nextOffset;
    }
    return tokens.get(0).getOffset();
  }

  /**
   * @return the resolved {@link DartUnit}.
   */
  public DartUnit getUnitNode() {
    return unitNode;
  }
}
