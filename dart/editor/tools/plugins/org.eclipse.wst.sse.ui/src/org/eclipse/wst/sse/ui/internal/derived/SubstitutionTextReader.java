/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.derived;

import java.io.IOException;
import java.io.Reader;

/*
 * Copied from org.eclipse.jdt.internal.ui.text.SubstitutionTextReader. Modifications were made to
 * read() to allow whitespaces and fixed statement unnecessarily nested within else clause warning
 * in nextChar()
 */
/**
 * Reads the text contents from a reader and computes for each character a potential substitution.
 * The substitution may eat more characters than only the one passed into the computation routine.
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
   * Implement to compute the substitution for the given character and if necessary subsequent
   * characters. Use <code>nextChar</code> to read subsequent characters.
   */
  protected abstract String computeSubstitution(int c) throws IOException;

  /**
   * Returns the internal reader.
   */
  protected Reader getReader() {
    return fReader;
  }

  /**
   * Returns the next character.
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

  /**
   * @see Reader#read()
   */
  public int read() throws IOException {
    int c;
    do {

      c = nextChar();
      while (!fReadFromBuffer && c != -1) {
        String s = computeSubstitution(c);
        if (s == null)
          break;
        if (s.length() > 0)
          fBuffer.insert(0, s);
        c = nextChar();
      }

    } while (fSkipWhiteSpace && fWasWhiteSpace && ((c == ' ') && !fReadFromBuffer));
    /*
     * SSE: For above and below check, if whitespace is read from buffer, do not skip
     */
    fWasWhiteSpace = ((c == ' ' && !fReadFromBuffer) || c == '\r' || c == '\n');
    return c;
  }

  /**
   * @see Reader#ready()
   */
  public boolean ready() throws IOException {
    return fReader.ready();
  }

  /**
   * @see Reader#close()
   */
  public void close() throws IOException {
    fReader.close();
  }

  /**
   * @see Reader#reset()
   */
  public void reset() throws IOException {
    fReader.reset();
    fWasWhiteSpace = true;
    fCharAfterWhiteSpace = -1;
    fBuffer.setLength(0);
    fIndex = 0;
  }

  protected final void setSkipWhitespace(boolean state) {
    fSkipWhiteSpace = state;
  }

  protected final boolean isSkippingWhitespace() {
    return fSkipWhiteSpace;
  }
}
