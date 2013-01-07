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

package com.google.dart.tools.ui.web.yaml;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

/**
 * An indent strategy for yaml.
 */
public class YamlAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {

  public YamlAutoIndentStrategy() {

  }

  @Override
  public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
    if (c.length == 0 && c.text != null
        && TextUtilities.endsWith(d.getLegalLineDelimiters(), c.text) != -1) {
      autoIndentAfterNewLine(d, c);
    }
  }

  /**
   * Copies the indentation of the previous line.
   * 
   * @param d the document to work on
   * @param c the command to deal with
   */
  private void autoIndentAfterNewLine(IDocument d, DocumentCommand c) {
    if (c.offset == -1 || d.getLength() == 0) {
      return;
    }

    try {
      // find start of line
      int p = (c.offset == d.getLength() ? c.offset - 1 : c.offset);
      IRegion info = d.getLineInformationOfOffset(p);
      int start = info.getOffset();

      boolean endsInColon = (d.getChar(c.offset - 1) == ':');

      // find white spaces
      int end = findEndOfWhiteSpace(d, start, c.offset);

      StringBuffer buf = new StringBuffer(c.text);

      if (end > start) {
        // append to input
        buf.append(d.get(start, end - start));
      }

      if (endsInColon) {
        buf.append("  ");
      }

      c.text = buf.toString();
    } catch (BadLocationException excp) {
      // stop work
    }
  }

}
