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
package com.google.dart.tools.ui.test.model.internal.workbench;

import com.google.dart.tools.ui.test.runnable.Result;

import static com.google.dart.tools.ui.test.runnable.UIThreadRunnable.syncExec;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Workbench access helpers.
 */
public class WorkbenchFinder {

  public static IWorkbenchPage getActivePage() {
    IWorkbench workbench = getWorkbench();
    if (workbench == null) {
      return null;
    }
    IWorkbenchWindow window = getActiveWindow(workbench);
    if (window == null) {
      return null;
    }
    return window.getActivePage();
  }

  public static IWorkbenchWindow getActiveWindow() {
    return getActiveWindow(getWorkbench());
  }

  public static IWorkbenchWindow getActiveWindow(final IWorkbench workbench) {

    return syncExec(new Result<IWorkbenchWindow>() {
      @Override
      public IWorkbenchWindow run() {
        return workbench.getActiveWorkbenchWindow();
      }
    });

  }

  public static IWorkbench getWorkbench() {

    return syncExec(new Result<IWorkbench>() {
      @Override
      public IWorkbench run() {
        return PlatformUI.getWorkbench();
      }
    });

  }

}
