/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.debug.ui.internal;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.util.BrowserManager;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * This class provides utilities for clients of the debug UI.
 */
public class DartDebugUITools {
  private static class RunAppMessageDialog extends MessageDialog {

    private Button button;

    public RunAppMessageDialog(Shell parentShell, String dialogTitle, String dialogMessage) {
      super(parentShell, dialogTitle, null, dialogMessage, MessageDialog.CONFIRM, new String[] {
          IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
    }

    @Override
    protected Control createCustomArea(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
      layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
      layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
      composite.setLayout(layout);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      button = new Button(composite, SWT.CHECK);
      button.setText("Do not show this dialog again");
      button.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          DartDebugCorePlugin.getPlugin().setShowRunResumeDialogPref(!button.getSelection());
        }
      });
      return composite;
    }

    @Override
    protected boolean customShouldTakeFocus() {
      return false;
    }

  }

  /**
   * Launches the given launch configuration in the specified mode in a background Job with progress
   * reported via the Job. Exceptions are reported in the Progress view.
   * 
   * @param configuration the configuration to launch
   * @param mode launch mode
   */
  public static void launch(ILaunchConfiguration config, String mode) {

    boolean doLaunch = true;

    if (DartDebugCorePlugin.getPlugin().canShowRunResumeDialog()) {

      boolean isAppRunning = false;
      DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);
      ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
      ILaunch browserLaunch = BrowserManager.currentLaunch;
      if (browserLaunch != null && config.equals(browserLaunch.getLaunchConfiguration())) {
        isAppRunning = true;
      } else {
        for (ILaunch launch : launches) {
          if (!launch.isTerminated()) {
            DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(
                launch.getLaunchConfiguration());
            if (wrapper.getApplicationResource().equals(launchWrapper.getApplicationResource())) {
              isAppRunning = true;
              break;
            }
          }
        }
      }
      if (isAppRunning) {
        int returnCode = new RunAppMessageDialog(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            "Launching " + config.getName(),
            "Launching " + config.getName() + " will interrupt the current application session."
                + "\nDo you wish to continue?").open();
        if (returnCode != 0) {
          doLaunch = false;
        }
      }
    }

    if (doLaunch) {
      // This waits for all build jobs to complete before launching
      DebugUITools.launch(config, mode);
    }
  }

}
