/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Derived from org.eclipse.search.internal.ui.ResourceTransferDragAdapter
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.basecode;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.part.ResourceTransfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A drag adapter that transfers the current selection as </code> IResource</code>. Only those
 * elements in the selection are part of the transfer which can be converted into an <code>IResource
 * </code>.
 */
public class ResourceTransferDragAdapter extends DragSourceAdapter implements
    TransferDragSourceListener {

  private ISelectionProvider fProvider;

  /**
   * Creates a new ResourceTransferDragAdapter for the given selection provider.
   * 
   * @param provider the selection provider to access the viewer's selection
   */
  public ResourceTransferDragAdapter(ISelectionProvider provider) {
    fProvider = provider;
    Assert.isNotNull(fProvider);
  }

  public Transfer getTransfer() {
    return ResourceTransfer.getInstance();
  }

  public void dragStart(DragSourceEvent event) {
    event.doit = convertSelection().size() > 0;
  }

  public void dragSetData(DragSourceEvent event) {
    List resources = convertSelection();
    event.data = resources.toArray(new IResource[resources.size()]);
  }

  public void dragFinished(DragSourceEvent event) {
    if (!event.doit)
      return;
  }

  private List convertSelection() {
    ISelection s = fProvider.getSelection();
    if (!(s instanceof IStructuredSelection))
      return Collections.EMPTY_LIST;
    IStructuredSelection selection = (IStructuredSelection) s;
    List result = new ArrayList(selection.size());
    for (Iterator iter = selection.iterator(); iter.hasNext();) {
      Object element = iter.next();
      if (element instanceof IResource) {
        result.add(element);
      }
    }
    return result;
  }
}
