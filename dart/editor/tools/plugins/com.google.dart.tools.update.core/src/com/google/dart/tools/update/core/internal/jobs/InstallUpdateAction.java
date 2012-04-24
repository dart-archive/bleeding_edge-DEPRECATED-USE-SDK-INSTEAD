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
package com.google.dart.tools.update.core.internal.jobs;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * An action that installs an available Dart Editor update.
 */
public class InstallUpdateAction extends Action {

  private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$
  private static final String PROP_EXIT_DATA = "eclipse.exitdata"; //$NON-NLS-1$  
  private static final String PROP_VM = "eclipse.vm"; //$NON-NLS-1$
  private static final String PROP_COMMANDS = "eclipse.commands"; //$NON-NLS-1$
  private static final String PROP_VMARGS = "eclipse.vmargs"; //$NON-NLS-1$
  private static final String CMD_VMARGS = "-vmargs"; //$NON-NLS-1$

  private static final String NEW_LINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$

  @Override
  public void run() {
    applyUpdate();
    restart();
  }

  private void applyUpdate() {
    //TODO (pquitslund): implement
  }

  private String buildCommandLine() {
    String property = System.getProperty(PROP_VM);
    if (property == null) {
      //TODO (pquitslund): handle error
//      MessageDialog.openError(window.getShell(),
//          IDEWorkbenchMessages.OpenWorkspaceAction_errorTitle,
//          NLS.bind(IDEWorkbenchMessages.OpenWorkspaceAction_errorMessage, PROP_VM));
      return null;
    }

    StringBuffer result = new StringBuffer(512);
    result.append(property);
    result.append(NEW_LINE);

    // append the vmargs and commands. Assume that these already end in \n
    String vmargs = System.getProperty(PROP_VMARGS);
    if (vmargs != null) {
      result.append(vmargs);
    }

    //TODO (pquitslund): where does this really belong?
    result.append("-Declipse.refreshBundles=true");
    result.append(NEW_LINE);

    property = System.getProperty(PROP_COMMANDS);
    if (property != null) {
      result.append(property);
    }

    // put the vmargs back at the very end (the eclipse.commands property
    // already contains the -vm arg)
    if (vmargs != null) {
      result.append(CMD_VMARGS);
      result.append(NEW_LINE);
      result.append(vmargs);
    }

    return result.toString();
  }

  private void restart() {

    String commandLine = buildCommandLine();

    System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
    System.setProperty(PROP_EXIT_DATA, commandLine);

    PlatformUI.getWorkbench().restart();
  }
}
