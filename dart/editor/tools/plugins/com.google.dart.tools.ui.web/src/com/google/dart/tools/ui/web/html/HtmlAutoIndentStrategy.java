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

package com.google.dart.tools.ui.web.html;

import com.google.dart.tools.ui.web.utils.WebEditorAutoIndentStrategy;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * An indent strategy for html.
 */
public class HtmlAutoIndentStrategy extends WebEditorAutoIndentStrategy {

  public HtmlAutoIndentStrategy() {

  }

  @Override
  protected void doAutoIndentAfterNewLine(IDocument document, DocumentCommand command) {
    if (command.offset == -1 || document.getLength() == 0) {
      return;
    }

    try {
      // find start of line
      int location = (command.offset == document.getLength() ? command.offset - 1 : command.offset);
      IRegion lineInfo = document.getLineInformationOfOffset(location);
      int start = lineInfo.getOffset();

      boolean endsInBracket = (document.getChar(command.offset - 1) == '>');
      String startStr = document.get(start, command.offset - start);

      // find white spaces
      int end = findEndOfWhiteSpace(document, start, command.offset);

      StringBuffer buf = new StringBuffer(command.text);

      if (end > start) {
        // append to input
        buf.append(document.get(start, end - start));
      }

      // Indent after a >+eol, but not if we're closing an element tag.
      if (endsInBracket && !(startStr.contains("</") || startStr.contains("/>"))
          && !(startStr.toLowerCase().endsWith("<p>"))) {
        buf.append("  ");
      }

      command.text = buf.toString();
    } catch (BadLocationException excp) {
      // stop work
    }
  }

}
