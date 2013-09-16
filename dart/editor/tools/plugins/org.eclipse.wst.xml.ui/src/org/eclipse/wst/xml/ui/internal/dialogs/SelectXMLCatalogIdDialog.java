/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.ui.internal.util.XMLCommonUIContextIds;

public class SelectXMLCatalogIdDialog extends Dialog {
  protected String[] extensions;
  protected Button okButton;
  protected SelectXMLCatalogIdPanel panel;
  protected String publicId;
  protected String systemId;

  public SelectXMLCatalogIdDialog(Shell parentShell, String[] extensions) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.extensions = extensions;
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      ISelection selection = panel.getTableViewer().getSelection();
      Object selectedObject = (selection instanceof IStructuredSelection)
          ? ((IStructuredSelection) selection).getFirstElement() : null;

      if (selectedObject instanceof ICatalogEntry) {
        ICatalogEntry mappingInfo = (ICatalogEntry) selectedObject;
        publicId = mappingInfo.getKey();
        systemId = computeDefaultSystemId(mappingInfo);
      }
    }
    super.buttonPressed(buttonId);
  }

  protected String computeDefaultSystemId(ICatalogEntry mappingInfo) {
    String result = mappingInfo.getAttributeValue(ICatalogEntry.ATTR_WEB_URL);
    if ((result == null) && (mappingInfo.getURI() != null)) {
      int index = mappingInfo.getURI().lastIndexOf("/"); //$NON-NLS-1$
      String lastSegment = index != -1 ? mappingInfo.getURI().substring(index + 1)
          : mappingInfo.getURI();
      result = lastSegment;
    }
    return result;
  }

  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    okButton.setEnabled(false);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(dialogArea,
        XMLCommonUIContextIds.XCUI_CATALOG_DIALOG);

    ICatalog xmlCatalog = XMLCorePlugin.getDefault().getDefaultXMLCatalog();
    panel = new SelectXMLCatalogIdPanel(dialogArea, xmlCatalog);

    ISelectionChangedListener listener = new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateButtonState();
      }
    };
    panel.getTableViewer().setFilterExtensions(extensions);
    panel.getTableViewer().addSelectionChangedListener(listener);
    return dialogArea;
  }

  public String getId() {
    return publicId;
  }

  public String getSystemId() {
    return systemId;
  }

  protected void updateButtonState() {
    ISelection selection = panel.getTableViewer().getSelection();
    okButton.setEnabled(!selection.isEmpty());
  }
}
