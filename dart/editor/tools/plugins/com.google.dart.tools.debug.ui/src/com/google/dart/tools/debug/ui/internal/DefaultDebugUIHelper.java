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

package com.google.dart.tools.debug.ui.internal;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.ui.internal.view.DebuggerView;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper to allow non-UI code to interact with the UI.
 */
public class DefaultDebugUIHelper extends DebugUIHelper {

  @Override
  public void activateApplication(File application, String name) {
    if (DartCore.isMac()) {
      activateApplicationMacOS(application);
    } else if (DartCore.isWindows()) {
      bringWindowToFrontWin32(".*" + name + "\\z");
    } else if (DartCore.isLinux()) {
      // This is not necessary on Linux.

    }
  }

  @Override
  public void showStatusLineMessage(final String message) {
    final Display display = Display.getDefault();

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (display.isDisposed()) {
          return;
        }

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        IViewPart part = page.findView(DebuggerView.ID);

        if (part != null) {
          IStatusLineManager manager = part.getViewSite().getActionBars().getStatusLineManager();

          manager.setMessage(message);
        }
      }
    });
  }

  protected void activateApplicationMacOS(File app) {
    String appleScript = "tell application \"" + app.getName() + "\" to activate";

    ProcessBuilder builder = new ProcessBuilder("osascript", "-e", appleScript);

    try {
      builder.start();
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

  protected void bringWindowToFrontWin32(final String regex) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          bringWindowToFrontWin32_impl(regex);
        } catch (Exception e) {
          DartDebugCorePlugin.logError(e);
        }
      }
    });
  }

  private void bringWindowToFrontWin32_impl(String regex) throws Exception {
    final int GW_HWNDNEXT = 0x2;

    Pattern pattern = Pattern.compile(regex);
    Number window = OS_GetActiveWindow();

    while (window.longValue() != 0) {
      int textLength = OS_GetWindowTextLength(window);

      if (textLength > 0) {
        String str = OS_GetWindowText(window, textLength);
        Matcher matcher = pattern.matcher(str);

        if (matcher.matches()) {
          OS_BringWindowToTop(window);

          return;
        }
      }

      window = OS_GetWindow(window, GW_HWNDNEXT);
    }
  }

  private Class<?> getOSClass() throws ClassNotFoundException {
    return Class.forName("org.eclipse.swt.internal.win32.OS");
  }

  private void OS_BringWindowToTop(Number window) throws Exception {
    if (window instanceof Integer) {
      Method method = getOSClass().getMethod("BringWindowToTop", int.class);
      method.invoke(null, window.intValue());
    } else {
      Method method = getOSClass().getMethod("BringWindowToTop", long.class);
      method.invoke(null, window.longValue());
    }
  }

  private Number OS_GetActiveWindow() throws Exception {
    return (Number) getOSClass().getMethod("GetActiveWindow").invoke(null);
  }

  private Number OS_GetWindow(Number window, int direction) throws Exception {
    if (window instanceof Integer) {
      Method method = getOSClass().getMethod("GetWindow", int.class, int.class);
      return (Number) method.invoke(null, window.intValue(), direction);
    } else {
      Method method = getOSClass().getMethod("GetWindow", long.class, int.class);
      return (Number) method.invoke(null, window.longValue(), direction);
    }
  }

  private String OS_GetWindowText(Number window, int textLength) throws Exception {
    Class<?> tcharClass = Class.forName("org.eclipse.swt.internal.win32.TCHAR");
    Constructor<?> tcharCtor = tcharClass.getConstructor(int.class, int.class);

    Object tchar = tcharCtor.newInstance(0, textLength + 2);

    if (window instanceof Integer) {
      Method method = getOSClass().getMethod("GetWindowText", int.class, tcharClass, int.class);
      method.invoke(null, window.intValue(), tchar, textLength + 1);
    } else {
      Method method = getOSClass().getMethod("GetWindowText", long.class, tcharClass, int.class);
      method.invoke(null, window.longValue(), tchar, textLength + 1);
    }

    Method tcharToString = tcharClass.getMethod("toString", int.class, int.class);
    return (String) tcharToString.invoke(tchar, 0, textLength);
  }

  private int OS_GetWindowTextLength(Number window) throws Exception {
    if (window instanceof Integer) {
      Method method = getOSClass().getMethod("GetWindowTextLength", int.class);
      return (Integer) method.invoke(null, window.intValue());
    } else {
      Method method = getOSClass().getMethod("GetWindowTextLength", long.class);
      return (Integer) method.invoke(null, window.longValue());
    }
  }

}
