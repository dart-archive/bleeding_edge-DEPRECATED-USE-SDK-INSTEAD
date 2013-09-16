/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.selection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.internal.selection.StructuredSelectActionDelegate;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.w3c.dom.Node;

public class StructuredSelectNextXMLActionDelegate extends StructuredSelectActionDelegate {

  protected IndexedRegion getCursorIndexedRegion(IDocument document, ITextSelection textSelection) {
    int offset = textSelection.getOffset() + textSelection.getLength() - 1;
    if (offset < 0) {
      offset = 0;
    }

    IndexedRegion indexedRegion = null;

    indexedRegion = getIndexedRegion(document, offset);

    return indexedRegion;
  }

  protected Region getNewSelectionRegion(IndexedRegion indexedRegion, ITextSelection textSelection) {
    Region newRegion = null;
    if (indexedRegion instanceof Node) {
      Node cursorNode = (Node) indexedRegion;

      // use parent node for empty text node
      if ((cursorNode.getNodeType() == Node.TEXT_NODE)
          && (cursorNode.getNodeValue().trim().length() == 0)) {
        cursorNode = cursorNode.getParentNode();

        if (cursorNode instanceof IndexedRegion) {
          indexedRegion = (IndexedRegion) cursorNode;
        }
      }

      Region cursorNodeRegion = new Region(indexedRegion.getStartOffset(),
          indexedRegion.getEndOffset() - indexedRegion.getStartOffset());
      int currentOffset = textSelection.getOffset();
      int currentEndOffset = currentOffset + textSelection.getLength();
      if ((cursorNodeRegion.getOffset() >= currentOffset)
          && (cursorNodeRegion.getOffset() <= currentEndOffset)
          && (cursorNodeRegion.getOffset() + cursorNodeRegion.getLength() >= currentOffset)
          && (cursorNodeRegion.getOffset() + cursorNodeRegion.getLength() <= currentEndOffset)) {
        newRegion = getNewSelectionRegion2(indexedRegion, textSelection);
      } else {
        newRegion = cursorNodeRegion;
      }
    }
    return newRegion;
  }

  public void init(IAction action) {
    if (action != null) {
      action.setText(XMLUIMessages.StructureSelectNext_label);
      action.setToolTipText(XMLUIMessages.StructureSelectNext_tooltip);
      action.setDescription(XMLUIMessages.StructureSelectNext_description);
    }
  }

  /**
   * This method was separated out from getNewSelectionRegion2 because the code in here is allowed
   * to be called recursively.
   * 
   * @param indexedRegion
   * @param textSelection
   * @return new region to select or null if none
   */
  private Region getNewSelectionRegion2(IndexedRegion indexedRegion, ITextSelection textSelection) {
    Region newRegion = null;
    if (indexedRegion instanceof Node) {
      Node node = (Node) indexedRegion;
      Node newNode = node.getNextSibling();
      if (newNode == null) {
        newNode = node.getParentNode();

        if (newNode instanceof IndexedRegion) {
          IndexedRegion newIndexedRegion = (IndexedRegion) newNode;
          newRegion = new Region(newIndexedRegion.getStartOffset(), newIndexedRegion.getEndOffset()
              - newIndexedRegion.getStartOffset());
        }
      } else {
        if (newNode instanceof IndexedRegion) {
          IndexedRegion newIndexedRegion = (IndexedRegion) newNode;
          newRegion = new Region(textSelection.getOffset(), newIndexedRegion.getEndOffset()
              - textSelection.getOffset());

          if (newNode.getNodeType() == Node.TEXT_NODE) {
            newRegion = getNewSelectionRegion2(newIndexedRegion,
                new TextSelection(newRegion.getOffset(), newRegion.getLength()));
          }
        }
      }
    }
    return newRegion;
  }
}
