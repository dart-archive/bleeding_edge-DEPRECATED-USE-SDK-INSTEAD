/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.ibm.icu.text.BreakIterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import java.text.CharacterIterator;

/**
 * Copied from DefaultTextDoubleClickStrategy. Same behavior, but also allows
 * <code>@identifier</code> to be selected.
 */
public class DartDocDoubleClickStrategy implements ITextDoubleClickStrategy {

  /**
   * Implements a character iterator that works directly on instances of <code>IDocument</code>.
   * Used to collaborate with the break iterator.
   * 
   * @see IDocument
   */
  static class DocumentCharacterIterator implements CharacterIterator {

    /** Document to iterate over. */
    private IDocument fDocument;
    /** Start offset of iteration. */
    private int fOffset = -1;
    /** End offset of iteration. */
    private int fEndOffset = -1;
    /** Current offset of iteration. */
    private int fIndex = -1;

    /** Creates a new document iterator. */
    public DocumentCharacterIterator() {
    }

    /*
     * @see CharacterIterator#clone()
     */
    @Override
    public Object clone() {
      DocumentCharacterIterator i = new DocumentCharacterIterator();
      i.fDocument = fDocument;
      i.fIndex = fIndex;
      i.fOffset = fOffset;
      i.fEndOffset = fEndOffset;
      return i;
    }

    /*
     * @see CharacterIterator#current()
     */
    @Override
    public char current() {
      if (fOffset <= fIndex && fIndex < fEndOffset) {
        try {
          return fDocument.getChar(fIndex);
        } catch (BadLocationException x) {
        }
      }
      return DONE;
    }

    /*
     * @see CharacterIterator#first()
     */
    @Override
    public char first() {
      fIndex = fOffset;
      return current();
    }

    /*
     * @see CharacterIterator#getBeginIndex()
     */
    @Override
    public int getBeginIndex() {
      return fOffset;
    }

    /*
     * @see CharacterIterator#getEndIndex()
     */
    @Override
    public int getEndIndex() {
      return fEndOffset;
    }

    /*
     * @see CharacterIterator#getIndex()
     */
    @Override
    public int getIndex() {
      return fIndex;
    }

    /*
     * @see CharacterIterator#last()
     */
    @Override
    public char last() {
      fIndex = fOffset < fEndOffset ? fEndOffset - 1 : fEndOffset;
      return current();
    }

    /*
     * @see CharacterIterator#next()
     */
    @Override
    public char next() {
      ++fIndex;
      int end = getEndIndex();
      if (fIndex >= end) {
        fIndex = end;
        return DONE;
      }
      return current();
    }

    /*
     * @see CharacterIterator#previous()
     */
    @Override
    public char previous() {
      if (fIndex == fOffset) {
        return DONE;
      }

      if (fIndex > fOffset) {
        --fIndex;
      }

      return current();
    }

    /**
     * Configures this document iterator with the document section to be visited.
     * 
     * @param document the document to be iterated
     * @param iteratorRange the range in the document to be iterated
     */
    public void setDocument(IDocument document, IRegion iteratorRange) {
      fDocument = document;
      fOffset = iteratorRange.getOffset();
      fEndOffset = fOffset + iteratorRange.getLength();
    }

    /*
     * @see CharacterIterator#setIndex(int)
     */
    @Override
    public char setIndex(int index) {
      fIndex = index;
      return current();
    }
  }

  /**
   * The document character iterator used by this strategy.
   */
  private DocumentCharacterIterator fDocIter = new DocumentCharacterIterator();

  /**
   * Creates a new default text double click strategy.
   */
  public DartDocDoubleClickStrategy() {
    super();
  }

  /*
   * @see org.eclipse.jface.text.ITextDoubleClickStrategy#doubleClicked(org.eclipse
   * .jface.text.ITextViewer)
   */
  @Override
  public void doubleClicked(ITextViewer text) {

    int position = text.getSelectedRange().x;

    if (position < 0) {
      return;
    }

    IRegion word = getWordRegion(text.getDocument(), position);

    if (word != null) {
      text.setSelectedRange(word.getOffset(), word.getLength());
    }
  }

  /**
   * Returns a region describing the word around <code>position</code>.
   * 
   * @param document the document
   * @param position the offset around which to return the word
   * @return the word's region, or <code>null</code> for no selection
   */
  private IRegion getWordRegion(IDocument document, int position) {
    try {

      IRegion line = document.getLineInformationOfOffset(position);
      if (position == line.getOffset() + line.getLength()) {
        return null;
      }

      fDocIter.setDocument(document, line);

      BreakIterator breakIter = BreakIterator.getWordInstance();
      breakIter.setText(fDocIter);

      int start = breakIter.preceding(position);
      if (start == BreakIterator.DONE) {
        start = line.getOffset();
      }

      int end = breakIter.following(position);
      if (end == BreakIterator.DONE) {
        end = line.getOffset() + line.getLength();
      }

      if (breakIter.isBoundary(position)) {
        if (end - position > position - start) {
          start = position;
        } else {
          end = position;
        }
      }

      if (start > 0
          && document.getChar(start - 1) == '@'
          && Character.isJavaIdentifierPart(document.getChar(start))
          && (start == 1 || Character.isWhitespace(document.getChar(start - 2)) || document.getChar(start - 2) == '{')) {
        // double click after @ident
        start--;
      } else if (end == position && end == start + 1 && end < line.getOffset() + line.getLength()
          && document.getChar(end) == '@') {
        // double click before " @ident"
        return getWordRegion(document, position + 1);
      }

      if (start == end) {
        return null;
      }
      return new Region(start, end - start);

    } catch (BadLocationException x) {
      return null;
    }
  }
}
