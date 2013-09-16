/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.handlers;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.internal.handlers.AbstractStructuredSelectHandler;

public class StructuredSelectPreviousHandler extends AbstractStructuredSelectHandler {

  protected IndexedRegion getCursorIndexedRegion(IDocument document, ITextSelection textSelection) {
    IndexedRegion indexedRegion = null;

    indexedRegion = getIndexedRegion(document, textSelection.getOffset());

    return indexedRegion;
  }

  protected Region getNewSelectionRegion(IndexedRegion indexedRegion, ITextSelection textSelection) {
    Region newRegion = null;
    if (indexedRegion instanceof ICSSNode) {
      ICSSNode cursorNode = (ICSSNode) indexedRegion;

      Region cursorNodeRegion = new Region(indexedRegion.getStartOffset(),
          indexedRegion.getEndOffset() - indexedRegion.getStartOffset());
      int currentOffset = textSelection.getOffset();
      int currentEndOffset = currentOffset + textSelection.getLength();
      if (cursorNodeRegion.getOffset() >= currentOffset
          && cursorNodeRegion.getOffset() <= currentEndOffset
          && cursorNodeRegion.getOffset() + cursorNodeRegion.getLength() >= currentOffset
          && cursorNodeRegion.getOffset() + cursorNodeRegion.getLength() <= currentEndOffset) {
        ICSSNode newNode = cursorNode.getPreviousSibling();
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
            newRegion = new Region(newIndexedRegion.getStartOffset(), currentEndOffset
                - newIndexedRegion.getStartOffset());
          }
        }
      } else
        newRegion = cursorNodeRegion;
    }
    return newRegion;
  }
}
