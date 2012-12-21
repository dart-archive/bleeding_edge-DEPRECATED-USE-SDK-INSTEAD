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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

/**
 * A simple auto indent strategy for Dartdoc comments (as well as regular multi-line comments).
 */
public class DartDocAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
  private String partitioning;

  public DartDocAutoIndentStrategy(String partitioning) {
    this.partitioning = partitioning;
  }

  @Override
  public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
    if (c.length == 0 && c.text != null
        && TextUtilities.endsWith(d.getLegalLineDelimiters(), c.text) != -1) {
      autoIndentAfterNewLine(d, c);
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
   * Copies the indentation of the previous line
   * {@link #customizeDocumentCommand(IDocument, DocumentCommand)}.
   * 
   * @see DartDocAutoIndentStrategy
   * @param d the document to work on
   * @param c the command to deal with
   */
  private void autoIndentAfterNewLine(IDocument d, DocumentCommand c) {
    int offset = c.offset;
    if (offset == -1 || d.getLength() == 0) {
      return;
    }

    try {
      // find start of line
      int p = (offset == d.getLength() ? offset - 1 : offset);
      IRegion info = d.getLineInformationOfOffset(p);
      int start = info.getOffset();

      // find white spaces
      int end = findEndOfWhiteSpace(d, start, offset);

      String startStr = getLineStart(d, start, offset, true);

      StringBuffer buf = new StringBuffer(c.text);

      if (end > start) {
        // append to input
        buf.append(d.get(start, end - start));
      }

      if (startStr.endsWith("*")) {
        // look for post '*' whitespace and preserve it (for 4-space code indents)
        String postWs = getLineStart(d, end + 1, offset, false);

        if (postWs.length() > 0) {
          buf.append("*");
          buf.append(postWs);
        } else {
          // append the Dartdoc continuation chars
          buf.append("* ");
        }
      } else if (startStr.endsWith("/")) {
        // indent one more then the /** indent
        buf.append(" * ");
        if (isNewComment(d, offset)) {
          c.shiftsCaret = false;
          c.caretOffset = offset + buf.length();
          String lineDelimiter = TextUtilities.getDefaultLineDelimiter(d);

          int eolOffset = start + info.getLength();
          int replacementLength = eolOffset - p;
          String restOfLine = d.get(p, replacementLength);
          IRegion prefix = findPrefixRange(d, info);
          String indentation = d.get(prefix.getOffset(), prefix.getLength());
          String endTag = lineDelimiter + indentation + " */"; //$NON-NLS-1$

          c.length = replacementLength;
          buf.append(restOfLine);
          buf.append(endTag);
        }
      }

      c.text = buf.toString();

    } catch (BadLocationException excp) {
      // stop work
    }
  }

  /**
   * Returns the range of the Javadoc prefix on the given line in <code>document</code>. The prefix
   * greedily matches the following regex pattern: <code>\w*\*\w*</code>, that is, any number of
   * whitespace characters, followed by an asterisk ('*'), followed by any number of whitespace
   * characters.
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
    if (indentEnd < lineEnd && document.getChar(indentEnd) == '*') {
      indentEnd++;
      while (indentEnd < lineEnd && document.getChar(indentEnd) == ' ') {
        indentEnd++;
      }
    }
    return new Region(lineOffset, indentEnd - lineOffset);
  }

  /**
   * Guesses if the command operates within a newly created Javadoc comment or not. If in doubt, it
   * will assume that the Javadoc is new.
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
}
