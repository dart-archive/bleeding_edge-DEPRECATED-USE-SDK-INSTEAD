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

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * An action to open the manage launch configurations dialog.
 */
@SuppressWarnings("restriction")
public class ManageLaunchesAction extends AbstractInstrumentedAction {
  private IWorkbenchWindow window;

  public ManageLaunchesAction(IWorkbenchWindow window) {
    this.window = window;

    setText("Manage Launches...");
    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/manage_launches.png"));
  }

  @Override
  public void run() {
    DebugUIPlugin.openLaunchConfigurationsDialog(window.getShell(), new StructuredSelection(),
        IDebugUIConstants.ID_RUN_LAUNCH_GROUP, true);

//    ManageLaunchesDialog dialog = new ManageLaunchesDialog(window);
//    dialog.open();
  }

}
