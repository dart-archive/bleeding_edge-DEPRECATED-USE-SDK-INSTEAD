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
package com.google.dart.eclipse.wizards;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;
import com.google.dart.tools.ui.internal.projects.ProjectMessages;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

class ProjectComposite extends Composite {

  private Label nameLabel;
  private Text nameField;
  private Group contentGroup;
  private Button newProjectInWorkspaceRadioButton;
  private Button newProjectFromSourceRadioButton;
  private Label directoryLabel;
  private Text existingSourcePathText;
  private Button browseButton;

//  private Button addSampleContentCheckbox;
  private Button webAppCheckboxButton;
  private Button pubSupportCheckboxButton;

  public ProjectComposite(Composite parent, int style) {
    super(parent, style);
    initialize();
  }

  /**
   * Get the project name.
   */
  public String getProjectName() {
    return nameField.getText().trim();
  }

  /**
   * Get the project path (or <code>null</code> if unset).
   */
  public String getProjectPath() {
    if (newProjectFromSourceRadioButton.getSelection()) {
      return existingSourcePathText.getText().trim();
    }

    return null;
  }

  public ProjectType getSampleType() {
//    if (!addSampleContentCheckbox.getSelection()) {
//      return ProjectType.NONE;
//    }
    return webAppCheckboxButton.getSelection() ? ProjectType.WEB : ProjectType.SERVER;
  }

  public boolean hasPubSupport() {
    return pubSupportCheckboxButton.getSelection();
  }

