/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.utils;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * @author Yuri Strot
 */
public class UIUtils {

  public static void asyncExec(final Control control, final Runnable runnable) {
    if (control != null && !control.isDisposed()) {
      control.getDisplay().asyncExec(new Runnable() {
        @Override
        public void run() {
          if (!control.isDisposed()) {
            runnable.run();
          }
        }
      });
    }
  }

  public static void asyncExec(final Display display, final Runnable runnable) {
    if (display != null && !display.isDisposed()) {
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          if (!display.isDisposed()) {
            runnable.run();
          }
        }
      });
    }
  }

  public static Display getDisplay() {
    return PlatformUI.getWorkbench().getDisplay();
  }

  public static void syncExec(final Display display, final Runnable runnable) {
    if (display != null && !display.isDisposed()) {
      display.syncExec(new Runnable() {
        @Override
        public void run() {
          if (!display.isDisposed()) {
            runnable.run();
          }
        }
      });
    }
  }

}
