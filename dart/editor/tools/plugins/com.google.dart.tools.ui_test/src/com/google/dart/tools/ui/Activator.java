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
package com.google.dart.tools.ui;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Activator extends AbstractUIPlugin implements IStartup {

  private static CountDownLatch EARLY_STARTUP_LATCH = new CountDownLatch(1);

  /**
   * Closes all editors.
   */
  public static void closeAllEditors() {
    // run event loop to allow any async's be executed
    while (Display.getCurrent().readAndDispatch()) {
      // do nothing
    }
    // close all editors
    IWorkbenchWindow window = getActiveWorkbenchWindow();
    if (window != null) {
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        page.closeAllEditors(false);
      }
    }
  }

  /**
   * Closes all {@link IViewPart}s.
   */
  public static void closeAllViews() {
    // run event loop to allow any async's be executed
    while (Display.getCurrent().readAndDispatch()) {
      // do nothing
    }
    // do close
    IWorkbenchWindow window = getActiveWorkbenchWindow();
    if (window != null) {
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        IViewReference[] viewReferences = page.getViewReferences();
        if (viewReferences.length != 0) {
          for (IViewReference viewReference : viewReferences) {
            page.hideView(viewReference);
          }
          waitEventLoop(100);
        }
      }
    }
  }

  /**
   * Move the Eclipse window to the specified location and wait for it to happen.
   */
  public static void setWindowLocation(int x, int y) {
    Shell shell = getActiveWorkbenchWindow().getShell();
    Point shellLocation = shell.getLocation();
    if (shellLocation.x != x || shellLocation.y != y) {
      Rectangle clientArea = Display.getDefault().getClientArea();
      shell.setBounds(x, y, clientArea.width - x, clientArea.height - 300);
      waitEventLoop(100, 10);
    }
  }

  /**
   * Waits given number of milliseconds and runs events loop every 1 millisecond. At least one
   * events loop will be executed.
   */
  public static void waitEventLoop(int time) {
    waitEventLoop(time, 0);
  }

  /**
   * Waits given number of milliseconds and runs events loop every <code>sleepMillis</code>
   * milliseconds. At least one events loop will be executed.
   */
  public static void waitEventLoop(int time, long sleepMillis) {
    long start = System.currentTimeMillis();
    do {
      try {
        Thread.sleep(sleepMillis);
      } catch (Throwable e) {
      }
      while (Display.getCurrent().readAndDispatch()) {
        // do nothing
      }
    } while (System.currentTimeMillis() - start < time);
  }

  /**
   * Wait until early startup has occurred or the specified time has elapsed.
   * 
   * @param milliseconds the maximum number of milliseconds to wait for early startup
   * @return {@code true} if early startup is complete, else {@code false}
   */
  public static boolean waitForEarlyStartup(long milliseconds) {
    boolean earlyStartupComplete = false;
    try {
      earlyStartupComplete = EARLY_STARTUP_LATCH.await(milliseconds, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      //$FALL-THROUGH$
    }
    return earlyStartupComplete;
  }

  private static IWorkbenchWindow getActiveWorkbenchWindow() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
  }

  @Override
  public void earlyStartup() {

//    Display.getDefault().asyncExec(new Runnable() {
//      @Override
//      public void run() {
//        setWindowLocation(450, 0);
//      }
//    });

    // Notify others that startup is complete
    EARLY_STARTUP_LATCH.countDown();
  }
}
