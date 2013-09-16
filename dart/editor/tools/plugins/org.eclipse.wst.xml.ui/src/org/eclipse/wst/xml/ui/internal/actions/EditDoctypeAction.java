/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.actions;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.document.DocumentImpl;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocumentType;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.dialogs.EditDoctypeDialog;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * EditDoctypeAction
 */
public class EditDoctypeAction extends NodeAction {
  protected DocumentType doctype;
  protected Document document;
  protected IStructuredModel model;
  protected String resourceLocation;
  protected String title;

  /**
   * This constructor is used to create a new doctype.
   */
  public EditDoctypeAction(IStructuredModel model, Document document, String resourceLocation,
      String title) {
    setText(title);
    this.model = model;
    this.document = document;
    this.resourceLocation = resourceLocation;
    this.title = title;
  }

  /**
   * This constructor is used to edit an exisitng doctype.
   */
  public EditDoctypeAction(IStructuredModel model, DocumentType doctype, String resourceLocation,
      String title) {
    setText(title);
    this.model = model;
    this.doctype = doctype;
    this.resourceLocation = resourceLocation;
    this.title = title;
  }

  protected DocumentType createDoctype(EditDoctypeDialog dialog, Document document) {
    DocumentType result = null;
    if (document instanceof DocumentImpl) {
      IDOMDocument documentImpl = (IDOMDocument) document;
      IDOMDocumentType doctypeImpl = (IDOMDocumentType) documentImpl.createDoctype(dialog.getName());
      doctypeImpl.setPublicId(dialog.getPublicId());
      doctypeImpl.setSystemId(dialog.getSystemId());
      result = doctypeImpl;
    }
    return result;
  }

  private Display getDisplay() {

    return PlatformUI.getWorkbench().getDisplay();
  }

  protected String getRootElementName(Document document) {
    Element rootElement = null;
    NodeList nodeList = document.getChildNodes();
    int nodeListLength = nodeList.getLength();
    for (int i = 0; i < nodeListLength; i++) {
      Node childNode = nodeList.item(i);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        rootElement = (Element) childNode;
        break;
      }
    }
    return rootElement != null ? rootElement.getNodeName()
        : XMLUIMessages._UI_LABEL_ROOT_ELEMENT_VALUE;
  }

  public String getUndoDescription() {
    return title;
  }

  protected void insertDoctype(DocumentType doctype, Document document) {
    Node refChild = null;
    NodeList nodeList = document.getChildNodes();
    int nodeListLength = nodeList.getLength();
    for (int i = 0; i < nodeListLength; i++) {
      Node childNode = nodeList.item(i);
      if ((childNode.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)
          || (childNode.getNodeType() == Node.COMMENT_NODE)) {
        // continue on to the nextNode
      } else {
        refChild = childNode;
        break;
      }
    }

    document.insertBefore(doctype, refChild);
    // manager.reformat(doctype, false);
  }

  public void run() {
    Shell shell = getDisplay().getActiveShell();
    if (validateEdit(model, shell)) {
      model.beginRecording(this, getUndoDescription());
      // Shell shell =
      // XMLCommonUIPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getShell();
      EditDoctypeDialog dialog = showEditDoctypeDialog(shell);

      if (dialog.getReturnCode() == Window.OK) {
        if (doctype != null) {
          updateDoctype(dialog, doctype);
        } else if (document != null) {
          DocumentType doctype = createDoctype(dialog, document);
          if (doctype != null) {
            insertDoctype(doctype, document);
          }
        }
      }
      model.endRecording(this);
    }
  }

  protected EditDoctypeDialog showEditDoctypeDialog(Shell shell) {
    EditDoctypeDialog dialog = null;

    if (doctype != null) {
      dialog = new EditDoctypeDialog(shell, doctype);
      if (title == null) {
        title = XMLUIMessages._UI_LABEL_EDIT_DOCTYPE;
      }
    } else if (document != null) {
      String rootElementName = getRootElementName(document);
      dialog = new EditDoctypeDialog(shell, rootElementName, "", rootElementName + ".dtd"); //$NON-NLS-1$ //$NON-NLS-2$
      if (title == null) {
        title = XMLUIMessages._UI_MENU_ADD_DTD_INFORMATION_TITLE;
      }
    }

    dialog.setComputeSystemId((doctype == null) || (doctype.getSystemId() == null)
        || (doctype.getSystemId().trim().length() == 0));

    dialog.setErrorChecking(false);// !model.getType().equals(IStructuredModel.HTML));
    dialog.create();
    dialog.getShell().setText(title);
    dialog.setBlockOnOpen(true);
    dialog.setResourceLocation(new Path(resourceLocation));
    dialog.open();

    return dialog;
  }

  protected void updateDoctype(EditDoctypeDialog dialog, DocumentType doctype) {
    if (doctype instanceof IDOMDocumentType) {
      IDOMDocumentType doctypeImpl = (IDOMDocumentType) doctype;
      if (doctypeImpl.getName().equals(dialog.getName())) {
        doctypeImpl.setPublicId(dialog.getPublicId());
        doctypeImpl.setSystemId(dialog.getSystemId());
      } else {
        // we need to create a new one and remove the old
        //                  
        Document document = doctype.getOwnerDocument();
        DocumentType newDoctype = createDoctype(dialog, document);
        document.insertBefore(newDoctype, doctype);
        document.removeChild(doctype);
        // manager.reformat(newDoctype, false);
      }
    }
  }
}
