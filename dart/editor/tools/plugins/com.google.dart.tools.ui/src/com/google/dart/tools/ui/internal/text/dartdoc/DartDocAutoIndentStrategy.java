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

package com.google.dart.tools.ui.internal.text.dartdoc;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

/**
 * A simple auto indent strategy for Dartdoc comments (as well as regular multi-line comments).
 * 
 * @coverage dart.editor.ui.text.dart
 */
public class DartDocAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
  public DartDocAutoIndentStrategy(String partitioning) {
  }

  @Override
  public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
    if (c.length == 0 && c.text != null
        && TextUtilities.endsWith(d.getLegalLineDelimiters(), c.text) != -1) {
      autoIndentAfterNewLine(d, c);
    }
  }

  /**
   * Copies the indentation of the previous line
   * {@link #customizeDocumentCommand(IDocument, DocumentCommand)}.
   * 
   * @see DartDocAutoIndentStrategy
   * @param d the document to work on
   * @param c the command to deal with
   */
  protected void autoIndentAfterNewLine(IDocument d, DocumentCommand c) {
    int offset = c.offset;
    if (offset == -1 || d.getLength() == 0) {
      return;
    }

    try {
      // find start of line
      int p = (offset == d.getLength() ? offset - 1 : offset);
      IRegion info = d.getLineInformationOfOffset(p);
      int start = info.getOffset();
      int end = start + info.getLength();

      // split line
      String strLine = d.get(start, end - start);
      int firstNotWS = StringUtils.indexOfAnyBut(strLine, " \t");
      String strWS = strLine.substring(0, firstNotWS);
      String strAfterWS = strLine.substring(firstNotWS);

      String lineDelimiter = TextUtilities.getDefaultLineDelimiter(d);
      StringBuffer buf = new StringBuffer();

      buf.append(lineDelimiter);
      buf.append(strWS);
      if (strAfterWS.startsWith("/")) {
        buf.append(" ");
      }
      buf.append("* ");
      int newCaretOffset = offset + buf.length();
      if (strLine.endsWith("*/")) {
        buf.append(lineDelimiter);
        buf.append(strWS);
        buf.append(" ");
      }
      c.shiftsCaret = false;
      c.caretOffset = newCaretOffset;
      c.text = buf.toString();
    } catch (BadLocationException excp) {
      // stop work
    }
  }

  protected String getLineStart(IDocument document, int offset, int end, boolean includeNonWs)
      throws BadLocationException {
    int start = offset;

    while (offset < end) {
      char c = document.getChar(offset);

      if (c != ' ' && c != '\t') {
        if (includeNonWs) {
          return document.get(start, offset + 1 - start);
        } else {
          return document.get(start, offset - start);
        }
      }

      offset++;
    }

    return document.get(start, end - start);
  }
}
