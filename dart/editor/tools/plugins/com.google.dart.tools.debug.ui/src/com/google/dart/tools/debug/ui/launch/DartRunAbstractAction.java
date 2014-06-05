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

import com.google.dart.tools.core.mobile.AndroidDebugBridge;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;
import com.google.dart.tools.debug.ui.internal.DebugInstrumentationUtilities;
import com.google.dart.tools.debug.ui.internal.dialogs.ManageLaunchesDialog;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;
import com.google.dart.tools.ui.actions.InstrumentedAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
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
public abstract class DartRunAbstractAction extends InstrumentedAction implements
    IWorkbenchWindowPulldownDelegate2, IMenuCreator {
  private static class LaunchConfigComparator implements Comparator<ILaunchConfiguration> {
    @Override
    public int compare(ILaunchConfiguration o1, ILaunchConfiguration o2) {
      DartLaunchConfigWrapper wrapper1 = new DartLaunchConfigWrapper(o1);
      DartLaunchConfigWrapper wrapper2 = new DartLaunchConfigWrapper(o2);

      long compare = wrapper2.getLastLaunchTime() - wrapper1.getLastLaunchTime();

      if (compare < 0) {
        return -1;
      }

      return compare == 0 ? 0 : 1;
    }
  }

  private static final int MAX_MENU_LENGTH = 10;

  private Menu menu;

  protected IWorkbenchWindow window;

  public DartRunAbstractAction(IWorkbenchWindow window, String name, int flags) {
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
  public void selectionChanged(IAction action, ISelection selection) {

  }

  protected abstract void doLaunch(UIInstrumentationBuilder instrumentation);

  @Override
  protected final void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    if (event == null) {
      doLaunch(instrumentation);
      return;
    }

    if (((event.stateMask & SWT.MOD1) > 0) && (event.type != SWT.KeyDown)) {
      instrumentation.metric("Skipped", "The Menu was opened");
      // The menu was opened.
    } else {
      doLaunch(instrumentation);
      return;

    }
  }

  protected IWorkbenchWindow getWindow() {
    return window;
  }

  protected void launch(ILaunchConfiguration config, UIInstrumentationBuilder instrumentation) {
    String mode = ILaunchManager.RUN_MODE;

    instrumentation.metric("Launch mode", mode);
    try {
      if (config.supportsMode(ILaunchManager.DEBUG_MODE)) {
        mode = ILaunchManager.DEBUG_MODE;
      }

      if (config.getType().getIdentifier().equals(DartDebugCorePlugin.DARTIUM_LAUNCH_CONFIG_ID)) {
        DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(config);
        DebugInstrumentationUtilities.recordLaunchConfiguration(launchConfig, instrumentation);

        launchConfig.markAsLaunched();
        LaunchUtils.clearDartiumConsoles();
      }

      LaunchUtils.launch(config, mode);
    } catch (CoreException e) {
      instrumentation.metric("Problem-Exception", e.getClass().getName());
      instrumentation.metric("Problem-Exception", e.toString());

      DartDebugCorePlugin.logError(e);
      DebugErrorHandler.errorDialog(
          window.getShell(),
          "Error Launching Application",
          e.getMessage(),
          e);
    }
  }

  protected void launch(ILaunchShortcut shortcut, ISelection selection,
      UIInstrumentationBuilder instrumentation) {
    instrumentation.record(selection);
    shortcut.launch(selection, ILaunchManager.DEBUG_MODE);
  }

  private void fillMenu(Menu menu) {
    ILaunchConfiguration[] launches = LaunchUtils.getAllLaunchesArray();

    Arrays.sort(launches, new LaunchConfigComparator());

    int count = Math.min(launches.length, MAX_MENU_LENGTH);

    for (int i = 0; i < count; i++) {
      final ILaunchConfiguration config = launches[i];

      InstrumentedAction launchAction = new InstrumentedAction(
          LaunchUtils.getLongLaunchName(config),
          DebugUITools.getDefaultImageDescriptor(config)) {
        @Override
        public void doRun(Event event, UIInstrumentationBuilder instrumentation) {

          // If this is a mobile launch and device is not connected or not authorized
          // then open launch dialog
          try {
            if (config.getType().getIdentifier().equals(DartDebugCorePlugin.MOBILE_LAUNCH_CONFIG_ID)) {
              if (!AndroidDebugBridge.getAndroidDebugBridge().isDeviceConnectedAndAuthorized()) {
                ManageLaunchesDialog.openAsync(window, config);
                return;
              }
            }
          } catch (CoreException e) {
            //$FALL-THROUGH$
          }

          launch(config, instrumentation);
        }
      };

      new ActionContributionItem(launchAction).fill(menu, -1);
    }

    if (menu.getItemCount() > 0) {
      new Separator().fill(menu, -1);
    }

    new ActionContributionItem(new ManageLaunchesAction(window)).fill(menu, -1);

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