  private void createContentGroup() {
    GridData gridData6 = new GridData();
    gridData6.widthHint = 75;
    gridData6.horizontalSpan = 2;

    GridData gridData5 = new GridData();
    gridData5.grabExcessHorizontalSpace = true;
    gridData5.verticalAlignment = GridData.CENTER;
    gridData5.horizontalAlignment = GridData.FILL;

    GridData gridData4 = new GridData();
    gridData4.horizontalSpan = 4;

    GridData gridData3 = new GridData();
    gridData3.horizontalSpan = 4;

    GridData gridData2 = new GridData();
    gridData2.horizontalSpan = 3;
    gridData2.horizontalAlignment = GridData.FILL;
    gridData2.verticalAlignment = GridData.FILL;
    gridData2.grabExcessVerticalSpace = false;
    gridData2.grabExcessHorizontalSpace = true;

    GridLayout gridLayout1 = new GridLayout();
    gridLayout1.numColumns = 4;
    gridLayout1.horizontalSpacing = 5;
    gridLayout1.marginWidth = 10;
    gridLayout1.marginHeight = 10;
    gridLayout1.verticalSpacing = 7;

    contentGroup = new Group(this, SWT.SHADOW_ETCHED_IN);
    contentGroup.setText("Contents");
    contentGroup.setLayout(gridLayout1);
    contentGroup.setLayoutData(gridData2);

    newProjectInWorkspaceRadioButton = new Button(contentGroup, SWT.RADIO);
    newProjectInWorkspaceRadioButton.setText("Create new project in workspace");
    newProjectInWorkspaceRadioButton.setSelection(true);
    newProjectInWorkspaceRadioButton.setLayoutData(gridData4);
    newProjectFromSourceRadioButton = new Button(contentGroup, SWT.RADIO);
    newProjectFromSourceRadioButton.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetDefaultSelected(SelectionEvent arg0) {
        toggleState();
      }

      @Override
      public void widgetSelected(SelectionEvent arg0) {
        toggleState();
      }

      private void toggleState() {
        if (newProjectFromSourceRadioButton.getSelection()) {
          directoryLabel.setEnabled(true);
          existingSourcePathText.setEnabled(true);
          browseButton.setEnabled(true);
        } else {
          directoryLabel.setEnabled(false);
          existingSourcePathText.setEnabled(false);
          browseButton.setEnabled(false);
        }
      }
    });

    newProjectFromSourceRadioButton.setText("Create project from existing source");
    newProjectFromSourceRadioButton.setLayoutData(gridData3);

    directoryLabel = new Label(contentGroup, SWT.NONE);
    directoryLabel.setText("Directory:");
    directoryLabel.setEnabled(false);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();

    IPath path = root.getLocation();
    String stringPath = path.toString();

    existingSourcePathText = new Text(contentGroup, SWT.BORDER);
    existingSourcePathText.setText(stringPath);
    existingSourcePathText.setEnabled(false);
    existingSourcePathText.setLayoutData(gridData5);

    browseButton = new Button(contentGroup, SWT.NONE);
    browseButton.setText("Browse...");
    browseButton.setLayoutData(gridData6);
    browseButton.setEnabled(false);
    browseButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {

      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        String selPath = new DirectoryDialog(getShell()).open();

        if (selPath != null) {
          existingSourcePathText.setText(selPath);
        }
      }
    });
  }

  private void createSampleGroup() {

    Group contentGroup = new Group(this, SWT.NONE);
    GridDataFactory.fillDefaults().span(3, 1).grab(true, false).indent(0, 10).applyTo(contentGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(contentGroup);

//    addSampleContentCheckbox = new Button(contentGroup, SWT.CHECK);
//    addSampleContentCheckbox.setText("Create sample content");

    webAppCheckboxButton = new Button(contentGroup, SWT.CHECK);
    webAppCheckboxButton.setText(ProjectMessages.NewApplicationWizardPage_webAppCheckbox_name_label);
    webAppCheckboxButton.setSelection(getWebAppCheckboxEnabled());
    webAppCheckboxButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
            NewApplicationCreationPage.NEW_APPPLICATION_SETTINGS);
        settings.put(
            NewApplicationCreationPage.WEB_APP_CHECKBOX_DISABLED,
            !webAppCheckboxButton.getSelection());
      }
    });

    pubSupportCheckboxButton = new Button(contentGroup, SWT.CHECK);
    pubSupportCheckboxButton.setText(ProjectMessages.NewApplicationCreationPage_pubSupportCheckbox_name_label);
    pubSupportCheckboxButton.setSelection(getPubSupportCheckboxEnabled());
    pubSupportCheckboxButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
            NewApplicationCreationPage.NEW_APPPLICATION_SETTINGS);
        settings.put(
            NewApplicationCreationPage.PUB_SUPPORT_CHECKBOX_DISABLED,
            !pubSupportCheckboxButton.getSelection());
      }
    });

  }

  private boolean getPubSupportCheckboxEnabled() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
        NewApplicationCreationPage.NEW_APPPLICATION_SETTINGS);
    return !settings.getBoolean(NewApplicationCreationPage.PUB_SUPPORT_CHECKBOX_DISABLED);
  }

  private boolean getWebAppCheckboxEnabled() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
        NewApplicationCreationPage.NEW_APPPLICATION_SETTINGS);
    // The following has one of three (not two) states:
    // 1) If it has never been set before (see listener on checkbox), this will return true- the default behavior
    // 2) If WEB_APP_CHECKBOX_DISABLED is false, return true.
    // 3) If WEB_APP_CHECKBOX_DISABLED is true, return false.
    return !settings.getBoolean(NewApplicationCreationPage.WEB_APP_CHECKBOX_DISABLED);
  }

  private void initialize() {
    GridData gridData1 = new GridData();
    gridData1.heightHint = -1;

    GridData gridData = new GridData();
    gridData.horizontalSpan = 2;
    gridData.horizontalAlignment = GridData.FILL;
    gridData.verticalAlignment = GridData.CENTER;
    gridData.grabExcessHorizontalSpace = true;

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.horizontalSpacing = 5;
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    gridLayout.verticalSpacing = 5;

    nameLabel = new Label(this, SWT.NONE);
    nameLabel.setText("Project name:");
    nameLabel.setLayoutData(gridData1);

    nameField = new Text(this, SWT.BORDER);
    nameField.setLayoutData(gridData);

    setLayout(gridLayout);
    createContentGroup();
    createSampleGroup();

    setSize(new Point(449, 311));
  }

}
