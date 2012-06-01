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

import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.util.AppSelectionDialog;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

/**
 * 
 */
public class DartServerMainTab extends AbstractLaunchConfigurationTab {
  private Text scriptText;
  private Text argsText;

  private Text heapText;
  private Button checkedModeButton;
  private Button enableDebuggingButton;

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
    GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 0, 4).applyTo(group);

    Label label = new Label(group, SWT.NONE);
    label.setText("Dart script:");

    scriptText = new Text(group, SWT.BORDER | SWT.SINGLE);
    scriptText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(scriptText);

    Button button = new Button(group, SWT.PUSH);
    button.setText("Browse...");
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
    label.setText("Script arguments:");
    label.pack();
    int labelWidth = label.getSize().x;
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

    argsText = new Text(group, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    argsText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).hint(400, 75).applyTo(
        argsText);

    // spacer
    label = new Label(group, SWT.NONE);

    // VM settings group
    group = new Group(composite, SWT.NONE);
    group.setText("VM settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).extendedMargins(0, 0, 0, 4).applyTo(group);

    checkedModeButton = new Button(group, SWT.CHECK);
    checkedModeButton.setText("Run in checked mode");
    GridDataFactory.swtDefaults().span(2, 1).applyTo(checkedModeButton);
    checkedModeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });

    enableDebuggingButton = new Button(group, SWT.CHECK);
    enableDebuggingButton.setText("Enable debugging");
    GridDataFactory.swtDefaults().span(2, 1).applyTo(enableDebuggingButton);
    enableDebuggingButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });

    label = new Label(group, SWT.NONE);
    label.setText("Heap (MB):");
    GridDataFactory.swtDefaults().hint(labelWidth, -1).applyTo(label);

    heapText = new Text(group, SWT.BORDER | SWT.SINGLE);
    heapText.setTextLimit(5);
    heapText.addModifyListener(textModifyListener);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    // check that the script name is not empty
    if (scriptText.getText().length() == 0) {
      return "A Dart script is required.";
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

    scriptText.setText(dartLauncher.getApplicationName());
    argsText.setText(dartLauncher.getArguments());

    checkedModeButton.setSelection(dartLauncher.getCheckedMode());
    heapText.setText(dartLauncher.getHeapMB());
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

    dartLauncher.setApplicationName(scriptText.getText());
    dartLauncher.setArguments(argsText.getText());

    dartLauncher.setCheckedMode(checkedModeButton.getSelection());
    dartLauncher.setHeapMB(heapText.getText());

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
    dartLauncher.setHeapMB("");
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
      String pathStr = ((IFile) results[0]).getFullPath().toPortableString();

      scriptText.setText(pathStr);
    }
  }

  private void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }

}
