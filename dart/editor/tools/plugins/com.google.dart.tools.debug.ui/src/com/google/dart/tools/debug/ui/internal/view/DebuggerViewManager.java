/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.ui.internal.view;

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.contexts.ISuspendTrigger;
import org.eclipse.debug.ui.contexts.ISuspendTriggerListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Manages the Debugger view during the debug session
 */
public class DebuggerViewManager implements ILaunchListener, ISuspendTriggerListener {

  private static DebuggerViewManager manager = new DebuggerViewManager();

  public static DebuggerViewManager getDefault() {
    return manager;
  }

  @Override
  public void launchAdded(ILaunch launch) {
    try {
      if (launch.getLaunchConfiguration().getType().getIdentifier().startsWith("com.google")
          && launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE)) {

        //  add the suspend trigger listener 
        ISuspendTrigger trigger = (ISuspendTrigger) launch.getAdapter(ISuspendTrigger.class);
        if (trigger != null) {
          trigger.addSuspendTriggerListener(this);
        }

        // open debugger view
        Display.getDefault().asyncExec(new Runnable() {

          @Override
          public void run() {
            try {
              IWorkbenchWindow window = DartDebugUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
              if (window == null) {
                IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
                if (windows.length > 0) {
                  IWorkbenchPage[] pages = windows[0].getPages();
                  if (pages.length > 0) {
                    pages[0].showView("com.google.dart.tools.debug.debuggerView"); //$NON-NLS-N$
                  }
                }
              } else {
                window.getActivePage().showView("com.google.dart.tools.debug.debuggerView"); //$NON-NLS-N$
              }
            } catch (PartInitException e) {
              DartUtil.logError(e);
            }
          }
        });

      }
    } catch (CoreException e) {
      DartUtil.logError(e);
    }

  }

  @Override
  public void launchChanged(ILaunch launch) {
  }

  @Override
  public void launchRemoved(ILaunch launch) {
    try {
      if (launch.getLaunchConfiguration().getType().getIdentifier().startsWith("com.google") //$NON-NLS-N$
          && launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE)) {
        ISuspendTrigger trigger = (ISuspendTrigger) launch.getAdapter(ISuspendTrigger.class);
        if (trigger != null) {
          trigger.removeSuspendTriggerListener(this);
        }
      }
    } catch (CoreException e) {
      DartUtil.logError(e);
    }
  }

  @Override
  public void suspended(ILaunch launch, Object context) {
    Display.getDefault().asyncExec(new Runnable() {

      @Override
      public void run() {
        IWorkbenchWindow window = DartDebugUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
          window = getWindowWithView("com.google.dart.tools.debug.debuggerView"); //$NON-NLS-N$
        }
        window.getShell().forceActive();
        IViewReference viewReference = window.getActivePage().findViewReference(
            "com.google.dart.tools.debug.debuggerView"); //$NON-NLS-N$
        window.getActivePage().activate(viewReference.getPart(true));

      }
    });

  }

  private IWorkbenchWindow getWindowWithView(String viewId) {
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    if (windows.length > 0) {
      for (int i = 0; i < windows.length; i++) {
        IWorkbenchPage[] pages = windows[i].getPages();
        for (IWorkbenchPage page : pages) {
          if (page.findView(viewId) != null) {
            return windows[i];
          }
        }

      }
    }
    return null;
  }

}
