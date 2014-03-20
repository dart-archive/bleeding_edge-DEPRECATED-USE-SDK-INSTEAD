/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.dartium;

import com.google.dart.tools.debug.core.dartium.DartiumDebugTarget;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import java.io.IOException;

/**
 * A class to help manage the devtools disconnect / re-connect process.
 */
public class DevToolsDisconnectManager {
  final DartiumDebugTarget target;

  private Display display;
  private Shell shell;
  private ShellListener shellListener;
  private ILaunchListener launchListener;

  public DevToolsDisconnectManager(DartiumDebugTarget target) {
    this.target = target;
    this.display = Display.getDefault();

    shellListener = new ShellAdapter() {
      @Override
      public void shellActivated(ShellEvent e) {
        handleShellActivated();
      }
    };

    launchListener = new ILaunchListener() {
      @Override
      public void launchAdded(ILaunch launch) {
        handleLaunchAdded(launch);
      }

      @Override
      public void launchChanged(ILaunch launch) {

      }

      @Override
      public void launchRemoved(ILaunch launch) {
        handleLaunchRemoved(launch);
      }
    };

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        shell.addShellListener(shellListener);
      };
    });

    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchListener);

    printToConsole("<devtools opened - debugger connection paused>");
  }

  void dispose() {
    if (!display.isDisposed() && launchListener != null) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchListener);
      launchListener = null;

      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          shell.removeShellListener(shellListener);
          shellListener = null;
        }
      });
    }
  }

  private void handleLaunchAdded(ILaunch launch) {
    if (isDartLaunch(launch)) {
      dispose();
    }
  }

  private void handleLaunchRemoved(ILaunch launch) {
    if (launch == target.getLaunch()) {
      dispose();
    }
  }

  private void handleShellActivated() {
    if (display.isDisposed()) {
      return;
    }

    if (target.getProcess().isTerminated()) {
      dispose();
      return;
    }

    try {
      target.reconnect();

      printToConsole("<debugger connection resumed>");

      dispose();
    } catch (IOException e) {
      // Ignore this exception - devtools is still active.

    }
  }

  private boolean isDartLaunch(ILaunch launch) {
    try {
      return launch.getLaunchConfiguration().getType().getIdentifier().startsWith("com.google");
    } catch (CoreException e) {
      return false;
    }
  }

  private void printToConsole(String message) {
    target.writeToStdout(message);
  }
}
