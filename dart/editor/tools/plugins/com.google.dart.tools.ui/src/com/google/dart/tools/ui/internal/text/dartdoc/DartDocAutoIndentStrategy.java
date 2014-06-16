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

import com.google.dart.tools.ui.text.DartPartitions;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

/**
 * A simple auto indent strategy for DartDoc comments (as well as regular multi-line comments).
 * 
 * @coverage dart.editor.ui.text.dart
 */
public class DartDocAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
  private String partitioning;

  public DartDocAutoIndentStrategy(String partitioning) {
    this.partitioning = partitioning;
  }

  @Override
  public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
    if (!isValidPartition(document, command.offset)) {
      return;
    }
    if (command.text != null) {
      if (command.length == 0) {
        String[] lineDelimiters = document.getLegalLineDelimiters();
        int idx = TextUtilities.endsWith(lineDelimiters, command.text);
        if (idx > -1) {
          if (lineDelimiters[idx].equals(command.text)) {
            // auto-indent only if adding a newline and nothing else
            autoIndentAfterNewLine(document, command);
          }
          return;
        }
      }
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
      if (firstNotWS == -1) {
        firstNotWS = 0;
      }
      String strWS = strLine.substring(0, firstNotWS);
      String strAfterWS = strLine.substring(firstNotWS);

      String lineDelimiter = TextUtilities.getDefaultLineDelimiter(d);
      StringBuffer buf = new StringBuffer();

      buf.append(lineDelimiter);
      buf.append(strWS);
      if (strAfterWS.startsWith("/")) {
        buf.append(" ");
      } else if (firstNotWS == 0) {
        buf.append(" ");
      }
      buf.append("* ");
      int newCaretOffset = offset + buf.length();
      if (strLine.endsWith("*/")) {
        buf.append(lineDelimiter);
        buf.append(strWS);
        buf.append(" ");
      } else if (isNewComment(d, offset)) {
        buf.append(lineDelimiter);
        buf.append(strWS);
        buf.append(" */");
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

  /**
   * Guesses if the command operates within a newly created DartDoc comment or not. If in doubt, it
   * will assume that the DartDoc is new.
   * 
   * @param document the document
   * @param commandOffset the command offset
   * @return <code>true</code> if the comment should be closed, <code>false</code> if not
   */
  private boolean isNewComment(IDocument document, int commandOffset) {

    try {
      int lineIndex = document.getLineOfOffset(commandOffset) + 1;
      if (lineIndex >= document.getNumberOfLines()) {
        return true;
      }

      IRegion line = document.getLineInformation(lineIndex);
      ITypedRegion partition = TextUtilities.getPartition(
          document,
          partitioning,
          commandOffset,
          false);
      int partitionEnd = partition.getOffset() + partition.getLength();
      if (line.getOffset() >= partitionEnd) {
        return false;
      }

      if (document.getLength() == partitionEnd) {
        return true; // partition goes to end of document - probably a new comment 
      }

      String comment = document.get(partition.getOffset(), partition.getLength());
      if (comment.indexOf("/*", 2) != -1) {
        return true; // enclosed another comment -> probably a new comment 
      }

      return false;

    } catch (BadLocationException e) {
      return false;
    }
  }

  private boolean isValidPartition(IDocument document, int offset) {
    try {
      String contentType = TextUtilities.getContentType(document, partitioning, offset, false);
      return DartPartitions.DART_DOC.equals(contentType)
          || DartPartitions.DART_MULTI_LINE_COMMENT.equals(contentType)
          || DartPartitions.DART_SINGLE_LINE_DOC.equals(contentType);
    } catch (BadLocationException e) {
    }
    return false;
  }
}
