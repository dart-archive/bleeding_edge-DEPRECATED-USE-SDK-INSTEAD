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
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.dialogs.EditElementDialog;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EditElementAction extends NodeAction {

  protected static ImageDescriptor imageDescriptor;

  public static ImageDescriptor createImageDescriptor() {
    if (imageDescriptor == null) {
      imageDescriptor = XMLEditorPluginImageHelper.getInstance().getImageDescriptor(
          XMLEditorPluginImages.IMG_OBJ_ELEMENT);
    }
    return imageDescriptor;
  }

  protected Element element;
  protected int insertionIndex = -1;
  protected AbstractNodeActionManager manager;
  protected Node parent;
  protected String title;

  public EditElementAction(AbstractNodeActionManager manager, Element element, String actionLabel,
      String dialogTitle) {
    this(manager, element.getParentNode(), -1, element, actionLabel, dialogTitle);
  }

  protected EditElementAction(AbstractNodeActionManager manager, Node parent, int index,
      Element element, String actionLabel, String title) {
    this.manager = manager;
    this.parent = parent;
    this.insertionIndex = index;
    this.element = element;
    this.title = title;
    setText(actionLabel);
    if (element == null) {
      setImageDescriptor(createImageDescriptor());
    }
  }

  public EditElementAction(AbstractNodeActionManager manager, Node parent, int index,
      String actionLabel, String title) {
    this(manager, parent, index, null, actionLabel, title);
  }

  public String getUndoDescription() {
    return title;
  }

  public void run() {
    Shell shell = XMLUIPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getShell();
    if (validateEdit(manager.getModel(), shell)) {
      manager.beginNodeAction(this);
      EditElementDialog dialog = new EditElementDialog(shell, element);
      dialog.create();
      dialog.getShell().setText(title);
      dialog.setBlockOnOpen(true);
      dialog.open();

      if (dialog.getReturnCode() == Window.OK) {
        Document document = parent.getNodeType() == Node.DOCUMENT_NODE ? (Document) parent
            : parent.getOwnerDocument();
        if (element != null) {
          // here we need to do a rename... which seems to be quite hard
          // to do :-(
          if (element instanceof IDOMElement) {
            IDOMElement elementImpl = (IDOMElement) element;
            IDOMModel model = elementImpl.getModel();
            String oldName = elementImpl.getNodeName();
            String newName = dialog.getElementName();
            setStructuredDocumentRegionElementName(model,
                elementImpl.getStartStructuredDocumentRegion(), oldName, newName);
            setStructuredDocumentRegionElementName(model,
                elementImpl.getEndStructuredDocumentRegion(), oldName, newName);
          }
        } else {
          Element newElement = document.createElement(dialog.getElementName());
          NodeList nodeList = parent.getChildNodes();
          int nodeListLength = nodeList.getLength();
          Node refChild = (insertionIndex < nodeListLength) && (insertionIndex >= 0)
              ? nodeList.item(insertionIndex) : null;
          parent.insertBefore(newElement, refChild);
          manager.reformat(newElement, false);
          manager.setViewerSelection(newElement);
        }
      }
      manager.endNodeAction(this);
    }
  }

  protected void setStructuredDocumentRegionElementName(IDOMModel model,
      IStructuredDocumentRegion flatNode, String oldName, String newName) {
    if (flatNode != null) {
      String string = flatNode.getText();
      int index = string.indexOf(oldName);
      if (index != -1) {
        index += flatNode.getStart();
        model.getStructuredDocument().replaceText(this, index, oldName.length(), newName);
      }
    }
  }
}
