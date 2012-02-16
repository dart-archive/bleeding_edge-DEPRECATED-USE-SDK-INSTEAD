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
package com.google.dart.tools.ui.wizard;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.StatusUtil;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/**
 * The Show Directory wizard page.
 * 
 * @see ShowDirectoryWizard
 */
public class ShowDirectoryWizardPage extends AbstractDartWizardPage {

  private Text directoryField;

  private String directory;

  protected ShowDirectoryWizardPage() {
    super("newApplicationWizardPage1"); //$NON-NLS-1$
    setTitle(WizardMessages.ShowDirectoryWizardPage_title);
    setDescription(WizardMessages.ShowDirectoryWizardPage_description);
    directory = null;
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);

    GridData gd_composite_1 = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
    container.setLayoutData(gd_composite_1);
    container.setLayout(new GridLayout(3, false));

    Label locationLabel = new Label(container, SWT.NONE);
    locationLabel.setText(WizardMessages.ShowDirectoryWizardPage_locationLabel);

    directoryField = new Text(container, SWT.BORDER);
    directoryField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    directoryField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        directoryField.selectAll();
      }

      @Override
      public void focusLost(FocusEvent e) {
      }
    });
    directoryField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLocation();
      }
    });

    Button browseButton = new Button(container, SWT.NONE);
    browseButton.setText(WizardMessages.ShowDirectoryWizardPage_browseButton);
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowseButton(directoryField);
      }
    });

    directoryField.setFocus();
    setPageComplete(false);
    setControl(container);
  }

  public String getDirectory() {
    return directory;
  }

  @Override
  protected void handleBrowseButton(Text locationField) {
    DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
    String location = locationField.getText().trim();
//    if (!new File(location).exists()) {
//      if (new File(defaultLocation).exists()) {
//        location = defaultLocation;
//      } else {
//        location = new Path(defaultLocation).removeLastSegments(1).toString();
//      }
//    }

    directoryDialog.setFilterPath(location);
    String directoryOnDisk = directoryDialog.open();
    if (directoryOnDisk != null) {
      locationField.setText(directoryOnDisk);
      return;
    }

  }

  protected void updateLocation() {
    updateState();
  }

  private void updateState() {
    String directoryStr = directoryField.getText();
    directoryStr = directoryStr.trim();
    if (directoryStr.isEmpty()) {
      return;
    }
    IStatus status = validate(directoryStr);
    if (status.isOK()) {
      this.directory = directoryStr;
      setPageComplete(true);
      setMessage(WizardMessages.ShowDirectoryWizardPage_message);
      setErrorMessage(null);
    } else {
      setPageComplete(false);
      setErrorMessage(status.getMessage());
    }
  }

  private IStatus validate(String absolutePathDir) {
    return StatusUtil.getMoreSevere(Status.OK_STATUS, validateLocation(absolutePathDir));
  }

  private IStatus validateLocation(String absolutePathDir) {
    if (absolutePathDir == null || absolutePathDir.isEmpty()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "Enter in a directory"); //$NON-NLS-1$
    }
    File file = new File(absolutePathDir);
    if (!file.exists()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Enter in a directory location that exists on disk"); //$NON-NLS-1$
    }
    if (!file.isDirectory()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Enter in a directory, the entered path is to a file"); //$NON-NLS-1$
    }
    if (file.isHidden()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "The directory you have entered is hidden"); //$NON-NLS-1$
    }
    char lastChar = absolutePathDir.charAt(absolutePathDir.length() - 1);
    if (lastChar == '\\' || lastChar == '/') {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Don't include a slash at the end of the path."); //$NON-NLS-1$
    }
    if (DartCore.getDirectorySetManager().hasPath(absolutePathDir)) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "The directory is already in the Files view."); //$NON-NLS-1$
    }
    return Status.OK_STATUS;
  }
}
