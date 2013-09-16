/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;

/**
 * Undo manager exclusively for StructuredTextViewers ONLY. This undo manager is aware that
 * structured documents have their own undo manager. This handles communication between IUndoManager
 * and IStructuredTextUndoManager.
 */
class StructuredTextViewerUndoManager implements IUndoManager {
  class UndoNotifier implements ISelectionChangedListener {
    public void selectionChanged(SelectionChangedEvent event) {
      if ((fUndoManager != null) && (event != null)) {
        if (event.getSelection() instanceof ITextSelection) {
          fUndoManager.forceEndOfPendingCommand(this,
              ((ITextSelection) event.getSelection()).getOffset(),
              ((ITextSelection) event.getSelection()).getLength());
        }
      }
    }

  }

  private StructuredTextViewer fTextViewer = null;
  private IStructuredTextUndoManager fUndoManager = null;
  private ISelectionChangedListener fUndoNotifier = new UndoNotifier();

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#beginCompoundChange()
   */
  public void beginCompoundChange() {
    // forward the request to the model-based undo manager
    if (fUndoManager != null)
      fUndoManager.beginRecording(fTextViewer);
  }

  /**
   * Associates a viewer to this undo manager and also attempts to get the correct document-specific
   * undo manager.
   * 
   * @param viewer - Assumes viewer instanceof StructuredTextViewer
   */
  public void connect(ITextViewer viewer) {
    // future_TODO could probably optimize this to check if already
    // connected to same viewer/undo manager, dont do anything

    // disconnect from any old manager/viewer
    disconnect();

    // connect to new viewer/undo manager
    fTextViewer = (StructuredTextViewer) viewer;
    IDocument doc = fTextViewer.getDocument();
    if (doc instanceof IStructuredDocument) {
      IStructuredDocument structuredDocument = (IStructuredDocument) doc;
      setDocument(structuredDocument);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#disconnect()
   */
  public void disconnect() {
    // disconnect the viewer from the undo manager
    if (fUndoManager != null) {
      fTextViewer.removeSelectionChangedListener(fUndoNotifier);
      fUndoManager.disconnect(fTextViewer);
    }

    // null out the viewer and undo manager
    fTextViewer = null;
    fUndoManager = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#endCompoundChange()
   */
  public void endCompoundChange() {
    // forward the request to the model-based undo manager
    if (fUndoManager != null)
      fUndoManager.endRecording(fTextViewer);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#redo()
   */
  public void redo() {
    // forward the request to the model-based undo manager
    if (fUndoManager != null)
      fUndoManager.redo(fTextViewer);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#redoable()
   */
  public boolean redoable() {
    boolean canRedo = false;

    // forward the request to the model-based undo manager
    if (fUndoManager != null)
      canRedo = fUndoManager.redoable();

    return canRedo;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#reset()
   */
  public void reset() {
    // cannot really reset model-based undo manager because other clients
    // will be affected
  }

  /**
   * Disconnect from the old undo manager and connect to the undo manager associated with the new
   * document.
   * 
   * @param document - assumes document is not null
   */
  public void setDocument(IStructuredDocument document) {
    if (fUndoManager != null) {
      fTextViewer.removeSelectionChangedListener(fUndoNotifier);
      fUndoManager.disconnect(fTextViewer);
    }

    fUndoManager = document.getUndoManager();
    if (fUndoManager != null) {
      fUndoManager.connect(fTextViewer);
      fTextViewer.addSelectionChangedListener(fUndoNotifier);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#setMaximalUndoLevel(int)
   */
  public void setMaximalUndoLevel(int undoLevel) {
    // cannot really set maximal undo level on model-based undo manager
    // because other clients will be affected
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#undo()
   */
  public void undo() {
    // forward the request to the model-based undo manager
    if (fUndoManager != null)
      fUndoManager.undo(fTextViewer);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.IUndoManager#undoable()
   */
  public boolean undoable() {
    boolean canUndo = false;

    // forward the request to the model-based undo manager
    if (fUndoManager != null)
      canUndo = fUndoManager.undoable();

    return canUndo;
  }
}
