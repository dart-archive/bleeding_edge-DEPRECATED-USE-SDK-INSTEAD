/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.actions;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.dialogs.EditProcessingInstructionDialog;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

/**
 * EditProcessingInstructionAction
 */
public class EditProcessingInstructionAction extends NodeAction {
  protected Node childRef;
  protected AbstractNodeActionManager manager;
  protected Node parent;
  protected ProcessingInstruction pi;
  protected String title;

  /**
   * This constructor is used to add a new ProcessingInstruction
   */
  public EditProcessingInstructionAction(AbstractNodeActionManager manager, Node parent,
      Node childRef, String actionLabel, String title) {
    setText(actionLabel);
    this.manager = manager;
    this.parent = parent;
    this.childRef = childRef;
    this.title = title;
  }

  /**
   * This constructor is used to edit a ProcessingInstruction
   */
  public EditProcessingInstructionAction(AbstractNodeActionManager manager,
      ProcessingInstruction pi, String actionLabel, String title) {
    setText(actionLabel);
    this.manager = manager;
    this.pi = pi;
    this.parent = pi.getParentNode();
    this.title = title;
  }

  public String getUndoDescription() {
    return title;
  }

  public void run() {
    Shell shell = XMLUIPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getShell();
    if (validateEdit(manager.getModel(), shell)) {
      manager.beginNodeAction(this);

      EditProcessingInstructionDialog dialog = null;
      if (pi != null) {
        dialog = new EditProcessingInstructionDialog(shell, pi);
      } else {
        dialog = new EditProcessingInstructionDialog(shell, XMLUIMessages._UI_PI_TARGET_VALUE,
            XMLUIMessages._UI_PI_DATA_VALUE);
      }

      dialog.create();
      dialog.getShell().setText(title);
      dialog.setBlockOnOpen(true);
      dialog.open();

      if (dialog.getReturnCode() == Window.OK) {
        if (pi != null) {
          childRef = pi;
        }

        Document document = parent.getNodeType() == Node.DOCUMENT_NODE ? (Document) parent
            : parent.getOwnerDocument();
        Node newNode = document.createProcessingInstruction(dialog.getTarget(), dialog.getData());
        parent.insertBefore(newNode, childRef);

        if (pi != null) {
          parent.removeChild(pi);
        }

        manager.reformat(newNode, false);
        manager.setViewerSelection(newNode);
      }
      manager.endNodeAction(this);
    }
  }
}
