/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.selection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SelectionHistory {
  private ITextEditor fEditor;
  private List fHistory;
  private List fHistoryActions;
  private int fSelectionChangeListenerCounter;
  private ISelectionChangedListener fSelectionListener;

  public SelectionHistory(ITextEditor editor) {
    Assert.isNotNull(editor);
    fEditor = editor;
    fHistory = new ArrayList(3);
    fSelectionListener = new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        if (fSelectionChangeListenerCounter == 0)
          flush();
      }
    };
    fEditor.getSelectionProvider().addSelectionChangedListener(fSelectionListener);
  }

  public void dispose() {
    fEditor.getSelectionProvider().removeSelectionChangedListener(fSelectionListener);
    fEditor = null;
    if (fHistory != null && !fHistory.isEmpty()) {
      fHistory.clear();
    }
    if (fHistoryActions != null && !fHistoryActions.isEmpty()) {
      fHistoryActions.clear();
    }
  }

  public void flush() {
    if (fHistory.isEmpty())
      return;
    fHistory.clear();
    updateHistoryAction();
  }

  public IRegion getLast() {
    if (isEmpty())
      return null;
    int size = fHistory.size();
    IRegion result = (IRegion) fHistory.remove(size - 1);
    updateHistoryAction();
    return result;
  }

  public void ignoreSelectionChanges() {
    fSelectionChangeListenerCounter++;
  }

  public boolean isEmpty() {
    return fHistory.isEmpty();
  }

  public void listenToSelectionChanges() {
    fSelectionChangeListenerCounter--;
  }

  public void remember(IRegion region) {
    fHistory.add(region);
    updateHistoryAction();
  }

  public void setHistoryAction(IAction action) {
    Assert.isNotNull(action);

    if (fHistoryActions == null)
      fHistoryActions = new ArrayList();
    if (!fHistoryActions.contains(action))
      fHistoryActions.add(action);

    // update action
    if (fHistory != null && !fHistory.isEmpty())
      action.setEnabled(true);
    else
      action.setEnabled(false);
  }

  private void updateHistoryAction() {
    if (fHistoryActions != null && !fHistoryActions.isEmpty()) {
      boolean enabled = false;
      if (fHistory != null && !fHistory.isEmpty())
        enabled = true;

      Iterator iter = fHistoryActions.iterator();
      while (iter.hasNext()) {
        ((IAction) iter.next()).setEnabled(enabled);
      }
    }
  }
}
