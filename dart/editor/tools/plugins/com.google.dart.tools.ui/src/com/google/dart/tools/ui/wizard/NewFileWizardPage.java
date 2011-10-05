/*
 * Copyright (c) 2011, the Dart project authors.
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
import com.google.dart.tools.core.generator.AbstractGenerator;
import com.google.dart.tools.core.generator.FileGenerator;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.model.DartLibrary;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
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
 * The New File wizard page.
 * 
 * @see NewFileWizard
 * @see FileGenerator
 */
public class NewFileWizardPage extends AbstractDartWizardPage {

  private final FileGenerator fileGenerator;

  private Text fileLocationField;

  private Text fileNameField;

  private DartLibrary library = null;

  private boolean useDefaultLocation = true;

  private String defaultLocation;

  protected NewFileWizardPage(FileGenerator fileGenerator) {
    super("newFileWizardPage1"); //$NON-NLS-1$
    Assert.isNotNull(fileGenerator);
    this.fileGenerator = fileGenerator;
    setTitle(WizardMessages.NewFileWizardPage_title);
    setDescription(FileGenerator.DESCRIPTION);
  }

  @Override
  public void createControl(Composite parent) {
    // If there is a selected library, then set the library in the fileGenerator
    library = getSelectedLibrary();
    fileGenerator.setLibrary(library);

    Composite container = new Composite(parent, SWT.NULL);
    setControl(container);
    container.setLayout(new GridLayout(1, false));

    GridData gd_composite_1 = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
    container.setLayoutData(gd_composite_1);
    container.setLayout(new GridLayout(3, false));

    Label fileNameLabel = new Label(container, SWT.NONE);
    fileNameLabel.setText(WizardMessages.NewFileWizardPage_fileNameLabel);

    fileNameField = new Text(container, SWT.BORDER);
    fileNameField.setText(""); //$NON-NLS-1$
    fileNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    fileNameField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        fileNameField.selectAll();
      }
    });
    fileNameField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateFileName();
      }
    });

    Label libraryLocationLabel = new Label(container, SWT.NONE);

    libraryLocationLabel.setText(WizardMessages.NewFileWizardPage_directory);

    fileLocationField = new Text(container, SWT.BORDER);
    fileLocationField.setText(defaultLocation = getDefaultFileLocationFieldText());
    fileGenerator.setFileLocation(fileLocationField.getText());
    fileLocationField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    fileLocationField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        fileLocationField.selectAll();
      }
    });
    fileLocationField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateFileLocation();
      }
    });

    Button browseButton = new Button(container, SWT.NONE);

    browseButton.setText(WizardMessages.NewFileWizardPage_browse);
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowseButton(fileLocationField);
      }
    });

    fileNameField.setFocus();
    setPageComplete(false);
  }

  @Override
  protected void handleBrowseButton(Text locationField) {
    DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
    String location = locationField.getText().trim();
    if (!new File(location).exists()) {
      if (new File(defaultLocation).exists()) {
        location = defaultLocation;
      } else {
        location = new Path(defaultLocation).removeLastSegments(1).toString();
      }
    }

    directoryDialog.setFilterPath(location);
    String directoryOnDisk = directoryDialog.open();
    if (directoryOnDisk != null) {
      locationField.setText(directoryOnDisk);
      useDefaultLocation = false;
      return;
    }

  }

  /**
   * Update the location of the file in the {@link FileGenerator}.
   */
  protected void updateFileLocation() {
    fileGenerator.setFileLocation(fileLocationField.getText());
    updateState();
  }

  /**
   * Update the name of the file in the {@link FileGenerator}.
   */
  protected void updateFileName() {
    fileGenerator.setFileName(fileNameField.getText());
    updateState();
  }

  private String getDefaultFileLocationFieldText() {
    String defaultLocation = System.getProperty("user.home"); //$NON-NLS-1$
    if (defaultLocation == null || defaultLocation.isEmpty()) {
      defaultLocation = File.separator + "dart" + File.separator;
    } else {
      defaultLocation = defaultLocation + File.separator + "dart" + File.separator;
    }
    // Note, we do not want the workspace directory, this directory cannot be linked:
    // ResourcesPlugin.getWorkspace().getRoot().getLocation().makeAbsolute().toOSString()
    // if there was was a selected library, be intelligent and set this location as the default location
    if (library != null) {
      String tempLocString = FileGenerator.getStringLibraryPath(library);
      if (tempLocString != null && !tempLocString.isEmpty()) {
        defaultLocation = tempLocString;
      }
    }
    return defaultLocation;
  }

  /**
   * Update the error/warning message, and page complete flag.
   */
  private void updateState() {
    // first use the file generator to validate the 
    IStatus status = fileGenerator.validate();
    if (status.getSeverity() == IStatus.ERROR) {
      setMessage(null);
      setErrorMessage(status.getMessage());
      setPageComplete(false);
    } else {
      setErrorMessage(null);
      // Write a different message depending on the type of file.
      // First, if some name without a '.' was entered in, then append ".dart"
      String fileName = AbstractGenerator.appendIfNoExtension(fileGenerator.getFileName(),
          Extensions.DOT_DART);
      if (DartCore.isDartLikeFileName(fileName)) {
        setMessage(
            status.isOK() ? WizardMessages.NewFileWizardPage_newDartFile + fileName
                : status.getMessage(), status.matches(IStatus.WARNING) ? WARNING : NONE);
      } else if (DartCore.isHTMLLikeFileName(fileName)) {
        setMessage(
            status.isOK() ? WizardMessages.NewFileWizardPage_newHTMLFile + fileName
                : status.getMessage(), status.matches(IStatus.WARNING) ? WARNING : NONE);
      } else if (DartCore.isCSSLikeFileName(fileName)) {
        setMessage(
            status.isOK() ? WizardMessages.NewFileWizardPage_newCSSFile + fileName
                : status.getMessage(), status.matches(IStatus.WARNING) ? WARNING : NONE);
      } else {
        setMessage(status.isOK() ? WizardMessages.NewFileWizardPage_newEmptyFile + fileName
            : status.getMessage(), status.matches(IStatus.WARNING) ? WARNING : NONE);
      }
      setPageComplete(true);
    }
  }

}
