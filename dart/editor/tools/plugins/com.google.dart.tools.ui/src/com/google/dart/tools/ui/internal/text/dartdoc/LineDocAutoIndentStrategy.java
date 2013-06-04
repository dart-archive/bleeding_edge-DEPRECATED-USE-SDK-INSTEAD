/*
 * Copyright (c) 2013, the Dart project authors.
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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class LineDocAutoIndentStrategy extends DartDocAutoIndentStrategy {

  public LineDocAutoIndentStrategy(String partitioning) {
    super(partitioning);
  }

  /**
   * Copies the indentation of the previous line
   * {@link #customizeDocumentCommand(IDocument, DocumentCommand)}.
   * 
   * @see DartDocAutoIndentStrategy
   * @param d the document to work on
   * @param c the command to deal with
   */
  @Override
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
      String startStr = getLineStart(d, start, offset, true);
      StringBuffer buf = new StringBuffer(c.text);
      if (startStr.endsWith("/")) {
        IRegion prefix = findPrefixRange(d, info);
        String indentation = d.get(prefix.getOffset(), prefix.getLength());
        buf.append(indentation);
      }

      c.text = buf.toString();

    } catch (BadLocationException excp) {
      // stop work
    }
  }

  /**
   * Returns the range of the Dart doc prefix on the given line in <code>document</code>. The prefix
   * greedily matches the following pattern: any number of whitespace characters, followed by at
   * least two plus any number of slashes, followed by any number of whitespace characters.
   * 
   * @param document the document to which <code>line</code> refers
   * @param line the line from which to extract the prefix range
   * @return an <code>IRegion</code> describing the range of the prefix on the given line
   * @throws BadLocationException if accessing the document fails
   */
  private IRegion findPrefixRange(IDocument document, IRegion line) throws BadLocationException {
    int lineOffset = line.getOffset();
    int lineEnd = lineOffset + line.getLength();
    int indentEnd = findEndOfWhiteSpace(document, lineOffset, lineEnd);
    int initialIndentEnd = indentEnd;
    while (indentEnd < lineEnd && document.getChar(indentEnd) == '/') {
      indentEnd++;
    }
    if (indentEnd - initialIndentEnd < 2) {
      return new Region(lineOffset, initialIndentEnd - lineOffset);
    }
    while (indentEnd < lineEnd && document.getChar(indentEnd) == ' ') {
      indentEnd++;
    }
    return new Region(lineOffset, indentEnd - lineOffset);
  }
}
