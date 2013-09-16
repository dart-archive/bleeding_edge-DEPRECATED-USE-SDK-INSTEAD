/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.quickoutline;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.wst.sse.ui.IContentSelectionProvider;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

public class XMLContentSelectionProvider implements IContentSelectionProvider {

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
    if (object instanceof Node) {
      Node node = (Node) object;
      short nodeType = node.getNodeType();
      // replace attribute node in selection with its parent
      if (nodeType == Node.ATTRIBUTE_NODE) {
        node = ((Attr) node).getOwnerElement();
      }
      // anything else not visible, replace with parent node
      else if (nodeType == Node.TEXT_NODE) {
        node = node.getParentNode();
      }
      return node;
    }
    return object;
  }

}
