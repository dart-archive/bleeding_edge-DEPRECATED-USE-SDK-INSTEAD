/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.views.contentoutline;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPrimitiveValue;
import org.eclipse.wst.sse.ui.IContentSelectionProvider;

class CSSContentSelectionProvider implements IContentSelectionProvider {

  public ISelection getSelection(TreeViewer viewer, ISelection selection) {
    ISelection filteredSelection = selection;
    if (selection instanceof IStructuredSelection) {
      Object[] filteredNodes = getFilteredNodes(((IStructuredSelection) selection).toArray());
      filteredSelection = new StructuredSelection(filteredNodes);
    }
    return filteredSelection;
  }

  private Object[] getFilteredNodes(Object[] filteredNodes) {
    for (int i = 0; i < filteredNodes.length; i++) {
      filteredNodes[i] = getFilteredNode(filteredNodes[i]);
    }
    return filteredNodes;
  }

  private Object getFilteredNode(Object object) {
    // If the selection is a primitive value, get the property that contains it */
    if (object instanceof ICSSPrimitiveValue) {
      ICSSPrimitiveValue value = (ICSSPrimitiveValue) object;
      object = value.getParentNode();
    }
    return object;
  }

}
