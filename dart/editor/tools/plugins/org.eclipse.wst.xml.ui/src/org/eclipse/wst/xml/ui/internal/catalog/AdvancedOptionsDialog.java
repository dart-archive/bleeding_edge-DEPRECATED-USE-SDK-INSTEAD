/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.wst.common.ui.internal.dialogs.SelectSingleFileDialog;
import org.eclipse.wst.xml.core.internal.catalog.CatalogSet;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

public class AdvancedOptionsDialog extends Dialog {
  protected ICatalog workingUserCatalog;

  public AdvancedOptionsDialog(Shell parentShell, ICatalog workingUserCatalog) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.workingUserCatalog = workingUserCatalog;
  }

  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    // WorkbenchHelp.setHelp(dialogArea, new
    // ControlContextComputer(dialogArea,
    // XMLBuilderContextIds.XMLP_PROJECT_DIALOG));

    Composite composite = new Composite(dialogArea, SWT.NONE);
    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label label = new Label(composite, SWT.NONE);
    label.setText(XMLCatalogMessages.UI_LABEL_DIALOG_DESCRIPTION);

    Composite buttonComposite = new Composite(composite, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    buttonComposite.setLayout(gridLayout);
    buttonComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Composite placeHolder = new Composite(buttonComposite, SWT.NONE);
    placeHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Composite buttonGroup = new Composite(buttonComposite, SWT.NONE);
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    gridLayout.makeColumnsEqualWidth = true;
    buttonGroup.setLayout(gridLayout);
    buttonGroup.setLayoutData(createGridData());

    Button importButton = new Button(buttonGroup, SWT.PUSH | SWT.CENTER);
    importButton.setText("  " + XMLCatalogMessages.UI_BUTTON_IMPORT + "  "); //$NON-NLS-1$ //$NON-NLS-2$

    Button exportButton = new Button(buttonGroup, SWT.PUSH | SWT.CENTER);
    exportButton.setText("  " + XMLCatalogMessages.UI_BUTTON_EXPORT + "  "); //$NON-NLS-1$ //$NON-NLS-2$

    placeHolder = new Composite(buttonComposite, SWT.NONE);
    placeHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // add importButton listener
    SelectionListener importButtonSelectionListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        invokeImportDialog();
      }
    };
    importButton.addSelectionListener(importButtonSelectionListener);

    // add exportButton listener
    SelectionListener exportButtonSelectionListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        invokeExportDialog();
      }
    };
    exportButton.addSelectionListener(exportButtonSelectionListener);

    return dialogArea;
  }

  protected GridData createGridData() {
    GridData gd = new GridData(GridData.CENTER);
    gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_CENTER;
    gd.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
    return gd;
  }

  protected void invokeImportDialog() {
    SelectSingleFileDialog dialog = new SelectSingleFileDialog(getShell(), null, true);
    String[] extensions = {".xmlcatalog", ".xml"}; //$NON-NLS-1$ //$NON-NLS-2$
    dialog.addFilterExtensions(extensions);
    dialog.create();
    dialog.getShell().setText(XMLCatalogMessages.UI_LABEL_IMPORT_DIALOG_TITLE);
    dialog.setTitle(XMLCatalogMessages.UI_LABEL_IMPORT_DIALOG_HEADING);
    dialog.setMessage(XMLCatalogMessages.UI_LABEL_IMPORT_DIALOG_MESSAGE);
    dialog.setBlockOnOpen(true);
    int rc = dialog.open();
    if (rc == Window.OK) {
      IFile file = dialog.getFile();
      if (file != null) {
        String fileName = file.getLocation().toFile().toURI().toString();
        try {
          CatalogSet tempResourceSet = new CatalogSet();
          ICatalog newCatalog = tempResourceSet.lookupOrCreateCatalog("temp", fileName); //$NON-NLS-1$
          workingUserCatalog.addEntriesFromCatalog(newCatalog);
        } catch (Exception e) {
          // TODO... give error message
        }
      }
      close();
    }
  }

  protected void invokeExportDialog() {
    IPath originalFilePath = null;
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    if (projects.length > 0) {
      originalFilePath = projects[0].getFullPath().append(".xmlcatalog"); //$NON-NLS-1$
    }

    SaveAsDialog dialog = new SaveAsDialog(getShell());
    if (originalFilePath != null) {
      IFile originalFile = ResourcesPlugin.getWorkspace().getRoot().getFile(originalFilePath);
      dialog.setOriginalFile(originalFile);
    }
    dialog.create();

    dialog.getShell().setText(XMLCatalogMessages.UI_LABEL_EXPORT_DIALOG_TITLE);
    dialog.setTitle(XMLCatalogMessages.UI_LABEL_EXPORT_DIALOG_HEADING);
    dialog.setMessage(XMLCatalogMessages.UI_LABEL_EXPORT_DIALOG_MESSAGE);

    dialog.setBlockOnOpen(true);
    int rc = dialog.open();
    if (rc == Window.OK) {
      IPath path = dialog.getResult();
      if (path != null) {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        String fileName = file.getLocation().toFile().toURI().toString();

        // here we save the catalog entries to the selected file
        try {
          createFileIfRequired(file);
          workingUserCatalog.setLocation(fileName);
          workingUserCatalog.save();
        } catch (Exception ex) {
          try {
            String title = XMLCatalogMessages.UI_LABEL_CATALOG_SAVE_ERROR;
            String briefMessage = XMLCatalogMessages.UI_LABEL_CATALOG_COULD_NOT_BE_SAVED;
            String reason = file.isReadOnly() ? NLS.bind(
                XMLCatalogMessages.UI_LABEL_FILE_IS_READ_ONLY, fileName) : NLS.bind(
                XMLCatalogMessages.ERROR_SAVING_FILE, fileName);
            String details = NLS.bind(XMLCatalogMessages.ERROR_SAVING_FILE, fileName);

            ErrorDialog.openError(Display.getCurrent().getActiveShell(), title, briefMessage,
                createStatus(reason, details));
          } catch (Exception ex2) {
          }
        }
        close();
      }
    }
  }

  // TODO... This was copied from WindowUtility. Is there an easier way to
  // create a status object?
  // If not, we should open an eclipse bug or add a similar utility to
  // baseExtensionsUI.
  //
  private static IStatus createStatus(String reason, String msg) {
    String pluginId = XMLUIPlugin.getDefault().getBundle().getSymbolicName();
    MultiStatus multiStatus = new MultiStatus(pluginId, 0, reason, null);
    Status status = new Status(IStatus.ERROR, pluginId, 0, msg, null);
    multiStatus.add(status);
    return multiStatus;
  }

  protected void createFileIfRequired(IFile file) {
    try {
      if ((file != null) && !file.exists()) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        file.create(inputStream, true, null);
        // createEmptyXMLCatalog(file);
      }
    } catch (Exception e) {
    }
  }
}
