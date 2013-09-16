/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.internal.selection.SelectionHistory;

abstract public class AbstractStructuredSelectHandler extends AbstractHandler {

  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    ITextEditor textEditor = null;
    if (editor instanceof ITextEditor)
      textEditor = (ITextEditor) editor;
    else {
      Object o = editor.getAdapter(ITextEditor.class);
      if (o != null)
        textEditor = (ITextEditor) o;
    }
    if (textEditor != null) {
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
            SelectionHistory history = (SelectionHistory) editor.getAdapter(SelectionHistory.class);
            if (history != null) {
              history.remember(new Region(textSelection.getOffset(), textSelection.getLength()));
              try {
                history.ignoreSelectionChanges();
                textEditor.selectAndReveal(newSelectionRegion.getOffset(),
                    newSelectionRegion.getLength());
              } finally {
                history.listenToSelectionChanges();
              }
            }
          }
        }
      }
    }
    return null;
  }

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

  abstract protected IndexedRegion getCursorIndexedRegion(IDocument document,
      ITextSelection textSelection);

  abstract protected Region getNewSelectionRegion(IndexedRegion indexedRegion,
      ITextSelection textSelection);
}
