/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.preferences;

import com.xored.glance.internal.ui.GlancePlugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * @author Yuri Strot
 */
public class GlancePreferenceInitializer extends AbstractPreferenceInitializer implements
    IPreferenceConstants {

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore preferences = GlancePlugin.getDefault().getPreferenceStore();
    preferences.setDefault(SEARCH_CASE_SENSITIVE, false);
    preferences.setDefault(SEARCH_CAMEL_CASE, false);
    preferences.setDefault(SEARCH_REGEXP, false);
    preferences.setDefault(SEARCH_WORD_PREFIX, false);

    preferences.setDefault(PANEL_DIRECTIONS, true);
    preferences.setDefault(PANEL_STATUS_LINE, true);
    preferences.setDefault(PANEL_CLOSE, true);
    preferences.setDefault(PANEL_TEXT_SIZE, 30);
    preferences.setDefault(PANEL_LINK, true);
    preferences.setDefault(PANEL_STARTUP, false);
    preferences.setDefault(PANEL_AUTO_INDEXING, false);
    preferences.setDefault(SEARCH_INCREMENTAL, true);
    preferences.setDefault(PANEL_MAX_INDEXING_DEPTH, 4);
  }

}
