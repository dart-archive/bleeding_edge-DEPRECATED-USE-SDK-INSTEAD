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

public class StructuredSelectPreviousXMLActionDelegate extends StructuredSelectActionDelegate {

  protected IndexedRegion getCursorIndexedRegion(IDocument document, ITextSelection textSelection) {
    IndexedRegion indexedRegion = null;

    indexedRegion = getIndexedRegion(document, textSelection.getOffset());

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

      if ((cursorNodeRegion.getOffset() >= textSelection.getOffset())
          && (cursorNodeRegion.getOffset() <= textSelection.getOffset() + textSelection.getLength())
          && (cursorNodeRegion.getOffset() + cursorNodeRegion.getLength() >= textSelection.getOffset())
          && (cursorNodeRegion.getOffset() + cursorNodeRegion.getLength() <= textSelection.getOffset()
              + textSelection.getLength())) {
        Node newNode = cursorNode.getPreviousSibling();
        if (newNode == null) {
          newNode = cursorNode.getParentNode();

          if (newNode instanceof IndexedRegion) {
            IndexedRegion newIndexedRegion = (IndexedRegion) newNode;
            newRegion = new Region(newIndexedRegion.getStartOffset(),
                newIndexedRegion.getEndOffset() - newIndexedRegion.getStartOffset());
          }
        } else {
          if (newNode instanceof IndexedRegion) {
            IndexedRegion newIndexedRegion = (IndexedRegion) newNode;
            newRegion = new Region(newIndexedRegion.getStartOffset(), textSelection.getOffset()
                + textSelection.getLength() - newIndexedRegion.getStartOffset());

            if (newNode.getNodeType() == Node.TEXT_NODE) {
              newRegion = getNewSelectionRegion(newIndexedRegion,
                  new TextSelection(newRegion.getOffset(), newRegion.getLength()));
            }
          }
        }

      } else {
        newRegion = cursorNodeRegion;
      }
    }
    return newRegion;
  }

  public void init(IAction action) {
    if (action != null) {
      action.setText(XMLUIMessages.StructureSelectPrevious_label);
      action.setToolTipText(XMLUIMessages.StructureSelectPrevious_tooltip);
      action.setDescription(XMLUIMessages.StructureSelectPrevious_description);
    }
  }

}
