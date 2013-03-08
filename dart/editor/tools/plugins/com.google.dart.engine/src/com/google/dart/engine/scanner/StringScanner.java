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
package com.google.dart.engine.scanner;

import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code StringScanner} implement a scanner that reads from a string. The
 * scanning logic is in the superclass.
 * 
 * @coverage dart.engine.parser
 */
public class StringScanner extends AbstractScanner {
  /**
   * The offset from the beginning of the file to the beginning of the source being scanned.
   */
  private int offsetDelta;

  /**
   * The string from which characters will be read.
   */
  private final String string;

  /**
   * The number of characters in the string.
   */
  private final int stringLength;

  /**
   * The index, relative to the string, of the last character that was read.
   */
  private int charOffset;

  /**
   * Initialize a newly created scanner to scan the characters in the given string.
   * 
   * @param source the source being scanned
   * @param string the string from which characters will be read
   * @param errorListener the error listener that will be informed of any errors that are found
   */
  public StringScanner(Source source, String string, AnalysisErrorListener errorListener) {
    super(source, errorListener);
    this.offsetDelta = 0;
    this.string = string;
    this.stringLength = string.length();
    this.charOffset = -1;
  }

  @Override
  public int getOffset() {
    return offsetDelta + charOffset;
  }

  /**
   * Record that the source begins on the given line and column at the given offset. The line starts
   * for lines before the given line will not be correct.
   * <p>
   * This method must be invoked at most one time and must be invoked before scanning begins. The
   * values provided must be sensible. The results are undefined if these conditions are violated.
   * 
   * @param line the one-based index of the line containing the first character of the source
   * @param column the one-based index of the column in which the first character of the source
   *          occurs
   * @param offset the zero-based offset from the beginning of the larger context to the first
   *          character of the source
   */
  public void setSourceStart(int line, int column, int offset) {
    if (line < 1 || column < 1 || offset < 0 || (line + column - 2) >= offset) {
      return;
    }
    offsetDelta = 1;
    for (int i = 2; i < line; i++) {
      recordStartOfLine();
    }
    offsetDelta = offset - column + 1;
    recordStartOfLine();
    offsetDelta = offset;
  }

  @Override
  protected int advance() {
    if (charOffset + 1 >= stringLength) {
      return -1;
    }
    return string.charAt(++charOffset);
  }

  @Override
  protected String getString(int start, int endDelta) {
    return string.substring(start - offsetDelta, charOffset + 1 + endDelta);
  }

  @Override
  protected int peek() {
    if (charOffset + 1 >= string.length()) {
      return -1;
    }
    return string.charAt(charOffset + 1);
  }
}
