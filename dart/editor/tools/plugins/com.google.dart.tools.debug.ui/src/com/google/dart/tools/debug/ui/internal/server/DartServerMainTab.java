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
package com.google.dart.tools.debug.ui.internal.server;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.preferences.DebugPreferencePage;
import com.google.dart.tools.debug.ui.internal.util.AppSelectionDialog;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * The main (and currently only) tab for the Dart Server Application launch configuration.
 */
public class DartServerMainTab extends AbstractLaunchConfigurationTab {
  private Text applicationText;
  private Text argumentsText;
  private Text projectText;
  private Combo runnerCombo;
  private CLabel runnerWarningLabel;

  private ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  /**
   * Create a new instance of DartServerMainTab.
   */
  public DartServerMainTab() {

  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 1).applyTo(composite);

    // Project group
    Group group = new Group(composite, SWT.NONE);
    group.setText("Execution Target:");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    Label label = new Label(group, SWT.NONE);
    label.setText("Project:");

    projectText = new Text(group, SWT.BORDER | SWT.SINGLE);
    projectText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(projectText);

    Button button = new Button(group, SWT.PUSH);
    button.setText("Browse...");
    PixelConverter converter = new PixelConverter(button);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleProjectBrowseButton();
      }
    });

    label = new Label(group, SWT.NONE);
    label.setText("Application:");

    applicationText = new Text(group, SWT.BORDER | SWT.SINGLE);
    applicationText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        applicationText);

    button = new Button(group, SWT.PUSH);
    button.setText("Browse...");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleApplicationBrowseButton();
      }
    });

    label = new Label(group, SWT.NONE);
    label.setText("Application arguments:");
    GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);

    argumentsText = new Text(group, SWT.BORDER | SWT.MULTI);
    argumentsText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 75).applyTo(
        argumentsText);

    // spacer
    label = new Label(group, SWT.NONE);

    // Runner group
    group = new Group(composite, SWT.NONE);
    group.setText("Runner:");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);

    runnerCombo = new Combo(group, SWT.READ_ONLY | SWT.DROP_DOWN);
    runnerCombo.setItems(DartLaunchConfigWrapper.SERVER_RUNNERS);
    runnerCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        validateRunnerChoice();
        notifyPanelChanged();
      }
    });
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(runnerCombo);

    Link link = new Link(group, SWT.NONE);
    link.setText("<a href=\"settings\">settings...</a>");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(link);
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
            Display.getDefault().getActiveShell(), DebugPreferencePage.PAGE_ID, null, null);

        if (dialog != null) {
          dialog.open();
          validateRunnerChoice();
        }
      }
    });

    runnerWarningLabel = new CLabel(group, SWT.NONE);
    runnerWarningLabel.setText("");
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(runnerWarningLabel);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    // check that the project is filled in
    String projectName = projectText.getText();

    if (projectName.length() == 0) {
      return "A source project is required.";
    }

    // check that the project exists
    DartProject project = getDartProject();

    if (!project.exists()) {
      return "Project " + projectName + " does not exist.";
    }

    // check that the application name is not empty
    if (applicationText.getText().length() == 0) {
      return "An application name is required.";
    }

    return null;
  }

  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("chromium_16_server.png");
  }

  @Override
  public String getName() {
    return "Main";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    projectText.setText(dartLauncher.getProjectName());
    applicationText.setText(dartLauncher.getApplicationName());
    argumentsText.setText(dartLauncher.getArguments());

    runnerCombo.setText(dartLauncher.getServerRunner());

    validateRunnerChoice();
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    dartLauncher.setProjectName(projectText.getText());
    dartLauncher.setApplicationName(applicationText.getText());
    dartLauncher.setArguments(argumentsText.getText());

    dartLauncher.setServerRunner(runnerCombo.getText());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    dartLauncher.setProjectName("");
    dartLauncher.setApplicationName("");
    dartLauncher.setArguments("");

    dartLauncher.setServerRunner(DartLaunchConfigWrapper.SERVER_RUNNERS[0]);
  }

  protected DartProject getDartProject() {
    String projectName = projectText.getText().trim();
    if (projectName.length() < 1) {
      return null;
    }
    return DartCore.create(getWorkspaceRoot()).getDartProject(projectName);
  }

  protected void handleApplicationBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    AppSelectionDialog dialog = new AppSelectionDialog(getShell(), workspace.getRoot(), true, false);
    dialog.setTitle("Select a resource to launch");
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION);
    IPath path = new Path(applicationText.getText());
    if (workspace.validatePath(path.toString(), IResource.FILE).isOK()) {
      IFile file = workspace.getRoot().getFile(path);
      if (file != null && file.exists()) {
        dialog.setInitialSelections(new Object[] {path});
      }
    }

    dialog.open();

    Object[] results = dialog.getResult();
    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
      String pathStr = ((IFile) results[0]).getFullPath().toPortableString();

      applicationText.setText(pathStr);
    }
  }

  protected void handleProjectBrowseButton() {
    DartProject project = chooseDartProject();

    if (project != null) {
      projectText.setText(project.getElementName());
    }
  }

  private DartProject chooseDartProject() {
    ILabelProvider labelProvider = new LabelProvider();
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
    dialog.setTitle("Project Selection");
    dialog.setMessage("Select a project to constrain your search.");
    try {
      dialog.setElements(DartCore.create(getWorkspaceRoot()).getDartProjects());
    } catch (DartModelException ex) {
      DartUtil.logError(ex);
    }

    DartProject dartProject = getDartProject();

    if (dartProject != null) {
      dialog.setInitialSelections(new Object[] {dartProject});
    }

    if (dialog.open() == Window.OK) {
      return (DartProject) dialog.getFirstResult();
    }

    return null;
  }

  private IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  private void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }

  private void validateRunnerChoice() {
    if (runnerCombo.getText().equals(DartLaunchConfigWrapper.SERVER_RUNNER_NODEJS)
        && DartDebugCorePlugin.getPlugin().getNodeExecutablePath().length() == 0) {
      runnerWarningLabel.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
      runnerWarningLabel.setText("The Node executable path has not been set");
    } else if (runnerCombo.getText().equals(DartLaunchConfigWrapper.SERVER_RUNNER_RHINO)
        && DartDebugCorePlugin.getPlugin().getJreExecutablePath().length() == 0) {
      runnerWarningLabel.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
      runnerWarningLabel.setText("The Java executable path has not been set (required by Rhino)");
    } else {
      runnerWarningLabel.setImage(null);
      runnerWarningLabel.setText("");
    }
  }

}
