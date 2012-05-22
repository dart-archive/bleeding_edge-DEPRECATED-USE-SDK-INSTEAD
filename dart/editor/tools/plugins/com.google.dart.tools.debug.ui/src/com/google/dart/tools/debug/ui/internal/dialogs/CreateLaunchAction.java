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
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

import java.util.Arrays;
import java.util.Comparator;

/**
 * An action used to create a new launch configuration.
 */
public class CreateLaunchAction extends Action implements IWorkbenchWindowPulldownDelegate2,
    IMenuCreator {
  private Menu menu;
  private ManageLaunchesDialog launchConfigurationDialog;

  /**
   * Create a new CreateLaunchAction.
   */
  public CreateLaunchAction(ManageLaunchesDialog launchConfigurationDialog) {
    super("Create new launch", IAction.AS_DROP_DOWN_MENU);

    this.launchConfigurationDialog = launchConfigurationDialog;

    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/run_create.png"));

    setMenuCreator(this);
  }

  @Override
  public void dispose() {
    setMenu(null);
  }

  @Override
  public Menu getMenu(Control parent) {
    setMenu(new Menu(parent));
    fillMenu(menu);
    return menu;
  }

  @Override
  public Menu getMenu(Menu parent) {
    setMenu(new Menu(parent));
    fillMenu(menu);
    return menu;
  }

  @Override
  public void init(IWorkbenchWindow window) {

  }

  @Override
  public void run() {

  }

  @Override
  public void run(IAction action) {
    run();
  }

  @Override
  public void runWithEvent(Event event) {
    if (((event.stateMask & SWT.MOD1) > 0) && (event.type != SWT.KeyDown)) {
      // The menu was opened.
    } else {
      // If the user clicked on the button, open the menu.
      Widget widget = event.widget;

      if (widget instanceof ToolItem) {
        ToolItem toolItem = (ToolItem) widget;
        Listener[] listeners = toolItem.getListeners(SWT.Selection);
        if (listeners.length > 0) {
          Listener listener = listeners[0]; // should be only one listener
          // send an event to the widget to open the menu
          // see CommandContributionItem.openDropDownMenu(Event)
          Event e = new Event();
          e.type = SWT.Selection;
          e.widget = widget;
          e.detail = 4; // dropdown detail
          e.y = toolItem.getBounds().height; // position menu
          listener.handleEvent(e);
        }
      }
    }
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {

  }

  protected void create(ILaunchConfigurationType configType) {
    try {
      ILaunchConfigurationWorkingCopy wc = configType.newInstance(
          null,
          DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName("New launch"));

      wc.doSave();

      launchConfigurationDialog.selectLaunchConfiguration(wc.getName());
    } catch (CoreException exception) {
      DebugErrorHandler.errorDialog(
          launchConfigurationDialog.getShell(),
          "Error Created Launch",
          "Unable to create the selected launch: " + exception.toString(),
          exception);
    }
  }

  private void fillMenu(Menu menu) {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    // Iterate through all the launch configuration types and add them to the pulldown menu.
    for (final ILaunchConfigurationType configType : sort(manager.getLaunchConfigurationTypes())) {
      Action launchAction = new Action(
          "New " + configType.getName(),
          DebugUITools.getDefaultImageDescriptor(configType)) {
        @Override
        public void run() {
          create(configType);
        }
      };

      new ActionContributionItem(launchAction).fill(menu, -1);
    }
  }

  private void setMenu(Menu inMenu) {
    if (menu != null) {
      menu.dispose();
    }

    menu = inMenu;
  }

  private ILaunchConfigurationType[] sort(ILaunchConfigurationType[] configs) {
    Arrays.sort(configs, new Comparator<ILaunchConfigurationType>() {
      @Override
      public int compare(ILaunchConfigurationType config1, ILaunchConfigurationType config2) {
        return config1.getName().compareToIgnoreCase(config2.getName());
      }
    });

    return configs;
  }

}
