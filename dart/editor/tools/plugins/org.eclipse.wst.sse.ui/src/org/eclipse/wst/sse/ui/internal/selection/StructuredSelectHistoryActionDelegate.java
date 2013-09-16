/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.selection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;

/**
 * Selection history action delegate. Keeps track of selection within editor.
 */
public class StructuredSelectHistoryActionDelegate implements IEditorActionDelegate,
    IActionDelegate2, IViewActionDelegate {
  private IEditorPart fEditor;
  private SelectionHistory fHistory;

  public void dispose() {
    fEditor = null;
    fHistory = null;
  }

  public void init(IAction action) {
    if (action != null) {
      action.setText(SSEUIMessages.StructureSelectHistory_label);
      action.setToolTipText(SSEUIMessages.StructureSelectHistory_tooltip);
      action.setDescription(SSEUIMessages.StructureSelectHistory_description);
    }
  }

  public void init(IViewPart view) {
    // do nothing
  }

  public void run(IAction action) {
    IRegion old = fHistory.getLast();
    if (old != null) {
      try {
        fHistory.ignoreSelectionChanges();
        if (fEditor instanceof ITextEditor)
          ((ITextEditor) fEditor).selectAndReveal(old.getOffset(), old.getLength());
      } finally {
        fHistory.listenToSelectionChanges();
      }
    }
  }

  public void runWithEvent(IAction action, Event event) {
    run(action);
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
    if (fHistory != null) {
      fHistory.setHistoryAction(action);
    }
  }
}
