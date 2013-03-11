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

import com.google.dart.tools.core.generator.AbstractSample;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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

import java.util.List;

class ProjectComposite extends Composite {
  static final String NEW_APPPLICATION_SETTINGS = "newApplicationWizard.settings"; //$NON-NLS-1$
  private static final String CONTENT_GENERATION_DISABLED = "contentGenerationDisabled"; //$NON-NLS-1$

  private DartProjectWizardPage page;

  private Label nameLabel;
  private Text nameField;
  private Group contentGroup;
  private Button newProjectInWorkspaceRadioButton;
  private Button newProjectFromSourceRadioButton;
  private Label directoryLabel;
  private Text existingSourcePathText;
  private Button browseButton;

  private Button addSampleContentCheckbox;
  private ListViewer samplesListViewer;

  public ProjectComposite(DartProjectWizardPage page, Composite parent, int style) {
    super(parent, style);

    this.page = page;

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

  protected AbstractSample getCurrentSample() {
    if (addSampleContentCheckbox.getSelection()) {
      IStructuredSelection selection = (IStructuredSelection) samplesListViewer.getSelection();

      if (selection.isEmpty()) {
        return null;
      } else {
        return (AbstractSample) selection.getFirstElement();
      }
    } else {
      return null;
    }
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
    contentGroup.setText("Location");
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
    contentGroup.setText("Sample content");
    GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(contentGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(contentGroup);

    addSampleContentCheckbox = new Button(contentGroup, SWT.CHECK);
    addSampleContentCheckbox.setText("Create sample content");
    addSampleContentCheckbox.setSelection(getGenerateContentPreference());
    addSampleContentCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
            NEW_APPPLICATION_SETTINGS);
        settings.put(CONTENT_GENERATION_DISABLED, !addSampleContentCheckbox.getSelection());

        updateMessageAndEnablement();
      }
    });

    Label spacer = new Label(contentGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(spacer);

    samplesListViewer = new ListViewer(contentGroup);
    samplesListViewer.setLabelProvider(new LabelProvider());
    samplesListViewer.setContentProvider(new ArrayContentProvider());
    List<AbstractSample> samples = AbstractSample.getAllSamples();
    samplesListViewer.setInput(samples);
    GridDataFactory.fillDefaults().hint(-1, 60).grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(
        samplesListViewer.getControl());
    samplesListViewer.setSelection(new StructuredSelection(getDefaultSample(samples)));
    samplesListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateMessageAndEnablement();
      }
    });

    samplesListViewer.getList().setEnabled(addSampleContentCheckbox.getSelection());
  }

  private AbstractSample getDefaultSample(List<AbstractSample> samples) {
    for (AbstractSample sample : samples) {
      if (sample.shouldBeDefault()) {
        return sample;
      }
    }

    return samples.get(0);
  }

  private boolean getGenerateContentPreference() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
        NEW_APPPLICATION_SETTINGS);
    return !settings.getBoolean(CONTENT_GENERATION_DISABLED);
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
    nameField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        updateMessageAndEnablement();
      }
    });

    setLayout(gridLayout);
    createContentGroup();
    createSampleGroup();

    setSize(new Point(449, 311));
  }

  private void updateMessageAndEnablement() {
    page.updatePage();

    samplesListViewer.getList().setEnabled(addSampleContentCheckbox.getSelection());
  }

}
