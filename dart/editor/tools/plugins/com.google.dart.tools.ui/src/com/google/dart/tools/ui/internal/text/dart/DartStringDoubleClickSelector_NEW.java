/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.dart;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

/**
 * Double click strategy aware of Java string and character syntax rules.
 * 
 * @coverage dart.editor.ui.text
 */
public class DartStringDoubleClickSelector_NEW extends DartDoubleClickSelector_OLD {
  @Override
  public void doubleClicked(ITextViewer textViewer) {
    // prepare offset
    int offset = textViewer.getSelectedRange().x;
    if (offset < 0) {
      return;
    }
    // prepare document
    IDocument document = textViewer.getDocument();
    // try to get string region
    IRegion region = match(document, offset);
    if (region != null && region.getLength() > 0) {
      textViewer.setSelectedRange(region.getOffset(), region.getLength());
    } else {
      region = selectWord(document, offset);
      textViewer.setSelectedRange(region.getOffset(), region.getLength());
    }
  }

  private boolean isQuote(char c) {
    return c == '"' || c == '\'';
  }

  private boolean isQuote(IDocument document, int offset) throws BadLocationException {
    char c = document.getChar(offset);
    return isQuote(c);
  }

  private IRegion match(IDocument doc, int offset) {
    try {
      int docLength = doc.getLength();
      // previous is quote, search forward
      {
        char c = doc.getChar(offset - 1);
        if (isQuote(c)) {
          int end;
          if (offset >= 3 && isQuote(doc, offset - 2) && isQuote(doc, offset - 3)) {
            // triple quote
            end = match(doc, offset, 1, doc.get(offset - 3, 3));
          } else {
            // single quote
            end = match(doc, offset, 1, c);
          }
          if (end == -1) {
            return null;
          }
          return new Region(offset, end - offset);
        }
      }
      // next is quote, search backward
      {
        char c = doc.getChar(offset);
        if (isQuote(c)) {
          int end;
          if (offset + 2 < docLength && isQuote(doc, offset + 1) && isQuote(doc, offset + 2)) {
            // triple quote
            end = match(doc, offset - 1, -1, doc.get(offset, 3));
            if (end == -1) {
              return null;
            }
            end += 3;
          } else {
            // single quote
            end = match(doc, offset - 1, -1, c);
            if (end == -1) {
              return null;
            }
            end += 1;
          }
          return new Region(end, offset - end);
        }
      }
    } catch (BadLocationException e) {
    }
    return null;
  }

  private int match(IDocument doc, int offset, int delta, char charToFind)
      throws BadLocationException {
    String stringToFind = new String(new char[] {charToFind});
    return match(doc, offset, delta, stringToFind);
  }

  private int match(IDocument doc, int offset, int delta, String strToFind)
      throws BadLocationException {
    int docLength = doc.getLength();
    int strLength = strToFind.length();
    for (;; offset += delta) {
      if (offset < 0) {
        return -1;
      }
      if (offset + strLength >= docLength) {
        return -1;
      }
      if (doc.get(offset, strLength).equals(strToFind)) {
        return offset;
      }
    }
  }
}
