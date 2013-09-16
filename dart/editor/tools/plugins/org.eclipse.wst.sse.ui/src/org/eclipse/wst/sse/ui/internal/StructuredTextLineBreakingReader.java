/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.swt.graphics.GC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/*
 * Not a real reader. Could change if requested
 */
public class StructuredTextLineBreakingReader {
  private GC fGC;
  private int fIndex;
  private String fLine;
  private int fMaxWidth;

  private BufferedReader fReader;

  /**
   * Creates a reader that breaks an input text to fit in a given width.
   * 
   * @param reader Reader of the input text
   * @param gc The graphic context that defines the currently used font sizes
   * @param maxLineWidth The max width (pixes) where the text has to fit in
   */
  public StructuredTextLineBreakingReader(Reader reader, GC gc, int maxLineWidth) {
    fReader = new BufferedReader(reader);
    fGC = gc;
    fMaxWidth = maxLineWidth;
    fLine = null;
    fIndex = 0;
  }

  private int findNextBreakIndex(int currIndex) {
    int currWidth = 0;
    int lineLength = fLine.length();

    while (currIndex < lineLength) {
      char ch = fLine.charAt(currIndex);
      int nextIndex = currIndex + 1;
      // leading whitespaces are counted to the following word
      if (Character.isWhitespace(ch)) {
        while (nextIndex < lineLength && Character.isWhitespace(fLine.charAt(nextIndex))) {
          nextIndex++;
        }
      }
      while (nextIndex < lineLength && !Character.isWhitespace(fLine.charAt(nextIndex))) {
        nextIndex++;
      }
      String word = fLine.substring(currIndex, nextIndex);
      int wordWidth = fGC.textExtent(word).x;
      int nextWidth = wordWidth + currWidth;
      if (nextWidth > fMaxWidth && wordWidth < fMaxWidth) {
        return currIndex;
      }
      currWidth = nextWidth;
      currIndex = nextIndex;
    }
    return currIndex;
  }

  private int findWordBegin(int idx) {
    while (idx < fLine.length() && Character.isWhitespace(fLine.charAt(idx))) {
      idx++;
    }
    return idx;
  }

  /**
   * Reads the next line. The lengths of the line will not exceed the gived maximum width.
   */
  public String readLine() throws IOException {
    if (fLine == null) {
      String line = fReader.readLine();
      if (line == null) {
        return null;
      }
      int lineLen = fGC.textExtent(line).x;
      if (lineLen < fMaxWidth) {
        return line;
      }
      fLine = line;
      fIndex = 0;
    }
    int breakIdx = findNextBreakIndex(fIndex);
    String res = fLine.substring(fIndex, breakIdx);
    if (breakIdx < fLine.length()) {
      fIndex = findWordBegin(breakIdx);
    } else {
      fLine = null;
    }
    return res;
  }
}
