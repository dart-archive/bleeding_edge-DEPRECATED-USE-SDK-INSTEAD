/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.doubleclick;

import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Node;

public class XMLDoubleClickStrategy extends DefaultTextDoubleClickStrategy {
  protected static final char DOUBLE_QUOTE = '\"';
  protected static final char SINGLE_QUOTE = '\'';
  protected static final char SPACE = ' ';
  protected static final String RIGHT_MUSTACHE = "}}";
  protected static final String LEFT_MUSTACHE = "{{";
  protected int fCaretPosition = -1;
  protected int fDoubleClickCount = 0;
  protected Node fNode = null;
  protected IStructuredDocumentRegion fStructuredDocumentRegion = null;
  protected String fStructuredDocumentRegionText = ""; //$NON-NLS-1$
  protected IStructuredModel fStructuredModel = null;
  protected ITextViewer fTextViewer;
  protected ITextRegion fTextRegion = null;

  @Override
  public void doubleClicked(ITextViewer textViewer) {
    fTextViewer = textViewer;
    try {
      fStructuredModel = StructuredModelManager.getModelManager().getExistingModelForRead(
          fTextViewer.getDocument());

      if (fStructuredModel != null) {
        int caretPosition = textViewer.getSelectedRange().x;
        if (caretPosition < 0) {
          return;
        }

        fNode = (Node) fStructuredModel.getIndexedRegion(caretPosition);
        if (fNode == null) {
          return;
        }

        updateDoubleClickCount(caretPosition);
        updateStructuredDocumentRegion();
        updateTextRegion();

        if (fNode.getNodeType() == Node.TEXT_NODE) {
          processTextDoubleClicked();
        } else {
          processElementDoubleClicked();
        }
      }
    } finally {
      if (fStructuredModel != null) {
        fStructuredModel.releaseFromRead();
      }
    }
  }

  public void setModel(IStructuredModel structuredModel) {
    fStructuredModel = structuredModel;
  }

  protected Point getWord(String string, int cursor) {
    if (string == null) {
      return null;
    }

    int wordStart = 0;
    int wordEnd = string.length();
    boolean isMustache = false; // if true, allow for spaces in expression

    wordStart = string.lastIndexOf(SPACE, cursor - 1);
    int temp = string.lastIndexOf(SINGLE_QUOTE, cursor - 1);
    wordStart = Math.max(wordStart, temp);
    temp = string.lastIndexOf(DOUBLE_QUOTE, cursor - 1);
    wordStart = Math.max(wordStart, temp);
    temp = string.lastIndexOf(LEFT_MUSTACHE, cursor - 1);
    if (temp >= 0) {
      int newStart = Math.max(wordStart, temp + 1);
      if (newStart > wordStart) {
        wordStart = newStart;
        isMustache = true;
      }
    }
    if (wordStart == -1) {
      wordStart = cursor;
    } else {
      wordStart++;
    }

    wordEnd = string.indexOf(SPACE, cursor);
    if (wordEnd == -1) {
      wordEnd = string.length();
    }
    temp = string.indexOf(SINGLE_QUOTE, cursor);
    if (temp == -1) {
      temp = string.length();
    }
    wordEnd = Math.min(wordEnd, temp);
    temp = string.indexOf(DOUBLE_QUOTE, cursor);
    if (temp == -1) {
      temp = string.length();
    }
    wordEnd = Math.min(wordEnd, temp);
    temp = string.indexOf(RIGHT_MUSTACHE, cursor);
    if (temp == -1) {
      temp = string.length();
    } else if (isMustache) {
      wordEnd = temp;
    }
    if (wordEnd == string.length()) {
      wordEnd = cursor;
    }

    if ((wordStart == wordEnd) && !isQuoted(string)) {
      wordStart = 0;
      wordEnd = string.length();
    }

    return new Point(wordStart, wordEnd);
  }

