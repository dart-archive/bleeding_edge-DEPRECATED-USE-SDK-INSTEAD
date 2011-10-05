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
package com.google.dart.core.util;

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.formatter.IndentManipulation;

/**
 * TODO(devoncarew): This is a temporary class, used to resolve compilation errors.
 */
public class DartDocCommentReader extends SingleCharReader {

  private Buffer fBuffer;

  private int fCurrPos;

  private int fStartPos;

  private int fEndPos;

  private boolean fWasNewLine;

  public DartDocCommentReader(Buffer buf, int start, int end) {
    fBuffer = buf;
    fStartPos = start + 3;
    fEndPos = end - 2;

    reset();
  }

  /**
   * @see java.io.Reader#close()
   */
  @Override
  public void close() {
    fBuffer = null;
  }

  /**
   * Returns the offset of the last read character in the passed buffer.
   * 
   * @return the offset
   */
  public int getOffset() {
    return fCurrPos;
  }

  /**
   * @see java.io.Reader#read()
   */
  @Override
  public int read() {
    if (fCurrPos < fEndPos) {
      char ch;
      if (fWasNewLine) {
        do {
          ch = fBuffer.getChar(fCurrPos++);
        } while (fCurrPos < fEndPos && Character.isWhitespace(ch));
        if (ch == '*') {
          if (fCurrPos < fEndPos) {
            do {
              ch = fBuffer.getChar(fCurrPos++);
            } while (ch == '*');
          } else {
            return -1;
          }
        }
      } else {
        ch = fBuffer.getChar(fCurrPos++);
      }
      fWasNewLine = IndentManipulation.isLineDelimiterChar(ch);

      return ch;
    }
    return -1;
  }

  /**
   * @see java.io.Reader#reset()
   */
  @Override
  public void reset() {
    fCurrPos = fStartPos;
    fWasNewLine = true;
  }

}
