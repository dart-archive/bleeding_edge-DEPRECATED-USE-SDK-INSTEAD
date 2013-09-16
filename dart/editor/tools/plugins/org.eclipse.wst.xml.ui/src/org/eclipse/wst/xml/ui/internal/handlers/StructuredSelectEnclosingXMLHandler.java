/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.handlers;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.internal.handlers.AbstractStructuredSelectHandler;
import org.w3c.dom.Node;

public class StructuredSelectEnclosingXMLHandler extends AbstractStructuredSelectHandler {
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
        Node newNode = cursorNode.getParentNode();

        if (newNode instanceof IndexedRegion) {
          IndexedRegion newIndexedRegion = (IndexedRegion) newNode;
          newRegion = new Region(newIndexedRegion.getStartOffset(), newIndexedRegion.getEndOffset()
              - newIndexedRegion.getStartOffset());
        }
      } else {
        newRegion = cursorNodeRegion;
      }
    }
    return newRegion;
  }
}
