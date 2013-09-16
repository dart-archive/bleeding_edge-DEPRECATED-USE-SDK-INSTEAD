/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.selection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;

abstract public class StructuredSelectActionDelegate implements IEditorActionDelegate,
    IActionDelegate2, IViewActionDelegate {
  private IEditorPart fEditor;
  private SelectionHistory fHistory;

  public void dispose() {
    fEditor = null;
    fHistory = null;
  }

  public void init(IViewPart view) {
    // do nothing
  }

  public void runWithEvent(IAction action, Event event) {
    run(action);
  }

  public void run(IAction action) {
    if (fEditor instanceof ITextEditor) {
      ITextEditor textEditor = (ITextEditor) fEditor;

      ISelection selection = textEditor.getSelectionProvider().getSelection();
      IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
      // determine current text selection
      if (selection instanceof ITextSelection && document != null) {
        ITextSelection textSelection = (ITextSelection) selection;

        if (textSelection.getLength() < document.getLength()) {
          // get current indexed region
          IndexedRegion cursorIndexedRegion = getCursorIndexedRegion(document, textSelection);

          // determine new selection based on current indexed region
          Region newSelectionRegion = getNewSelectionRegion(cursorIndexedRegion, textSelection);

          // select new selection
          if (newSelectionRegion != null) {
            fHistory.remember(new Region(textSelection.getOffset(), textSelection.getLength()));
            try {
              fHistory.ignoreSelectionChanges();
              textEditor.selectAndReveal(newSelectionRegion.getOffset(),
                  newSelectionRegion.getLength());
            } finally {
              fHistory.listenToSelectionChanges();
            }
          }
        }
      }
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    // do nothing
  }

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    fEditor = targetEditor;
    if (fEditor != null)
      fHistory = (SelectionHistory) fEditor.getAdapter(SelectionHistory.class);
    else
      fHistory = null;
  }

  abstract protected IndexedRegion getCursorIndexedRegion(IDocument document,
      ITextSelection textSelection);

  abstract protected Region getNewSelectionRegion(IndexedRegion indexedRegion,
      ITextSelection textSelection);

  /**
   * This method will probably be removed and replaced by using new selection provider
   * 
   * @param document
   * @param offset
   * @return
   */
  protected IndexedRegion getIndexedRegion(IDocument document, int offset) {
    IndexedRegion indexedRegion = null;

    int lastOffset = offset;
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

}
