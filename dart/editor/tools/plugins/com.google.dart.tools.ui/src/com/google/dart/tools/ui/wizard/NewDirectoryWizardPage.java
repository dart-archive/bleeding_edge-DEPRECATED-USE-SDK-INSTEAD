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
import com.google.dart.tools.ui.actions.NewDirectoryWizardAction;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/**
 * The New Directory wizard page.
 * 
 * @see NewDirectoryWizardAction
 * @see NewDirectoryWizard
 */
public class NewDirectoryWizardPage extends AbstractDartWizardPage {

  private Text directoryNameField;

  private File directoryFile;

  protected NewDirectoryWizardPage(final File file) {
    super("newDirectoryWizardPage1"); //$NON-NLS-1$
    Assert.isNotNull(file);
    Assert.isTrue(file.exists());
    Assert.isTrue(file.isDirectory());
    setTitle(WizardMessages.NewDirectoryWizardPage_title);
    setDescription(WizardMessages.NewDirectoryWizardPage_description);
    directoryFile = file;
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);

    GridData gd_composite_1 = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
    container.setLayoutData(gd_composite_1);
    container.setLayout(new GridLayout(3, false));

    Label locationLabel = new Label(container, SWT.NONE);
    locationLabel.setText(WizardMessages.NewDirectoryWizardPage_locationLabel);

    directoryNameField = new Text(container, SWT.BORDER);
    directoryNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    directoryNameField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        directoryNameField.selectAll();
      }

      @Override
      public void focusLost(FocusEvent e) {
      }
    });
    directoryNameField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLocation();
      }
    });

    directoryNameField.setFocus();
    setPageComplete(false);
    setControl(container);
  }

  public File getNewDirectoryFile() {
    return new File(directoryFile.getAbsolutePath() + File.separator
        + directoryNameField.getText().trim());
  }

  protected void updateLocation() {
    updateState();
  }

  private void updateState() {
    String dirName = directoryNameField.getText();
    dirName = dirName.trim();
    if (dirName.isEmpty()) {
      return;
    }
    IStatus status = validate(dirName);
    if (status.isOK()) {
      setPageComplete(true);
      setMessage(WizardMessages.NewDirectoryWizardPage_message);
      setErrorMessage(null);
    } else {
      setPageComplete(false);
      setErrorMessage(status.getMessage());
    }
  }

  private IStatus validate(String dirName) {
    return StatusUtil.getMoreSevere(Status.OK_STATUS, validateLocation(dirName));
  }

  private IStatus validateLocation(String dirName) {
    if (dirName == null || dirName.isEmpty()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "Enter in a new directory name."); //$NON-NLS-1$
    }
    if (dirName.startsWith(".")) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Enter a directory name that won't be private, don't have the directory name start with '.'."); //$NON-NLS-1$
    }
    if (dirName.indexOf("/") != -1 || dirName.indexOf("\\") != -1) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "Directory name can't contain slashs."); //$NON-NLS-1$
    }
    File file = getNewDirectoryFile();
    if (file.exists() && file.isDirectory()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Enter a directory name that does not already exist on disk."); //$NON-NLS-1$
    }
    return Status.OK_STATUS;
  }
}
