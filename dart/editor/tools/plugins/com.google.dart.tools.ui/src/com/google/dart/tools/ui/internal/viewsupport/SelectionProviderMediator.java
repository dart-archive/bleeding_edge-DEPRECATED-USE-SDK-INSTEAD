/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

/**
 * A selection provider for view parts with more that one viewer. Tracks the focus of the viewers to
 * provide the correct selection.
 */
public class SelectionProviderMediator implements IPostSelectionProvider {

  private class InternalListener implements ISelectionChangedListener, FocusListener {
    /*
     * @see FocusListener#focusGained
     */
    @Override
    public void focusGained(FocusEvent e) {
      doFocusChanged(e.widget);
    }

    /*
     * @see FocusListener#focusLost
     */
    @Override
    public void focusLost(FocusEvent e) {
      // do not reset due to focus behavior on GTK
      // fViewerInFocus= null;
    }

    /*
     * @see ISelectionChangedListener#selectionChanged
     */
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      doSelectionChanged(event);
    }
  }

  private class InternalPostSelectionListener implements ISelectionChangedListener {
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      doPostSelectionChanged(event);
    }

  }

  private StructuredViewer[] fViewers;

  private StructuredViewer fViewerInFocus;
  private ListenerList fSelectionChangedListeners;
  private ListenerList fPostSelectionChangedListeners;

  /**
   * @param viewers All viewers that can provide a selection
   * @param viewerInFocus the viewer currently in focus or <code>null</code>
   */
  public SelectionProviderMediator(StructuredViewer[] viewers, StructuredViewer viewerInFocus) {
    Assert.isNotNull(viewers);
    fViewers = viewers;
    InternalListener listener = new InternalListener();
    fSelectionChangedListeners = new ListenerList();
    fPostSelectionChangedListeners = new ListenerList();
    fViewerInFocus = viewerInFocus;

    for (int i = 0; i < fViewers.length; i++) {
      StructuredViewer viewer = fViewers[i];
      viewer.addSelectionChangedListener(listener);
      viewer.addPostSelectionChangedListener(new InternalPostSelectionListener());
      Control control = viewer.getControl();
      control.addFocusListener(listener);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IPostSelectionProvider# addPostSelectionChangedListener
   * (org.eclipse.jface.viewers.ISelectionChangedListener)
   */
  @Override
  public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
    fPostSelectionChangedListeners.add(listener);
  }

  /*
   * @see ISelectionProvider#addSelectionChangedListener
   */
  @Override
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    fSelectionChangedListeners.add(listener);
  }

  /*
   * @see ISelectionProvider#getSelection
   */
  @Override
  public ISelection getSelection() {
    if (fViewerInFocus != null) {
      return fViewerInFocus.getSelection();
    }
    return StructuredSelection.EMPTY;
  }

  /**
   * Returns the viewer in focus or null if no viewer has the focus
   */
  public StructuredViewer getViewerInFocus() {
    return fViewerInFocus;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IPostSelectionProvider# removePostSelectionChangedListener
   * (org.eclipse.jface.viewers.ISelectionChangedListener)
   */
  @Override
  public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
    fPostSelectionChangedListeners.remove(listener);
  }

  /*
   * @see ISelectionProvider#removeSelectionChangedListener
   */
  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    fSelectionChangedListeners.remove(listener);
  }

  /*
   * @see ISelectionProvider#setSelection
   */
  @Override
  public void setSelection(ISelection selection) {
    if (fViewerInFocus != null) {
      fViewerInFocus.setSelection(selection);
    }
  }

  public void setSelection(ISelection selection, boolean reveal) {
    if (fViewerInFocus != null) {
      fViewerInFocus.setSelection(selection, reveal);
    }
  }

  final void doPostSelectionChanged(SelectionChangedEvent event) {
    ISelectionProvider provider = event.getSelectionProvider();
    if (provider == fViewerInFocus) {
      firePostSelectionChanged();
    }
  }

  final void doSelectionChanged(SelectionChangedEvent event) {
    ISelectionProvider provider = event.getSelectionProvider();
    if (provider == fViewerInFocus) {
      fireSelectionChanged();
    }
  }

  final void propagateFocusChanged(StructuredViewer viewer) {
    if (viewer != fViewerInFocus) { // OK to compare by identity
      fViewerInFocus = viewer;
      fireSelectionChanged();
      firePostSelectionChanged();
    }
  }

  private void doFocusChanged(Widget control) {
    for (int i = 0; i < fViewers.length; i++) {
      if (fViewers[i].getControl() == control) {
        propagateFocusChanged(fViewers[i]);
        return;
      }
    }
  }

  private void firePostSelectionChanged() {
    if (fPostSelectionChangedListeners != null) {
      SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());

      Object[] listeners = fPostSelectionChangedListeners.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
        listener.selectionChanged(event);
      }
    }
  }

  private void fireSelectionChanged() {
    if (fSelectionChangedListeners != null) {
      SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());

      Object[] listeners = fSelectionChangedListeners.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
        listener.selectionChanged(event);
      }
    }
  }
}
