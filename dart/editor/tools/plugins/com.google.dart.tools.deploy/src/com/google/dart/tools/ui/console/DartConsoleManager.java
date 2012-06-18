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

package com.google.dart.tools.ui.console;

import com.google.dart.tools.deploy.Activator;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to manage opening and activating DartConsoleView classes in response to lifecycle events
 * from IConsole objects.
 */
@SuppressWarnings("restriction")
public class DartConsoleManager implements IConsoleListener {
  private static DartConsoleManager manager;

  public static DartConsoleManager getManager() {
    return manager;
  }

  public static void initialize() {
    if (manager == null) {
      IConsoleManager replacementManager = new ReplacementConsoleManager();

      setField(ConsolePlugin.getDefault(), "fConsoleManager", replacementManager);

      manager = new DartConsoleManager();
    }
  }

  public static void shutdown() {
    if (manager != null) {
      manager.dispose();
      manager = null;
    }
  }

  private static void setField(Object object, String fieldName, Object value) {
    try {
      Field field = object.getClass().getDeclaredField(fieldName);

      field.setAccessible(true);

      field.set(object, value);
    } catch (Throwable e) {
      Activator.logError(e);
    }
  }

  private List<DartConsoleView> consoleViews = new ArrayList<DartConsoleView>();

  private DartConsoleManager() {
    getConsoleManager().addConsoleListener(this);
  }

  @Override
  public void consolesAdded(IConsole[] consoles) {
    // If the new console is a process console, create a cooresponding view immediately - don't wait
    // for the first write to stdout.
    for (IConsole console : consoles) {
      if (console instanceof ProcessConsole) {
        warnOfContentChange(console);
      }
    }
  }

  @Override
  public void consolesRemoved(IConsole[] consoles) {

  }

  protected void consoleViewClosed(DartConsoleView view) {
    consoleViews.remove(view);
  }

  protected void consoleViewOpened(DartConsoleView view) {
    consoleViews.add(view);
  }

  protected void warnOfContentChange(final IConsole console) {
    if (console == null) {
      return;
    }

    if (consoleTerminated(console)) {
      return;
    }

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        boolean found = false;

        for (DartConsoleView view : consoleViews) {
          if (view == null) {
            continue;
          }

          if (console.equals(view.getConsole())) {
            found = true;

            view.warnOfContentChange(console);
          }
        }

        if (!found) {
          createConsole(console);
        }
      }
    });
  }

  private boolean consoleTerminated(IConsole console) {
    if (console instanceof ProcessConsole) {
      IProcess process = ((ProcessConsole) console).getProcess();

      if (process != null) {
        return process.isTerminated();
      }
    }

    return false;
  }

  private void createConsole(IConsole console) {
    // There is no view open showing the given console, so:

    // Find a dead console and recycle it.
    DartConsoleView view = findDeadConsoleView();

    if (view != null) {
      view.display(console);

      view.warnOfContentChange(console);
    } else {
      // Else create a new console.
      try {
        if (PlatformUI.getWorkbench() != null) {
          view = (DartConsoleView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
              DartConsoleView.VIEW_ID, createViewId(console), IWorkbenchPage.VIEW_VISIBLE);

          view.display(console);

          view.warnOfContentChange(console);
        }
      } catch (PartInitException e) {
        Activator.logError(e);
      }
    }
  }

  private String createViewId(IConsole console) {
    return Integer.toString(System.identityHashCode(console));
  }

  private void dispose() {
    if (getConsoleManager() != null) {
      getConsoleManager().removeConsoleListener(this);
    }
  }

  private DartConsoleView findDeadConsoleView() {
    for (DartConsoleView view : consoleViews) {
      if (view.isDead()) {
        return view;
      }
    }

    return null;
  }

  private IConsoleManager getConsoleManager() {
    return ConsolePlugin.getDefault().getConsoleManager();
  }

}
