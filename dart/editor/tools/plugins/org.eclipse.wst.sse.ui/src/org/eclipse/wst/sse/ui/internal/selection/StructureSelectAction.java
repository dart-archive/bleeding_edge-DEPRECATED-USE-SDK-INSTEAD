/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.selection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.util.Assert;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.w3c.dom.Node;

/**
 * @deprecated use StructuredSelectActionDelegate instead
 */

public abstract class StructureSelectAction extends Action {
  protected StructuredTextEditor fEditor = null;
  protected SelectionHistory fHistory;
  protected IStructuredModel fModel = null;
  protected StructuredTextViewer fViewer = null;

  public StructureSelectAction(StructuredTextEditor editor) {
    super();

    Assert.isNotNull(editor);
    fEditor = editor;
    fHistory = (SelectionHistory) editor.getAdapter(SelectionHistory.class);
    fViewer = editor.getTextViewer();
    fModel = editor.getModel();
    Assert.isNotNull(fViewer);
  }

  abstract protected IndexedRegion getCursorIndexedRegion();

  protected IndexedRegion getIndexedRegion(int offset) {
    IndexedRegion indexedRegion = null;

    int lastOffset = offset;
    IDocument document = fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
    IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
        document);
    if (model != null) {
      try {
        indexedRegion = model.getIndexedRegion(lastOffset);
        while (indexedRegion == null && lastOffset >= 0) {
          lastOffset--;
          indexedRegion = model.getIndexedRegion(lastOffset);
        }
      } finally {
        model.releaseFromRead();
      }
    }

    return indexedRegion;
  }

  abstract protected Region getNewSelectionRegion(Node node, Region region);

  public void run() {
    Region currentRegion = new Region(fViewer.getSelectedRange().x, fViewer.getSelectedRange().y);
    if (currentRegion.getLength() == fViewer.getDocument().getLength())
      return;

    IndexedRegion cursorIndexedRegion = getCursorIndexedRegion();
    if (cursorIndexedRegion instanceof Node) {
      Node cursorNode = (Node) cursorIndexedRegion;

      // use parent node for empty text node
      if (cursorNode.getNodeType() == Node.TEXT_NODE
          && cursorNode.getNodeValue().trim().length() == 0) {
        cursorNode = cursorNode.getParentNode();

        if (cursorNode instanceof IndexedRegion)
          cursorIndexedRegion = (IndexedRegion) cursorNode;
      }

      Region cursorNodeRegion = new Region(cursorIndexedRegion.getStartOffset(),
          cursorIndexedRegion.getEndOffset() - cursorIndexedRegion.getStartOffset());

      Region newRegion = null;
      if (cursorNodeRegion.getOffset() >= currentRegion.getOffset()
          && cursorNodeRegion.getOffset() <= currentRegion.getOffset() + currentRegion.getLength()
          && cursorNodeRegion.getOffset() + cursorNodeRegion.getLength() >= currentRegion.getOffset()
          && cursorNodeRegion.getOffset() + cursorNodeRegion.getLength() <= currentRegion.getOffset()
              + currentRegion.getLength())
        newRegion = getNewSelectionRegion(cursorNode, currentRegion);
      else
        newRegion = cursorNodeRegion;

      if (newRegion != null) {
        fHistory.remember(currentRegion);
        try {
          fHistory.ignoreSelectionChanges();
          fEditor.selectAndReveal(newRegion.getOffset(), newRegion.getLength());
        } finally {
          fHistory.listenToSelectionChanges();
        }
      }
    }
  }
}
