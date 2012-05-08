/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.debug.ui.internal.DartUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.contextlaunching.ContextRunner;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.ui.actions.AbstractLaunchHistoryAction;
import org.eclipse.debug.ui.actions.LaunchAction;
import org.eclipse.debug.ui.actions.OpenLaunchDialogAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Menu;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstract superclass of the Run and Debug launch configuration actions.
 */
@SuppressWarnings("restriction")
public abstract class AbstractToolbarAction extends AbstractLaunchHistoryAction {

  public AbstractToolbarAction(String launchGroupIdentifier) {
    super(launchGroupIdentifier);
  }

  /**
   * Find an existing launch configurations, or create a new one, based on the active editor or
   * selection.
   */
  @Override
  public void run(IAction action) {
    ContextRunner.getDefault().launch(
        DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(
            getLaunchGroupIdentifier()));

//    ILaunchConfiguration configuration = getLastLaunch();
//    if (configuration == null) {
//      DebugUITools.openLaunchConfigurationDialogOnGroup(DebugUIPlugin.getShell(),
//          new StructuredSelection(), getLaunchGroupIdentifier());
//    } else {
//      DebugUITools.launch(configuration, getMode());
//    }
  }

  @Override
  protected final void fillMenu(Menu menu) {
    ILaunchConfiguration[] launches = new ILaunchConfiguration[0];

    try {
      List<ILaunchConfiguration> configs = new ArrayList<ILaunchConfiguration>();

      for (ILaunchConfiguration config : LaunchConfigurationManager.filterConfigs(DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations())) {
        if (config.getType().supportsMode(getMode())) {
          configs.add(config);
        }
      }

      launches = configs.toArray(new ILaunchConfiguration[configs.size()]);
    } catch (CoreException ce) {
      DartUtil.logError(ce);
    }

    int accelerator = 1;

    for (int i = 0; i < launches.length; i++) {
      ILaunchConfiguration launch = launches[i];
      LaunchAction action = new LaunchAction(launch, getMode());
      addToMenu(menu, action, accelerator);
      accelerator++;
    }

    // Separator between the launch configurations and common actions
    if (menu.getItemCount() > 0) {
      addSeparator(menu);
    }

    addToMenu(menu, getOpenDialogAction(), -1);
  }

  protected IAction getOpenDialogAction() {
    OpenLaunchDialogAction action = new OpenLaunchDialogAction(getLaunchGroupIdentifier());

    action.setText("Manage " + action.getText());

    return action;
  }

}
