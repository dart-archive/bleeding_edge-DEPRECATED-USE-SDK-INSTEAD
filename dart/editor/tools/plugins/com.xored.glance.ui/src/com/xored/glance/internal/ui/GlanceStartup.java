/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui;

import com.xored.glance.internal.ui.preferences.IPreferenceConstants;
import com.xored.glance.internal.ui.search.SearchManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

public class GlanceStartup implements IStartup, IPreferenceConstants {

  @Override
  public void earlyStartup() {
    IPreferenceStore store = GlancePlugin.getDefault().getPreferenceStore();
    if (store.getBoolean(PANEL_STARTUP)) {
      PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
        @Override
        public void run() {
          SearchManager.getIntance().startup();
        }
      });
    }
  }
}
