/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class BusyIndicatorUtils {

  static final String BUSYID_NAME = "SWT BusyIndicator"; //$NON-NLS-1$
  static final Integer NO_BUSY = new Integer(0);

  public static void withoutIndicator(Display display, Runnable runnable) {
    if (runnable == null) {
      SWT.error(SWT.ERROR_NULL_ARGUMENT);
    }
    if (display == null) {
      display = Display.getCurrent();
      if (display == null) {
        runnable.run();
        return;
      }
    }

    Shell[] shells = display.getShells();
    for (int i = 0; i < shells.length; i++) {
      Integer id = (Integer) shells[i].getData(BUSYID_NAME);
      if (id == null) {
        shells[i].setData(BUSYID_NAME, NO_BUSY);
      }
    }

    try {
      runnable.run();
    } finally {
      shells = display.getShells();
      for (int i = 0; i < shells.length; i++) {
        Integer id = (Integer) shells[i].getData(BUSYID_NAME);
        if (id == NO_BUSY) {
          shells[i].setData(BUSYID_NAME, null);
        }
      }
    }
  }

}
