/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - Initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.ui.internal.UIPlugin;
import org.eclipse.wst.common.ui.internal.viewers.SelectSingleFileView;

public class SelectSingleFileDialog extends TitleAreaDialog {
  protected SelectSingleFileView selectSingleFileView;
  protected Button okButton;
  protected Image image;
  private ImageDescriptor imageDescriptor;

  public SelectSingleFileDialog(Shell parentShell, IStructuredSelection selection,
      boolean isFileMandatory) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    if (selection == null) {
      selection = new StructuredSelection();
    }
    selectSingleFileView = new SelectSingleFileView(selection, isFileMandatory) {
      public void createFilterControl(Composite composite) {
        SelectSingleFileDialog.this.createFilterControl(composite);
      }
    };
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);

    // TODO.. enable context help
    // WorkbenchHelp.setHelp(dialogArea,
    // B2BGUIContextIds.BTBG_SELECT_SINGLE_FILE_DIALOG);

    Composite composite = new Composite(dialogArea, SWT.NONE);
    composite.setLayout(new GridLayout());
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 350;
    gd.heightHint = 350;
    composite.setLayoutData(gd);

    SelectSingleFileView.Listener listener = new SelectSingleFileView.Listener() {
      public void setControlComplete(boolean isComplete) {
        okButton.setEnabled(isComplete);
      }
    };
    selectSingleFileView.setListener(listener);
    selectSingleFileView.createControl(composite);
    return dialogArea;
  }

  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    okButton.setEnabled(false);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  public void create() {
    super.create();
    selectSingleFileView.setVisibleHelper(true);
    image = getSaveAsImage();
    setTitleImage(image);
  }

  /**
   * this image was copied from org.eclipse.ui.ide/icons/full/wizban/ where it is a non-API image,
   * denoted by IDEInternalWorkbenchImages.IMG_DLGBAN_SAVEAS_DLG.
   */
  private Image getSaveAsImage() {
    Image localimage = null;
    try {
      if (imageDescriptor == null) {
        imageDescriptor = UIPlugin.getDefault().getImageDescriptor("icons/saveas_wiz.png");
      }
      localimage = (Image) imageDescriptor.createResource(getContents().getDisplay());
    } catch (DeviceResourceException e) {
      // if image not found
      localimage = (ImageDescriptor.getMissingImageDescriptor()).createImage();
    }
    return localimage;
  }

  public void createFilterCombo(Composite composite) {
  }

  public IFile getFile() {
    return selectSingleFileView.getFile();
  }

  public void addFilterExtensions(String[] filterExtensions) {
    selectSingleFileView.addFilterExtensions(filterExtensions);
  }

  public void addFilterExtensions(String[] filterExtensions, IFile[] excludedFiles) {
    selectSingleFileView.addFilterExtensions(filterExtensions, excludedFiles);
  }

  public void createFilterControl(Composite composite) {
  }

  public boolean close() {
    if (image != null && imageDescriptor != null) {
      imageDescriptor.destroyResource(image);
      image = null;
    }
    return super.close();
  }
}
