/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.dialogs;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/**
 * The Open Folder Dialog
 */
public class OpenFolderDialog extends TitleAreaDialog {

  public static final String DIALOGSTORE_LAST_DIR = DartUI.class.getPackage().getName()
      + ".last.dir"; //$NON-NLS-1$

  private Text text;
  private Button runPubButton;

  private boolean runpub = false;
  private String folderLocation;

  public OpenFolderDialog(Shell parentShell) {
    super(parentShell);
  }

  public String getFolderLocation() {
    if (folderLocation.startsWith("~")) {
      String home = System.getProperty("user.home");
      return new File(new File(home), folderLocation.substring(1)).toString();
    }
    return folderLocation;
  }

  public boolean isRunpub() {
    return runpub;
  }

  @Override
  protected void configureShell(Shell newShell) {
    newShell.setText(DialogMessages.OpenFolderDialog_title);
    super.configureShell(newShell);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    getButton(IDialogConstants.OK_ID).setEnabled(false);
    return control;
  }

  @Override
  protected Control createDialogArea(Composite parent) {

    Composite composite = (Composite) super.createDialogArea(parent);
    setTitle(DialogMessages.OpenFolderDialog_message);
    setMessage(DialogMessages.OpenFolderDialog_description);
    createFolderBrowseRow(composite);
    if (!DartCore.isWindowsXp()) {
      createRunPubMessage(parent);
    }
    return composite;
  }

  @Override
  protected void okPressed() {
    folderLocation = text.getText().trim();
    if (runPubButton != null) {
      runpub = runPubButton.getSelection();
    }
    super.okPressed();
  }

  private void createFolderBrowseRow(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout(3, false);
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    panel.setLayout(layout);
    panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    panel.setFont(parent.getFont());

    Label label = new Label(panel, SWT.NONE);
    label.setText(DialogMessages.OpenFolderDialog_label);

    text = new Text(panel, SWT.BORDER);
    text.setFocus();
    text.setLayoutData(new GridData(400, SWT.DEFAULT));
    text.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        folderLocation = text.getText();
        Button okButton = getButton(Window.OK);
        if (okButton != null && !okButton.isDisposed()) {
          boolean nonWhitespaceFound = false;
          String characters = getFolderLocation();
          for (int i = 0; !nonWhitespaceFound && i < characters.length(); i++) {
            if (!Character.isWhitespace(characters.charAt(i))) {
              nonWhitespaceFound = true;
            }
          }
          okButton.setEnabled(nonWhitespaceFound);
          if (!DartCore.isWindowsXp()) {
            setCheckBoxState();
          }
        }
      }
    });

    Button browseButton = new Button(panel, SWT.PUSH);
    browseButton.setText(DialogMessages.OpenFolderDialog_browse);
    setButtonLayoutData(browseButton);
    GridData data = (GridData) browseButton.getLayoutData();
    data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
    browseButton.setLayoutData(data);
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SHEET);
        dialog.setText(DialogMessages.OpenFolderDialog_title);
        dialog.setMessage(DialogMessages.OpenFolderDialog_dialogMessage);
        dialog.setFilterPath(getInitialBrowsePath());
        String dir = dialog.open();
        dir = TextProcessor.process(dir);
        if (dir != null) {
          text.setText(dir);
        }
      }

    });
  }

  private void createRunPubMessage(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    panel.setLayout(layout);
    panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    panel.setFont(parent.getFont());

    runPubButton = new Button(panel, SWT.CHECK);
    runPubButton.setText(DialogMessages.OpenFolderDialog_rubPubMessage);
    runPubButton.setSelection(true);
  }

  private String getInitialBrowsePath() {
    IDialogSettings dialogSettings = DartToolsPlugin.getDefault().getDialogSettings();
    return dialogSettings.get(DIALOGSTORE_LAST_DIR);
  }

  private void setCheckBoxState() {
    File file = new File(getFolderLocation(), DartCore.PUBSPEC_FILE_NAME);
    if (!file.exists()) {
      runPubButton.setSelection(false);
      runPubButton.setEnabled(false);
    } else {
      runPubButton.setSelection(true);
      runPubButton.setEnabled(true);
    }
  }

}
