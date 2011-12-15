/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;

/**
 * A buffered document scanner. The buffer always contains a section of a fixed size of the document
 * to be scanned.
 */

public final class BufferedDocumentScanner implements ICharacterScanner {

  /** The document being scanned. */
  private IDocument fDocument;
  /** The offset of the document range to scan. */
  private int fRangeOffset;
  /** The length of the document range to scan. */
  private int fRangeLength;
  /** The delimiters of the document. */
  private char[][] fDelimiters;

  /** The buffer. */
  private final char[] fBuffer;
  /** The offset of the buffer within the document. */
  private int fBufferOffset;
  /** The valid length of the buffer for access. */
  private int fBufferLength;
  /** The offset of the scanner within the buffer. */
  private int fOffset;

  /**
   * Creates a new buffered document scanner. The buffer size is set to the given number of
   * characters.
   * 
   * @param size the buffer size
   */
  public BufferedDocumentScanner(int size) {
    Assert.isTrue(size >= 1);
    fBuffer = new char[size];
  }

  @Override
  public final int getColumn() {

    try {
      final int offset = fBufferOffset + fOffset;
      final int line = fDocument.getLineOfOffset(offset);
      final int start = fDocument.getLineOffset(line);
      return offset - start;
    } catch (BadLocationException e) {
    }

    return -1;
  }

  @Override
  public final char[][] getLegalLineDelimiters() {
    return fDelimiters;
  }

  /**
   * Peek ahead in the buffer without changing the current offset. The distance, which must be both
   * non-negative and less than the maximum size of the buffer, is the number of characters past the
   * current position to look. A distance of zero (<code>0</code>) will return the same character
   * that {@link #read()} would return, but without advancing the position.
   * 
   * @param distance the number of characters past the current character to look
   * @return the character at the specified position, or {@link ICharacterScanner#EOF} if the
   *         position is past the end of the document
   */
  public final int peek(int distance) {
    if (distance < 0) {
      throw new IllegalArgumentException("Cannot peek before current position (distance = "
          + distance + ")");
    } else if (distance >= fBuffer.length) {
      throw new IllegalArgumentException("Cannot peek more than " + (fBuffer.length - 1)
          + " character after the current position (distance = " + distance + ")");
    }
    int targetOffset = fOffset + distance;
    if (targetOffset >= fBufferLength) {
      int end = fBufferOffset + fBufferLength;
      if (end == fDocument.getLength() || end == fRangeOffset + fRangeLength) {
        return EOF;
      } else {
        updateBuffer(fBufferOffset + fOffset);
        fOffset = 0;
        targetOffset = distance;
      }
    }
    if (targetOffset < fBufferLength) {
      return fBuffer[targetOffset];
    } else {
      return EOF;
    }
  }

  @Override
  public final int read() {

    if (fOffset == fBufferLength) {
      int end = fBufferOffset + fBufferLength;
      if (end == fDocument.getLength() || end == fRangeOffset + fRangeLength) {
        return EOF;
      } else {
        updateBuffer(fBufferOffset + fBufferLength);
        fOffset = 0;
      }
    }

    try {
      return fBuffer[fOffset++];
    } catch (ArrayIndexOutOfBoundsException ex) {
      StringBuffer buf = new StringBuffer();
      buf.append("Detailed state of 'BufferedDocumentScanner:'"); //$NON-NLS-1$
      buf.append("\n\tfOffset= "); //$NON-NLS-1$
      buf.append(fOffset);
      buf.append("\n\tfBufferOffset= "); //$NON-NLS-1$
      buf.append(fBufferOffset);
      buf.append("\n\tfBufferLength= "); //$NON-NLS-1$
      buf.append(fBufferLength);
      buf.append("\n\tfRangeOffset= "); //$NON-NLS-1$
      buf.append(fRangeOffset);
      buf.append("\n\tfRangeLength= "); //$NON-NLS-1$
      buf.append(fRangeLength);
      DartToolsPlugin.logErrorMessage(buf.toString());
      throw ex;
    }
  }

  /**
   * Configures the scanner by providing access to the document range over which to scan.
   * 
   * @param document the document to scan
   * @param offset the offset of the document range to scan
   * @param length the length of the document range to scan
   */
  public final void setRange(IDocument document, int offset, int length) {

    fDocument = document;
    fRangeOffset = offset;
    fRangeLength = length;

    String[] delimiters = document.getLegalLineDelimiters();
    fDelimiters = new char[delimiters.length][];
    for (int i = 0; i < delimiters.length; i++) {
      fDelimiters[i] = delimiters[i].toCharArray();
    }

    updateBuffer(offset);
    fOffset = 0;
  }

  @Override
  public final void unread() {

    if (fOffset == 0) {
      if (fBufferOffset == fRangeOffset) {
        // error: BOF
      } else {
        updateBuffer(fBufferOffset - fBuffer.length);
        fOffset = fBuffer.length - 1;
      }
    } else {
      --fOffset;
    }
  }

  /**
   * Fills the buffer with the contents of the document starting at the given offset.
   * 
   * @param offset the document offset at which the buffer starts
   */
  private final void updateBuffer(int offset) {

    fBufferOffset = offset;

    if (fBufferOffset + fBuffer.length > fRangeOffset + fRangeLength) {
      fBufferLength = fRangeLength - (fBufferOffset - fRangeOffset);
    } else {
      fBufferLength = fBuffer.length;
    }

    try {
      final String content = fDocument.get(fBufferOffset, fBufferLength);
      content.getChars(0, fBufferLength, fBuffer, 0);
    } catch (BadLocationException e) {
    }
  }
}
