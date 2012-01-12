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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

/**
 * The abstract superclass of the run and debug actions.
 */
public abstract class DartAbstractAction extends AbstractInstrumentedAction implements
    IWorkbenchWindowPulldownDelegate2, IMenuCreator {

  private Menu menu;
  private IWorkbenchWindow window;

  public DartAbstractAction(IWorkbenchWindow window, String name, int flags) {
    super(name, flags);

    this.window = window;

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

  /**
   * @return implemented by subclasses; either "run" or "debug"
   */
  protected abstract String getLaunchMode();

  protected IWorkbenchWindow getWindow() {
    return window;
  }

  protected boolean supportsLaunchConfig(ILaunchConfiguration launch) {
    try {
      return launch.getType().supportsMode(getLaunchMode());
    } catch (CoreException e) {
      return false;
    }
  }

  private void fillMenu(Menu menu) {
    try {
      ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

      // Iterate through all the launch configurations and add them to the pulldown menu.
      for (final ILaunchConfiguration launch : manager.getLaunchConfigurations()) {
        if (supportsLaunchConfig(launch)) {
          Action launchAction = new Action(launch.getName(),
              DebugUITools.getDefaultImageDescriptor(launch)) {
            @Override
            public void run() {
              DebugUITools.launch(launch, getLaunchMode());
            }
          };

          new ActionContributionItem(launchAction).fill(menu, -1);
        }
      }
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

}
