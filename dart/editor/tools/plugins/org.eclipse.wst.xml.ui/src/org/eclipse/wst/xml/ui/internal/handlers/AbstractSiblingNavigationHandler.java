/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Provides navigation to next/previous DOM sibling Nodes
 */
abstract public class AbstractSiblingNavigationHandler extends AbstractHandler {

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
      if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
        Object o = ((IStructuredSelection) selection).getFirstElement();
        if (o instanceof Node) {
          Node sibling = null;

          if (((Node) o).getNodeType() == Node.ATTRIBUTE_NODE) {
            o = ((Attr) o).getOwnerElement();
          }
          if (moveForward()) {
            sibling = ((Node) o).getNextSibling();
            while (sibling != null && sibling.getNodeType() == Node.TEXT_NODE
                && sibling.getNodeValue().trim().length() == 0) {
              sibling = sibling.getNextSibling();
            }
            if (sibling == null) {
              sibling = ((Node) o).getParentNode().getFirstChild();
              while (sibling != null && sibling.getNodeType() == Node.TEXT_NODE
                  && sibling.getNodeValue().trim().length() == 0) {
                sibling = sibling.getNextSibling();
              }
            }
          } else {
            sibling = ((Node) o).getPreviousSibling();
            while (sibling != null && sibling.getNodeType() == Node.TEXT_NODE
                && sibling.getNodeValue().trim().length() == 0) {
              sibling = sibling.getPreviousSibling();
            }
            if (sibling == null) {
              sibling = ((Node) o).getParentNode().getLastChild();
              while (sibling != null && sibling.getNodeType() == Node.TEXT_NODE
                  && sibling.getNodeValue().trim().length() == 0) {
                sibling = sibling.getPreviousSibling();
              }
            }
          }

          // The only child is a Text Node, go to the parent Node
          if (((Node) o).getNodeType() == Node.TEXT_NODE && o.equals(sibling)) {
            sibling = ((Node) o).getParentNode();
          }

          if (sibling != null) {
            textEditor.getSelectionProvider().setSelection(new StructuredSelection(sibling));
          }
        }
      }
    }
    return null;
  }

  abstract protected boolean moveForward();
}
