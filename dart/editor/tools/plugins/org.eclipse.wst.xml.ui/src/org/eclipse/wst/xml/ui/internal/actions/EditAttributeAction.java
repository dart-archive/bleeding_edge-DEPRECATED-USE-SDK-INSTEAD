/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.dialogs.EditAttributeDialog;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EditAttributeAction extends NodeAction {
  protected static ImageDescriptor imageDescriptor;

  public static ImageDescriptor createImageDescriptor() {
    if (imageDescriptor == null) {
      imageDescriptor = XMLEditorPluginImageHelper.getInstance().getImageDescriptor(
          XMLEditorPluginImages.IMG_OBJ_ATTRIBUTE);
    }
    return imageDescriptor;
  }

  protected Attr attr;
  protected AbstractNodeActionManager manager;
  protected Element ownerElement;
  protected String title;

  public EditAttributeAction(AbstractNodeActionManager manager, Element ownerElement, Attr attr,
      String actionLabel, String title) {
    this.manager = manager;
    this.ownerElement = ownerElement;
    this.attr = attr;
    this.title = title;
    setText(actionLabel);
    // assume if attr is null then this is an 'Add' that requires action
    // an icons... otherwise this is an edit
    if (attr == null) {
      setImageDescriptor(createImageDescriptor());
    }
  }

  public String getUndoDescription() {
    return title;
  }

  public void run() {
    Shell shell = XMLUIPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getShell();
    if (validateEdit(manager.getModel(), shell)) {
      manager.beginNodeAction(this);
      EditAttributeDialog dialog = new EditAttributeDialog(shell, ownerElement, attr);
      dialog.create();
      dialog.getShell().setText(title);
      dialog.setBlockOnOpen(true);
      dialog.open();

      if (dialog.getReturnCode() == Window.OK) {
        if (attr != null) {
          ownerElement.removeAttributeNode(attr);
        }
        Document document = ownerElement.getOwnerDocument();
        Attr newAttribute = document.createAttribute(dialog.getAttributeName());
        newAttribute.setValue(dialog.getAttributeValue());
        ownerElement.setAttributeNode(newAttribute);
        manager.setViewerSelection(newAttribute);
      }
      manager.endNodeAction(this);
    }
  }
}
