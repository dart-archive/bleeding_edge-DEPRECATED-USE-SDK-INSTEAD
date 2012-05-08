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

import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.ui.actions.AbstractInstrumentedAction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The abstract superclass of the run and debug actions.
 */
public abstract class DartAbstractAction extends AbstractInstrumentedAction implements
    IWorkbenchWindowPulldownDelegate2, IMenuCreator {

  private Menu menu;
  protected IWorkbenchWindow window;

  public DartAbstractAction(IWorkbenchWindow window, String name, int flags) {
    super(name, flags);

    this.window = window;

    if ((flags & IAction.AS_DROP_DOWN_MENU) != 0) {
      setMenuCreator(this);
    }
  }

  @Override
  public void dispose() {
    setMenu(null);
  }

  @Override
  public Menu getMenu(Control parent) {
    setMenu(new Menu(parent));
    fillMenu(menu);
    initMenu();
    return menu;
  }

  @Override
  public Menu getMenu(Menu parent) {
    setMenu(new Menu(parent));
    fillMenu(menu);
    initMenu();
    return menu;
  }

  @Override
  public void init(IWorkbenchWindow window) {

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
      run();
    }
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {

  }

  protected IWorkbenchWindow getWindow() {
    return window;
  }

  protected void launch(ILaunchConfiguration config) {
    boolean supportsDebug = false;

    try {
      supportsDebug = config.supportsMode(ILaunchManager.DEBUG_MODE);
    } catch (CoreException e) {

    }

    if (supportsDebug) {
      DebugUITools.launch(config, ILaunchManager.DEBUG_MODE);
    } else {
      DebugUITools.launch(config, ILaunchManager.RUN_MODE);
    }
  }

  protected void launch(ILaunchShortcut shortcut, ISelection selection) {
    shortcut.launch(selection, ILaunchManager.DEBUG_MODE);
  }

  private void fillMenu(Menu menu) {
    try {
      ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

      // Iterate through all the launch configurations and add them to the pulldown menu.
      for (final ILaunchConfiguration config : sort(manager.getLaunchConfigurations())) {
        Action launchAction = new Action(
            config.getName(),
            DebugUITools.getDefaultImageDescriptor(config)) {
          @Override
          public void run() {
            launch(config);
          }
        };

        new ActionContributionItem(launchAction).fill(menu, -1);
      }

      if (menu.getItemCount() > 0) {
        new Separator().fill(menu, -1);
      }

      new ActionContributionItem(new ManageLaunchesAction(window)).fill(menu, -1);
    } catch (CoreException exception) {
      DartUtil.logError(exception);
    }
  }

  private void initMenu() {

  }

  private void setMenu(Menu inMenu) {
    if (menu != null) {
      menu.dispose();
    }

    menu = inMenu;
  }

  private ILaunchConfiguration[] sort(ILaunchConfiguration[] configs) {
    Arrays.sort(configs, new Comparator<ILaunchConfiguration>() {
      @Override
      public int compare(ILaunchConfiguration config1, ILaunchConfiguration config2) {
        return config1.getName().compareToIgnoreCase(config2.getName());
      }
    });

    return configs;
  }

}
