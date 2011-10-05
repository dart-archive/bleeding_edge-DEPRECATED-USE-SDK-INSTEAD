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
package com.google.dart.tools.debug.ui.internal.remote;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * The main (and currently only) tab for the Dart Remote Debug launch configuration.
 */
public class DartRemoteMainTab extends AbstractLaunchConfigurationTab {
  private Label connectionTypeInstructions;
  private Combo connectionTypesCombo;

  private Text hostText;

  private Text portText;
  private Text projectText;

  private ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  /**
   * Create a new instance of DartRemoteMainTab.
   */
  public DartRemoteMainTab() {

  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 1).applyTo(composite);

    // Project group
    Group group = new Group(composite, SWT.NONE);
    group.setText("Project:");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);

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
        handleBrowseButton();
      }
    });

    // Connection type group
    group = new Group(composite, SWT.NONE);
    group.setText("Connection Type:");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().applyTo(group);

    connectionTypesCombo = new Combo(group, SWT.READ_ONLY | SWT.DROP_DOWN);
    connectionTypesCombo.setItems(DartLaunchConfigWrapper.CONNECTION_TYPES);
    connectionTypesCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });
    GridDataFactory.swtDefaults().applyTo(connectionTypesCombo);

    connectionTypeInstructions = new Label(group, SWT.NONE);
    connectionTypeInstructions.setText("");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(connectionTypeInstructions);

    // Connection properties group
    group = new Group(composite, SWT.NONE);
    group.setText("Connection Properties:");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).spacing(8, 5).applyTo(group);

    Label label = new Label(group, SWT.NONE);
    label.setText("Host:");
    GridDataFactory.swtDefaults().applyTo(label);

    hostText = new Text(group, SWT.BORDER | SWT.SINGLE);
    hostText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(hostText);

    label = new Label(group, SWT.NONE);
    label.setText("Port:");
    GridDataFactory.swtDefaults().applyTo(label);

    portText = new Text(group, SWT.BORDER | SWT.SINGLE);
    portText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(portText);

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
    DartProject project = DartCore.create(getWorkspaceRoot()).getDartProject(projectName);

    if (!project.exists()) {
      return "Project " + projectName + " does not exist.";
    }

    // check that the host is not empty
    if (hostText.getText().length() == 0) {
      return "A host name is required.";
    }

    // check that the port value is reasonable
    if (portText.getText().length() == 0) {
      return "A port value is required.";
    }

    if (getPort() < 1 || getPort() >= 65536) {
      return "Invalid value for the connection port.";
    }

    return null;
  }

  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("chromium_16_remote.png");
  }

  @Override
  public String getName() {
    return "Main";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    projectText.setText(dartLauncher.getProjectName());

    connectionTypesCombo.setText(dartLauncher.getConnectionType());

    hostText.setText(dartLauncher.getConnectionHost());
    portText.setText(Integer.toString(dartLauncher.getConnectionPort()));

    updateInstructionText();
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    dartLauncher.setProjectName(projectText.getText());

    dartLauncher.setConnectionType(connectionTypesCombo.getText());

    dartLauncher.setConnectionHost(hostText.getText());
    dartLauncher.setConnectionPort(getPort());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    dartLauncher.setProjectName("");

    dartLauncher.setConnectionType(DartLaunchConfigWrapper.CONNECTION_TYPES[0]);

    dartLauncher.setConnectionHost(DartLaunchConfigWrapper.DEFAULT_HOST);
    dartLauncher.setConnectionPort(DartLaunchConfigWrapper.DEFAULT_CHROME_PORT);
  }

  protected DartProject getDartProject() {
    String projectName = projectText.getText().trim();
    if (projectName.length() < 1) {
      return null;
    }
    return DartCore.create(getWorkspaceRoot()).getDartProject(projectName);
  }

  protected void handleBrowseButton() {
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

  private int getPort() {
    try {
      return Integer.parseInt(portText.getText());
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }

  private IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  private void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();

    updateInstructionText();
  }

  private void updateInstructionText() {
    String connectionType = connectionTypesCombo.getText();

    if (DartLaunchConfigWrapper.CONNECTION_TYPE_CHROME.equals(connectionType)) {
      connectionTypeInstructions.setText("Start Chrome with: chrome --remote-shell-port="
          + portText.getText());
    } else if (DartLaunchConfigWrapper.CONNECTION_TYPE_V8.equals(connectionType)) {
//    node --debug[=port] NodeApp.js
//      or
//    node --debug-brk[=port] NodeApp.js

      connectionTypeInstructions.setText("If using Node.js, start it using: node --debug="
          + portText.getText() + " NodeApp.js");
    } else {
      connectionTypeInstructions.setText("");
    }

    connectionTypeInstructions.getParent().layout();
  }
}
