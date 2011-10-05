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
package com.google.dart.tools.ui.internal.text.html;

import java.io.IOException;
import java.io.Reader;

/**
 * Reads the text contents from a reader and computes for each character a potential substitution.
 * The substitution may eat more characters than only the one passed into the computation routine.
 * <p>
 * Moved into this package from <code>org.eclipse.jface.internal.text.revisions</code>.
 * </p>
 */
public abstract class SubstitutionTextReader extends SingleCharReader {

  protected static final String LINE_DELIM = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

  private Reader fReader;
  protected boolean fWasWhiteSpace;
  private int fCharAfterWhiteSpace;

  /**
   * Tells whether white space characters are skipped.
   */
  private boolean fSkipWhiteSpace = true;

  private boolean fReadFromBuffer;
  private StringBuffer fBuffer;
  private int fIndex;

  protected SubstitutionTextReader(Reader reader) {
    fReader = reader;
    fBuffer = new StringBuffer();
    fIndex = 0;
    fReadFromBuffer = false;
    fCharAfterWhiteSpace = -1;
    fWasWhiteSpace = true;
  }

  /**
   * @see Reader#close()
   */
  @Override
  public void close() throws IOException {
    fReader.close();
  }

  /**
   * @see Reader#read()
   */
  @Override
  public int read() throws IOException {
    int c;
    do {

      c = nextChar();
      while (!fReadFromBuffer) {
        String s = computeSubstitution(c);
        if (s == null) {
          break;
        }
        if (s.length() > 0) {
          fBuffer.insert(0, s);
        }
        c = nextChar();
      }

    } while (fSkipWhiteSpace && fWasWhiteSpace && (c == ' '));
    fWasWhiteSpace = (c == ' ' || c == '\r' || c == '\n');
    return c;
  }

  /**
   * @see Reader#ready()
   */
  @Override
  public boolean ready() throws IOException {
    return fReader.ready();
  }

  /**
   * @see Reader#reset()
   */
  @Override
  public void reset() throws IOException {
    fReader.reset();
    fWasWhiteSpace = true;
    fCharAfterWhiteSpace = -1;
    fBuffer.setLength(0);
    fIndex = 0;
  }

  /**
   * Computes the substitution for the given character and if necessary subsequent characters.
   * Implementation should use <code>nextChar</code> to read subsequent characters.
   * 
   * @param c the character to be substituted
   * @return the substitution for <code>c</code>
   * @throws IOException in case computing the substitution fails
   */
  protected abstract String computeSubstitution(int c) throws IOException;

  /**
   * Returns the internal reader.
   * 
   * @return the internal reader
   */
  protected Reader getReader() {
    return fReader;
  }

  protected final boolean isSkippingWhitespace() {
    return fSkipWhiteSpace;
  }

  /**
   * Returns the next character.
   * 
   * @return the next character
   * @throws IOException in case reading the character fails
   */
  protected int nextChar() throws IOException {
    fReadFromBuffer = (fBuffer.length() > 0);
    if (fReadFromBuffer) {
      char ch = fBuffer.charAt(fIndex++);
      if (fIndex >= fBuffer.length()) {
        fBuffer.setLength(0);
        fIndex = 0;
      }
      return ch;
    }

    int ch = fCharAfterWhiteSpace;
    if (ch == -1) {
      ch = fReader.read();
    }
    if (fSkipWhiteSpace && Character.isWhitespace((char) ch)) {
      do {
        ch = fReader.read();
      } while (Character.isWhitespace((char) ch));
      if (ch != -1) {
        fCharAfterWhiteSpace = ch;
        return ' ';
      }
    } else {
      fCharAfterWhiteSpace = -1;
    }
    return ch;
  }

  protected final void setSkipWhitespace(boolean state) {
    fSkipWhiteSpace = state;
  }
}
