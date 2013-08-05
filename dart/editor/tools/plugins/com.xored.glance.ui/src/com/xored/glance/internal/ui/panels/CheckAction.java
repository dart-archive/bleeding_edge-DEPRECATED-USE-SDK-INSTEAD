/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui.panels;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.ui.panels.SearchPanel;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * @author Yuri Strot
 */
public class CheckAction extends Action {

  private SearchPanel panel;
  private String name;

  public CheckAction(String name, String label, SearchPanel panel) {
    super(label, AS_CHECK_BOX);
    this.name = name;
    this.panel = panel;
    setChecked(getStore().getBoolean(name));
  }

  public IPreferenceStore getStore() {
    return GlancePlugin.getDefault().getPreferenceStore();
  }

  @Override
  public void run() {
    getStore().setValue(name, isChecked());
    panel.storeSettings();
  }
}
