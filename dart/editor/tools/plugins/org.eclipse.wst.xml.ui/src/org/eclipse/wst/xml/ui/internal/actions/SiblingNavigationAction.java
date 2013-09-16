/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.actions;

import java.util.ResourceBundle;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Provides navigation to next/previous DOM sibling Nodes
 * 
 * @author nitin
 */
class SiblingNavigationAction extends TextEditorAction {

  private boolean fForward;

  /**
   * @param bundle
   * @param prefix
   * @param editor
   */
  SiblingNavigationAction(ResourceBundle bundle, String prefix, ITextEditor editor, boolean forward) {
    super(bundle, prefix, editor);
    fForward = forward;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
   */
  public void runWithEvent(Event event) {
    super.runWithEvent(event);
    if (getTextEditor() == null)
      return;

    ISelection selection = getTextEditor().getSelectionProvider().getSelection();
    if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
      Object o = ((IStructuredSelection) selection).getFirstElement();
      if (o instanceof Node) {
        Node sibling = null;

        if (((Node) o).getNodeType() == Node.ATTRIBUTE_NODE) {
          o = ((Attr) o).getOwnerElement();
        }
        if (fForward) {
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
          getTextEditor().getSelectionProvider().setSelection(new StructuredSelection(sibling));
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.TextEditorAction#update()
   */
  public void update() {

  }
}
