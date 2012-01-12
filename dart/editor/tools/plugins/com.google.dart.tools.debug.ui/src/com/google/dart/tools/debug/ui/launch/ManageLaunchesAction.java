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

package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.ui.actions.AbstractInstrumentedAction;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * An action to open the manage launch configurations dialog.
 */
public class ManageLaunchesAction extends AbstractInstrumentedAction {
  private IWorkbenchWindow window;

  public ManageLaunchesAction(IWorkbenchWindow window, boolean useCreateLaunchIcon) {
    this.window = window;

    setText("Manage Launches...");
    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor(useCreateLaunchIcon
        ? "obj16/create_launch.png" : "obj16/manage_launches.png"));
  }

  @Override
  public void run() {
    // TODO(devoncarew): open a dialog to manage existing launch configurations
    MessageDialog.openInformation(window.getShell(), "TODO", "ManageLaunchesAction.run()");
  }

}
