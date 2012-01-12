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

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A toolbar action to enumerate a launch debug launch configurations.
 */
public class DartRunAction extends DartAbstractAction {

  public DartRunAction(IWorkbenchWindow window) {
    this(window, false);
  }

  public DartRunAction(IWorkbenchWindow window, boolean noMenu) {
    super(window, "Run", noMenu ? IAction.AS_UNSPECIFIED : IAction.AS_DROP_DOWN_MENU);

    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/run_exc.gif"));
  }

  @Override
  public void run() {
    // TODO(devoncarew): choose the right launch configuration to run, or open a dialog to create one.
    MessageDialog.openInformation(getWindow().getShell(), "TODO", "DartRunAction.run()");
  }

  @Override
  protected String getLaunchMode() {
    return ILaunchManager.RUN_MODE;
  }

}
