/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dialogs;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolver;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverPlugin;
import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.ContentModelManager;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.util.XMLCommonUIContextIds;

public class EditNamespaceInfoDialog extends Dialog {

  public static EditNamespaceInfoDialog invokeDialog(Shell shell, String title, NamespaceInfo info,
      IPath resourceLocation) {
    EditNamespaceInfoDialog dialog = new EditNamespaceInfoDialog(shell, info);
    dialog.create();
    dialog.getShell().setText(title);
    dialog.setBlockOnOpen(true);
    dialog.setResourceLocation(resourceLocation);
    dialog.open();
    return dialog;
  }

  protected Button browseButton;
  protected String errorMessage;

  protected Label errorMessageLabel;
  protected NamespaceInfo fInfo;
  protected Text locationHintField;

  protected Button okButton;
  protected Text prefixField;
  protected IPath resourceLocation;
  protected Text uriField;

  public EditNamespaceInfoDialog(Shell parentShell, NamespaceInfo info) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.fInfo = info;
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      fInfo.uri = uriField.getText();
      fInfo.prefix = prefixField.getText();
      fInfo.locationHint = locationHintField.getText();
    }
    super.buttonPressed(buttonId);
  }

  protected void computeErrorMessage() {
    errorMessage = null;
  }

  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    okButton.setEnabled(false);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    updateWidgets();
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogsubArea = (Composite) super.createDialogArea(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(dialogsubArea,
        XMLCommonUIContextIds.XCUI_NAMESPACE_DIALOG);

    Composite composite = new Composite(dialogsubArea, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    layout.marginWidth = 0;
    composite.setLayout(layout);

    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 350;
    composite.setLayoutData(gd);

    ModifyListener modifyListener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateWidgets();
      }
    };

    // row 1
    //
    Label uriLabel = new Label(composite, SWT.NONE);
    uriLabel.setText(XMLUIMessages._UI_LABEL_NAMESPACE_NAME_COLON);

    uriField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.grabExcessHorizontalSpace = true;
    uriField.setLayoutData(gd);
    uriField.setText(getDisplayValue(fInfo.uri));
    uriField.addModifyListener(modifyListener);
    uriField.setEnabled(fInfo.getProperty("uri-readOnly") == null); //$NON-NLS-1$

    // never read
    Label placeHolder1 = new Label(composite, SWT.NONE);
    placeHolder1.setText(""); //$NON-NLS-1$

    // row 2
    //
    Label prefixLabel = new Label(composite, SWT.NONE);
    prefixLabel.setText(XMLUIMessages._UI_LABEL_PREFIX_COLON);

    prefixField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.grabExcessHorizontalSpace = true;
    prefixField.setLayoutData(gd);
    prefixField.setText(getDisplayValue(fInfo.prefix));
    prefixField.addModifyListener(modifyListener);
    prefixField.setEnabled(fInfo.getProperty("prefix-readOnly") == null); //$NON-NLS-1$

    // never read
    Label placeHolder2 = new Label(composite, SWT.NONE);
    placeHolder2.setText(""); //$NON-NLS-1$

    // row 3
    //
    Label locationHintLabel = new Label(composite, SWT.NONE);
    locationHintLabel.setText(XMLUIMessages._UI_LABEL_LOCATION_HINT_COLON);

    locationHintField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.grabExcessHorizontalSpace = true;
    locationHintField.setLayoutData(gd);
    locationHintField.setText(getDisplayValue(fInfo.locationHint));
    locationHintField.addModifyListener(modifyListener);
    locationHintField.setEnabled(fInfo.getProperty("locationHint-readOnly") == null); //$NON-NLS-1$

    SelectionListener selectionListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        performBrowse();
      }
    };

    browseButton = new Button(composite, SWT.NONE);
    browseButton.setText(XMLUIMessages._UI_LABEL_BROWSE);
    browseButton.addSelectionListener(selectionListener);
    browseButton.setEnabled(locationHintField.getEnabled());

    // error message
    errorMessageLabel = new Label(dialogsubArea, SWT.NONE);
    errorMessageLabel.setText(XMLUIMessages.error_message_goes_here);
    errorMessageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    Color color = new Color(errorMessageLabel.getDisplay(), 200, 0, 0);
    errorMessageLabel.setForeground(color);

    return dialogsubArea;
  }

  protected String getDisplayValue(String string) {
    return string != null ? string : ""; //$NON-NLS-1$
  }

  protected void performBrowse() {
    String[] extensions = {".xsd"}; //$NON-NLS-1$
    SelectFileOrXMLCatalogIdDialog dialog = new SelectFileOrXMLCatalogIdDialog(getShell(),
        extensions);
    dialog.create();
    dialog.getShell().setText(XMLUIMessages._UI_LABEL_SELECT_FILE);
    dialog.setBlockOnOpen(true);
    dialog.open();

    if (dialog.getReturnCode() == Window.OK) {
      String grammarURI = null;
      IFile file = dialog.getFile();
      String id = dialog.getId();
      if (file != null) {
        String uri = null;
        if (resourceLocation != null) {
          IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(resourceLocation);
          if (resource != null) {
            IPath location = resource.getLocation();
            if (location != null) {
              uri = URIHelper.getRelativeURI(file.getLocation(), location);
            }
          } else {
            uri = URIHelper.getRelativeURI(file.getLocation(), resourceLocation);
          }
          grammarURI = file.getLocation().toOSString();
        } else {
          uri = file.getLocation().toOSString();
          grammarURI = uri;
        }
        locationHintField.setText(uri);
      } else if (id != null) {
        locationHintField.setText(id);
        URIResolver resolver = URIResolverPlugin.createResolver();
        grammarURI = resolver.resolve(null, id, id);
      }

      CMDocument document = ContentModelManager.getInstance().createCMDocument(
          URIHelper.getURIForFilePath(grammarURI), "xsd"); //$NON-NLS-1$
      if (document != null) {
        List namespaceInfoList = (List) document.getProperty("http://org.eclipse.wst/cm/properties/namespaceInfo"); //$NON-NLS-1$
        if (namespaceInfoList != null) {
          NamespaceInfo info = (NamespaceInfo) namespaceInfoList.get(0);
          if (info != null) {
            if ((uriField.getText().trim().length() == 0) && (info.uri != null)) {
              uriField.setText(info.uri);
            }
            if ((prefixField.getText().trim().length() == 0) && (info.prefix != null)) {
              prefixField.setText(info.prefix);
            }
          }
        }
      }
    }
  }

  public void setResourceLocation(IPath path) {
    resourceLocation = path;
  }

  protected void updateErrorMessageLabel() {
    errorMessageLabel.setText(errorMessage != null ? errorMessage : ""); //$NON-NLS-1$
  }

  protected void updateOKButtonState() {
    if (okButton != null) {
      if ((uriField.getText().trim().length() == 0) && (prefixField.getText().trim().length() == 0)
          && (locationHintField.getText().trim().length() == 0)) {
        okButton.setEnabled(false);
      } else {
        okButton.setEnabled(errorMessage == null);
      }
    }
  }

  protected void updateWidgets() {
    computeErrorMessage();
    updateErrorMessageLabel();
    updateOKButtonState();
  }
}
