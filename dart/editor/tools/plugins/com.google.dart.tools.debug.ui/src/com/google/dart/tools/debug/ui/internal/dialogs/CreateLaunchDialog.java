/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.dialogs;

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * TODO(devoncarew):
 */
public class CreateLaunchDialog extends TitleAreaDialog {
  private List<ILaunchConfigurationType> launchTypes;

  public CreateLaunchDialog(IWorkbenchWindow window) {
    super(window.getShell());

    initLaunchInfo();
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText(Messages.CreateLaunchDialog_createLaunch);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    createButton(parent, IDialogConstants.OK_ID, Messages.CreateLaunchDialog_launchRun, true);
    Button debugButton = createButton(parent, IDialogConstants.CLIENT_ID,
        Messages.CreateLaunchDialog_launchDebug, false);
    debugButton.setEnabled(false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite contents = (Composite) super.createDialogArea(parent);

    setTitle(Messages.CreateLaunchDialog_createNewLaunch);
    setMessage("TODO: describe what to do here");
    setTitleImage(DartDebugUIPlugin.getImage("wiz/run_wiz.png")); //$NON-NLS-1$

    Composite composite = new Composite(contents, SWT.NONE);
    GridLayoutFactory.fillDefaults().margins(12, 6).applyTo(composite);
    createDialogUI(composite);

    return contents;
  }

  void initLaunchInfo() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    launchTypes = new ArrayList<ILaunchConfigurationType>(
        Arrays.asList(manager.getLaunchConfigurationTypes()));

    Collections.sort(launchTypes, new Comparator<ILaunchConfigurationType>() {
      @Override
      public int compare(ILaunchConfigurationType launch0, ILaunchConfigurationType launch1) {
        return launch0.getName().compareToIgnoreCase(launch1.getName());
      }
    });
  }

  private void createDialogUI(Composite composite) {
    List<String> launchNames = new ArrayList<String>();

    for (ILaunchConfigurationType launch : launchTypes) {
      launchNames.add(launch.getName());
    }

    Combo combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
    combo.setItems(launchNames.toArray(new String[launchNames.size()]));
    combo.select(0);

    // TODO:

  }

}
