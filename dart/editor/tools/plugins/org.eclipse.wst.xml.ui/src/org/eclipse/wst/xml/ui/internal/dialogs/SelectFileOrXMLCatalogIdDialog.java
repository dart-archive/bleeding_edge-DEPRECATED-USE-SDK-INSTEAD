/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class SelectFileOrXMLCatalogIdDialog extends Dialog {
  protected int catalogEntryType;
  protected String[] extensions;
  protected IFile file;
  protected String id;
  protected Button okButton;
  protected SelectFileOrXMLCatalogIdPanel panel;

  public SelectFileOrXMLCatalogIdDialog(Shell parentShell, String[] extensions) {
    this(parentShell, extensions, 0);
  }

  public SelectFileOrXMLCatalogIdDialog(Shell parentShell, String[] extensions, int catalogEntryType) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.extensions = extensions;
    this.catalogEntryType = catalogEntryType;
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      file = panel.getFile();
      id = panel.getXMLCatalogId();
    }
    super.buttonPressed(buttonId);
  }

  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    okButton.setEnabled(false);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);

    panel = new SelectFileOrXMLCatalogIdPanel(dialogArea);
    panel.setCatalogEntryType(catalogEntryType);
    panel.setFilterExtensions(extensions);
    panel.setVisibleHelper(true);
    SelectFileOrXMLCatalogIdPanel.Listener listener = new SelectFileOrXMLCatalogIdPanel.Listener() {
      public void completionStateChanged() {
        updateButtonState();
      }
    };
    panel.setListener(listener);

    return dialogArea;
  }

  public IFile getFile() {
    return file;
  }

  public String getId() {
    return id;
  }

  protected void updateButtonState() {
    okButton.setEnabled((panel.getFile() != null) || (panel.getXMLCatalogId() != null));
  }
}
