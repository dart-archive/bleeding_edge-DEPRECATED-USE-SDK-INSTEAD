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

import com.google.dart.eclipse.DartEclipseUI;
import com.google.dart.tools.core.generator.AbstractSample;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Dart project wizard creation page.
 */
public class DartProjectWizardPage extends WizardPage {
  private SamplesComposite samplesComposite;
  private Text defaultLocationPathText;
  private Button browseButton;
  private Label directoryLabel;
  private Button useDefaultLocation;
  private Text projectNameField;

  public DartProjectWizardPage(ISelection selection) {
    super("wizardPage");

    setTitle("Create a Dart Project");
    setDescription("This wizard creates a new Dart project.");
    setImageDescriptor(DartEclipseUI.getImageDescriptor("wizban/newprj_wiz.png"));
  }

  @Override
  public void createControl(Composite parent) {
    initializeDialogUnits(parent);

    final Composite composite = new Composite(parent, SWT.NULL);
    composite.setFont(parent.getFont());
    composite.setLayout(new GridLayout(1, false));
    composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

    Control nameControl = createNameGroup(composite);
    nameControl.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    Control locationControl = createLocationGroup(composite);
    locationControl.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

    samplesComposite = new SamplesComposite(this, composite, SWT.NONE);
    samplesComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    setControl(composite);
    updatePage();
  }

  public String getProjectLocation() {
    return defaultLocationPathText.getText().trim();
  }

  public String getProjectName() {
    return projectNameField.getText().trim();
  }

  protected AbstractSample getSampleContent() {
    return samplesComposite.getCurrentSample();
  }

  protected void updatePage() {
    AbstractSample sample = getSampleContent();
    setMessage(sample == null ? null : sample.getDescription());

    setPageComplete(validatePage());
  }

  protected boolean validatePage() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();

    String projectFieldContents = getProjectName();

    if (projectFieldContents.equals("")) {
      setErrorMessage(null);
      setMessage("Project name must be specified");
      return false;
    }

    IStatus nameStatus = workspace.validateName(projectFieldContents, IResource.PROJECT);
    if (!nameStatus.isOK()) {
      setErrorMessage(nameStatus.getMessage());
      return false;
    }

    IProject handle = workspace.getRoot().getProject(getProjectName());
    if (handle.exists()) {
      setErrorMessage("A project with that name already exists in the workspace.");
      return false;
    }

    setErrorMessage(null);
    setMessage(null);
    return true;
  }

  private Control createLocationGroup(Composite composite) {
    final int numColumns = 4;

    final Composite locationComposite = new Composite(composite, SWT.NONE);
    locationComposite.setLayout(new GridLayout(numColumns, false));

    useDefaultLocation = new Button(locationComposite, SWT.CHECK);
    useDefaultLocation.setSelection(true);
    useDefaultLocation.setText("Use default location");
    GridDataFactory.fillDefaults().grab(true, false).span(numColumns, 1).applyTo(useDefaultLocation);
    useDefaultLocation.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent arg0) {
        if (!useDefaultLocation.getSelection()) {
          directoryLabel.setEnabled(true);
          defaultLocationPathText.setEnabled(true);
          browseButton.setEnabled(true);
        } else {
          directoryLabel.setEnabled(false);
          defaultLocationPathText.setEnabled(false);
          browseButton.setEnabled(false);
        }
      }
    });

    directoryLabel = new Label(locationComposite, SWT.NONE);
    directoryLabel.setText("Location:");
    directoryLabel.setEnabled(false);

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    defaultLocationPathText = new Text(locationComposite, SWT.BORDER);
    defaultLocationPathText.setText(root.getLocation().toString());
    defaultLocationPathText.setEnabled(false);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(
        defaultLocationPathText);

    browseButton = new Button(locationComposite, SWT.NONE);
    browseButton.setText("Browse...");
    GridDataFactory.fillDefaults().hint(75, SWT.DEFAULT).applyTo(browseButton);
    browseButton.setEnabled(false);
    browseButton.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        String selPath = new DirectoryDialog(getShell()).open();

        if (selPath != null) {
          defaultLocationPathText.setText(selPath);
        }
      }
    });

    return locationComposite;
  }

  private Control createNameGroup(final Composite composite) {
    Composite nameComposite = new Composite(composite, SWT.NONE);
    nameComposite.setFont(composite.getFont());
    nameComposite.setLayout(new GridLayout(2, false));
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).applyTo(nameComposite);

    Label nameLabel = new Label(nameComposite, SWT.NONE);
    nameLabel.setText("Project name:");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1).applyTo(nameLabel);

    projectNameField = new Text(nameComposite, SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(1, 1).applyTo(
        projectNameField);
    projectNameField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        updateMessageAndEnablement();
      }
    });
    return nameComposite;
  }

  private void updateMessageAndEnablement() {
    updatePage();

    samplesComposite.update();
  }

}
