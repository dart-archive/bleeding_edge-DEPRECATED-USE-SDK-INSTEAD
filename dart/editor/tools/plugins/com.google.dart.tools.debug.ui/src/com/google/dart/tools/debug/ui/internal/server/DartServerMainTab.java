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

package com.google.dart.tools.debug.ui.internal.server;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.util.AppSelectionDialog;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

/**
 * 
 */
public class DartServerMainTab extends AbstractLaunchConfigurationTab {
  private Text scriptText;
  private Text argsText;

  private Button checkedModeButton;
  private Button enableDebuggingButton;
  private Text workingDirText;
  private IPath scriptPath;

  private ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  public DartServerMainTab() {

  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 1).applyTo(composite);

    // Application settings group
    Group group = new Group(composite, SWT.NONE);
    group.setText("Application");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
    ((GridLayout) group.getLayout()).marginBottom = 5;

    Label label = new Label(group, SWT.NONE);
    label.setText("Dart script:");

    scriptText = new Text(group, SWT.BORDER | SWT.SINGLE);
    scriptText.setEditable(false);
    scriptText.setCursor(composite.getShell().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    scriptText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(scriptText);

    Button button = new Button(group, SWT.PUSH);
    button.setText("Select...");
    PixelConverter converter = new PixelConverter(button);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleScriptBrowseButton();
      }
    });

    label = new Label(group, SWT.NONE);
    label.setText("Working directory:");

    workingDirText = new Text(group, SWT.BORDER | SWT.SINGLE);
    workingDirText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        workingDirText);

    button = new Button(group, SWT.PUSH);
    button.setText("Select...");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleCwdBrowseButton();
      }
    });

    label = new Label(group, SWT.NONE);
    label.setText("Script arguments:");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

    argsText = new Text(group, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    argsText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).hint(400, 74).applyTo(
        argsText);

    // spacer
    label = new Label(group, SWT.NONE);

    // VM settings group
    group = new Group(composite, SWT.NONE);
    group.setText("VM settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
    ((GridLayout) group.getLayout()).marginBottom = 4;

    checkedModeButton = new Button(group, SWT.CHECK);
    checkedModeButton.setText("Run in checked mode");
    checkedModeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });
    GridDataFactory.swtDefaults().span(2, 1).grab(true, false).applyTo(checkedModeButton);

    Link infoLink = new Link(group, SWT.NONE);
    infoLink.setText("<a href=\"" + DartDebugUIPlugin.CHECK_MODE_DESC_URL
        + "\">what is checked mode?</a>");
    infoLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ExternalBrowserUtil.openInExternalBrowser(DartDebugUIPlugin.CHECK_MODE_DESC_URL);
      }
    });

    enableDebuggingButton = new Button(group, SWT.CHECK);
    enableDebuggingButton.setText("Enable debugging");
    GridDataFactory.swtDefaults().span(3, 1).applyTo(enableDebuggingButton);
    enableDebuggingButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });

    if (DartCore.isWindows()) {
      enableDebuggingButton.setEnabled(false);

      String message = "not yet supported on win32";
      enableDebuggingButton.setText(enableDebuggingButton.getText() + " (" + message + ")");
    }

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    // check that the script name is not empty
    if (scriptText.getText().length() == 0) {
      return "Please select a Dart script.";
    }

    return null;
  }

  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("obj16/osprc_obj.png");
  }

  @Override
  public String getMessage() {
    return "Create a configuration to launch a Dart application on the command line";
  }

  @Override
  public String getName() {
    return "Main";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    scriptPath = new Path(dartLauncher.getApplicationName());
    IResource resource = dartLauncher.getApplicationResource();
    if (resource != null) {
      scriptText.setText(resource.getProjectRelativePath().toPortableString());
    }
    workingDirText.setText(dartLauncher.getWorkingDirectory());
    argsText.setText(dartLauncher.getArguments());

    checkedModeButton.setSelection(dartLauncher.getCheckedMode());
    if (enableDebuggingButton != null) {
      enableDebuggingButton.setSelection(dartLauncher.getEnableDebugging());
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    dartLauncher.setApplicationName(scriptPath.toPortableString());
    dartLauncher.setWorkingDirectory(workingDirText.getText());
    dartLauncher.setArguments(argsText.getText());

    dartLauncher.setCheckedMode(checkedModeButton.getSelection());

    if (enableDebuggingButton != null) {
      dartLauncher.setEnableDebugging(enableDebuggingButton.getSelection());
    }
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    dartLauncher.setApplicationName("");
    dartLauncher.setArguments("");

    dartLauncher.setCheckedMode(false);
  }

  protected void handleCwdBrowseButton() {
    DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.APPLICATION_MODAL | SWT.OPEN);
    dialog.setText("Select the Working Directory");

    String path = dialog.open();

    if (path != null) {
      workingDirText.setText(path);
    }
  }

  protected void handleScriptBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    AppSelectionDialog dialog = new AppSelectionDialog(getShell(), workspace.getRoot());
    dialog.setTitle("Select a Dart script to run");
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION);
    IPath path = new Path(scriptText.getText());
    if (workspace.validatePath(path.toString(), IResource.FILE).isOK()) {
      IFile file = workspace.getRoot().getFile(path);
      if (file != null && file.exists()) {
        dialog.setInitialSelections(new Object[] {path});
      }
    }

    dialog.open();

    Object[] results = dialog.getResult();

    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
      IFile resource = (IFile) results[0];
      scriptPath = (resource.getFullPath());
      scriptText.setText(resource.getProjectRelativePath().toPortableString());
    }
  }

  private void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }

}
