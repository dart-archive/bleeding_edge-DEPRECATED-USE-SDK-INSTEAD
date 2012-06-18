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
package com.google.dart.tools.ui.omni;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Searchbox-related utilities.
 *
 */
public class SearchBoxUtils {
  
  /**
   * Get the active shell from the workbench (if none is found, fall back
   * to a sensible default).
   * 
   * @return the default/active workbench shell
   */
  public static Shell getActiveWorkbenchShell() { 
    IWorkbenchWindow activeWindow = getActiveWindow();
    if (activeWindow != null) {
      return activeWindow.getShell();
    }
    return getActiveShell();
  }

  private static Shell getActiveShell() {
    Shell shell = Display.getDefault().getActiveShell();
    if (shell != null) {
      return shell;
    }
    //if there's no active shell, grab the first one
    Shell[] shells = Display.getDefault().getShells();
    if (shells.length > 0) {
      return shells[0];
    }
    //shouldn't get here
    return null;
  }

  private static IWorkbenchWindow getActiveWindow() {
    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (activeWindow == null) {
      IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
      if (windows.length > 0) {
        activeWindow = windows[0];
      }
    }
    return activeWindow;
  }
   
}
