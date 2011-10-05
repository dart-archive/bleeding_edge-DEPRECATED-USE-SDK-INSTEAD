/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.wizard;

import com.google.dart.tools.core.generator.ApplicationGenerator;
import com.google.dart.tools.core.generator.FileGenerator;

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
 * The New Application wizard page.
 * 
 * @see NewApplicationWizard
 * @see ApplicationGenerator
 */
public class NewApplicationWizardPage extends AbstractDartWizardPage {

  private final boolean DEBUG = false;

  private final ApplicationGenerator applicationGenerator;

  private Text applicationLocationField;

  private Text applicationNameField;

  private boolean useDefaultLocation = true;
  private String defaultLocation;

  protected NewApplicationWizardPage(ApplicationGenerator applicationGenerator) {
    super("newApplicationWizardPage1");
    Assert.isNotNull(applicationGenerator);
    this.applicationGenerator = applicationGenerator;
    setTitle(WizardMessages.NewApplicationWizardPage_title);
    setDescription(ApplicationGenerator.DESCRIPTION);
    defaultLocation = getDefaultFolder();
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    setControl(container);
    container.setLayout(new GridLayout(1, false));

    GridData gd_composite_1 = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
    container.setLayoutData(gd_composite_1);
    container.setLayout(new GridLayout(3, false));

    Label nameLabel = new Label(container, SWT.NONE);
    nameLabel.setText(WizardMessages.NewApplicationWizardPage_name);

    applicationNameField = new Text(container, SWT.BORDER);
    applicationNameField.setText("");
    applicationNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    applicationNameField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        applicationNameField.selectAll();
      }
    });
    applicationNameField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateName();
      }
    });

    Label locationLabel = new Label(container, SWT.NONE);
    locationLabel.setText(WizardMessages.NewApplicationWizardPage_directory);

    applicationLocationField = new Text(container, SWT.BORDER);
    applicationLocationField.setText(defaultLocation);
    applicationGenerator.setApplicationLocation(applicationLocationField.getText());
    applicationLocationField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    applicationLocationField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        applicationLocationField.selectAll();
      }
    });
    applicationLocationField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLocation();
      }
    });

    Button browseButton = new Button(container, SWT.NONE);
    browseButton.setText(WizardMessages.NewApplicationWizardPage_browse);
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowseButton(applicationLocationField);
      }
    });

    applicationNameField.setFocus();
    setPageComplete(false);
  }

  @Override
  protected String getDefaultFolder() {
    String defaultLocation = System.getProperty("user.home"); //$NON-NLS-1$
    return defaultLocation + File.separator + "dart" + File.separator;

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
   * Update the name of the library in the {@link ApplicationGenerator}.
   */
  protected void updateLocation() {
    applicationGenerator.setApplicationLocation(applicationLocationField.getText());
    updateState();
  }

  /**
   * Update the name of the file in the {@link FileGenerator}.
   */
  protected void updateName() {
    applicationGenerator.setApplicationName(applicationNameField.getText());
    if (useDefaultLocation) {
      applicationLocationField.setText(defaultLocation + applicationNameField.getText().trim());
    }
    updateState();
  }

  private void updateState() {
    if (applicationNameField.getText().isEmpty()) {
      return;
    }
    IStatus status = applicationGenerator.validate();
    // Should this be a trace instead of a local DEBUG flag?
    if (DEBUG) {
      System.out.println("NewLibraryWizardPage.updateState()");
      System.out.println("\t\"" + applicationLocationField.getText() + "\"");
      System.out.println("\t\"" + applicationNameField.getText() + "\"");
      if (status.isOK()) {
        System.out.println("\tSTATUS IS OKAY \"" + status.getMessage() + "\"");
      } else {
        System.out.println("\tSTATUS NOT OKAY \"" + status.getMessage() + "\"");
      }
    }
    setMessage(WizardMessages.NewApplicationWizardPage_message);
    if (status.isOK()) {
      setPageComplete(true);
      setMessage(WizardMessages.NewApplicationWizardPage_message);
      setErrorMessage(null);
    } else {
      setPageComplete(false);
      setErrorMessage(status.getMessage());
    }
  }
}
