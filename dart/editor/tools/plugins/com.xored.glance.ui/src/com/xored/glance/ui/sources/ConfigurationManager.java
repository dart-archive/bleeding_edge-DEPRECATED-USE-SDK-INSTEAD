/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.sources;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.internal.ui.preferences.IPreferenceConstants;

public final class ConfigurationManager {

  private static ConfigurationManager INSTANCE;

  public static ConfigurationManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ConfigurationManager();
    }
    return INSTANCE;
  }

  private ConfigurationManager() {
  }

  public int getMaxIndexingDepth() {
    return GlancePlugin.getDefault().getPreferenceStore().getInt(
        IPreferenceConstants.PANEL_MAX_INDEXING_DEPTH);
  }

  public boolean incremenstalSearch() {
    return GlancePlugin.getDefault().getPreferenceStore().getBoolean(
        IPreferenceConstants.SEARCH_INCREMENTAL);
  }
}