  protected boolean isQuoted(String string) {
    if ((string == null) || (string.length() < 2)) {
      return false;
    }

    int lastIndex = string.length() - 1;
    char firstChar = string.charAt(0);
    char lastChar = string.charAt(lastIndex);

    return (((firstChar == SINGLE_QUOTE) && (lastChar == SINGLE_QUOTE)) || ((firstChar == DOUBLE_QUOTE) && (lastChar == DOUBLE_QUOTE)));
  }

  protected void processElementAttrEqualsDoubleClicked2Times() {
    int prevRegionOffset = fStructuredDocumentRegion.getStartOffset(fTextRegion) - 1;
    ITextRegion prevRegion = fStructuredDocumentRegion.getRegionAtCharacterOffset(prevRegionOffset);
    int nextRegionOffset = fStructuredDocumentRegion.getEndOffset(fTextRegion);
    ITextRegion nextRegion = fStructuredDocumentRegion.getRegionAtCharacterOffset(nextRegionOffset);

    if ((prevRegion != null) && (prevRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)
        && (nextRegion != null)
        && (nextRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)) {
      fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(prevRegion),
          nextRegion.getTextEnd() - prevRegion.getStart());
    }
  }

  protected void processElementAttrNameDoubleClicked2Times() {
    int nextRegionOffset = fStructuredDocumentRegion.getEndOffset(fTextRegion);
    ITextRegion nextRegion = fStructuredDocumentRegion.getRegionAtCharacterOffset(nextRegionOffset);

    if (nextRegion != null) {
      nextRegionOffset = fStructuredDocumentRegion.getEndOffset(nextRegion);
      nextRegion = fStructuredDocumentRegion.getRegionAtCharacterOffset(nextRegionOffset);
      if ((nextRegion != null)
          && (nextRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)) {
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(fTextRegion),
            nextRegion.getTextEnd() - fTextRegion.getStart());
      } else {
        // attribute has no value
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart(),
            fStructuredDocumentRegion.getLength());
        fDoubleClickCount = 0;
      }
    }
  }

  protected void processElementAttrValueDoubleClicked() {
    String regionText = fStructuredDocumentRegion.getText(fTextRegion);

    if (fDoubleClickCount == 1) {
      Point word = getWord(regionText,
          fCaretPosition - fStructuredDocumentRegion.getStartOffset(fTextRegion));
      if (word.x == word.y) { // no word found; select whole region
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(fTextRegion),
            regionText.length());
        fDoubleClickCount++;
      } else {
        fTextViewer.setSelectedRange(
            fStructuredDocumentRegion.getStartOffset(fTextRegion) + word.x, word.y - word.x);
      }
    } else if (fDoubleClickCount == 2) {
      if (isQuoted(regionText)) {
        // ==> // Point word = getWord(regionText, fCaretPosition -
        // fStructuredDocumentRegion.getStartOffset(fTextRegion));
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(fTextRegion),
            regionText.length());
      } else {
        processElementAttrValueDoubleClicked2Times();
      }
    } else if (fDoubleClickCount == 3) {
      if (isQuoted(regionText)) {
        processElementAttrValueDoubleClicked2Times();
      } else {
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart(),
            fStructuredDocumentRegion.getLength());
        fDoubleClickCount = 0;
      }
    } else { // fDoubleClickCount == 4
      fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart(),
          fStructuredDocumentRegion.getLength());
      fDoubleClickCount = 0;
    }
  }

  protected void processElementAttrValueDoubleClicked2Times() {
    int prevRegionOffset = fStructuredDocumentRegion.getStartOffset(fTextRegion) - 1;
    ITextRegion prevRegion = fStructuredDocumentRegion.getRegionAtCharacterOffset(prevRegionOffset);

    if (prevRegion != null) {
      prevRegionOffset = fStructuredDocumentRegion.getStartOffset(prevRegion) - 1;
      prevRegion = fStructuredDocumentRegion.getRegionAtCharacterOffset(prevRegionOffset);
      if ((prevRegion != null) && (prevRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(prevRegion),
            fTextRegion.getTextEnd() - prevRegion.getStart());
      }
    }
  }

  protected void processElementDoubleClicked() {
    if (fTextRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
      processElementAttrValueDoubleClicked(); // special handling for
    } else {
      if (fDoubleClickCount == 1) {
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart() + fTextRegion.getStart(),
            fTextRegion.getTextLength());
      } else if (fDoubleClickCount == 2) {
        if (fTextRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
          processElementAttrNameDoubleClicked2Times();
        } else if (fTextRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
          processElementAttrEqualsDoubleClicked2Times();
        } else {
          fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart(),
              fStructuredDocumentRegion.getLength());
          fDoubleClickCount = 0;
        }
      } else { // fDoubleClickCount == 3
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart(),
            fStructuredDocumentRegion.getLength());
        fDoubleClickCount = 0;
      }
    }
  }

  protected void processTextDoubleClicked() {
    if (fDoubleClickCount == 1) {
      super.doubleClicked(fTextViewer);

      Point selectedRange = fTextViewer.getSelectedRange();
      if ((selectedRange.x == fStructuredDocumentRegion.getStartOffset(fTextRegion))
          && (selectedRange.y == fTextRegion.getTextLength())) {
        // only one word in region, skip one level of double click
        // selection
        fDoubleClickCount++;
      }
    } else if (fDoubleClickCount == 2) {
      if (fTextRegion.getType() == DOMRegionContext.UNDEFINED) {
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart(),
            fStructuredDocumentRegion.getLength());
        fDoubleClickCount = 0;
      } else {
        if (isQuoted(fStructuredDocumentRegion.getFullText(fTextRegion))) {
          fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(fTextRegion) + 1,
              fTextRegion.getTextLength() - 2);
        } else {
          fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(fTextRegion),
              fTextRegion.getTextLength());
        }
      }
    } else {
      if ((fDoubleClickCount == 3) && isQuoted(fStructuredDocumentRegion.getFullText(fTextRegion))) {
        fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(fTextRegion),
            fTextRegion.getTextLength());
      } else {
        if ((fDoubleClickCount == 3) && isQuoted(fStructuredDocumentRegionText)) {
          fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart() + 1,
              fStructuredDocumentRegion.getLength() - 2);
        } else {
          fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStart(),
              fStructuredDocumentRegion.getLength());
          fDoubleClickCount = 0;
        }
      }
    }
  }

  protected void updateDoubleClickCount(int caretPosition) {
    if (fCaretPosition == caretPosition) {
      if (fStructuredDocumentRegion != null) {
        fDoubleClickCount++;
      } else {
        fDoubleClickCount = 1;
      }
    } else {
      fCaretPosition = caretPosition;
      fDoubleClickCount = 1;
    }
  }

  protected void updateStructuredDocumentRegion() {
    fStructuredDocumentRegion = fStructuredModel.getStructuredDocument().getRegionAtCharacterOffset(
        fCaretPosition);
    if (fStructuredDocumentRegion != null) {
      fStructuredDocumentRegionText = fStructuredDocumentRegion.getText();
    } else {
      fStructuredDocumentRegionText = ""; //$NON-NLS-1$
    }
  }

  protected void updateTextRegion() {
    if (fStructuredDocumentRegion != null) {
      fTextRegion = fStructuredDocumentRegion.getRegionAtCharacterOffset(fCaretPosition);
      // if fTextRegion is null, it means we are at just past the last
      // fStructuredDocumentRegion,
      // at the very end of the document, so we'll use the last text
      // region in the document
      if (fTextRegion == null) {
        fTextRegion = fStructuredDocumentRegion.getRegionAtCharacterOffset(fCaretPosition - 1);
      }
    } else {
      fTextRegion = null;
    }
  }
}
