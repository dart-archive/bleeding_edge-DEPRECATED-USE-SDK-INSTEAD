/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;
import com.google.dart.tools.ui.internal.cleanup.preference.ProfileManager.CustomProfile;

import java.util.Iterator;
import java.util.Map;

public class CleanUpProfileVersioner implements IProfileVersioner {

  public static final String PROFILE_KIND = "CleanUpProfile"; //$NON-NLS-1$

  private static final int VERSION_1 = 1; // 3.3M2
  private static final int VERSION_2 = 2; // 3.3M3 Added ORGANIZE_IMPORTS

  public static final int CURRENT_VERSION = VERSION_2;

  private static void updateFrom1To2(Map<String, String> settings) {
    CleanUpOptions defaultSettings = DartToolsPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(
        CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
    settings.put(CleanUpConstants.ORGANIZE_IMPORTS,
        defaultSettings.getValue(CleanUpConstants.ORGANIZE_IMPORTS));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jdt.internal.ui.preferences.cleanup.IProfileVersioner#getCurrentVersion()
   */
  @Override
  public int getCurrentVersion() {
    return CURRENT_VERSION;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jdt.internal.ui.preferences.cleanup.IProfileVersioner#getFirstVersion()
   */
  @Override
  public int getFirstVersion() {
    return VERSION_1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getProfileKind() {
    return PROFILE_KIND;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jdt.internal.ui.preferences.cleanup.IProfileVersioner#updateAndComplete(org.eclipse
   * .jdt.internal.ui.preferences.cleanup.ProfileManager.CustomProfile)
   */
  @Override
  public void update(CustomProfile profile) {
    final Map<String, String> oldSettings = profile.getSettings();
    Map<String, String> newSettings = updateAndComplete(oldSettings, profile.getVersion());
    profile.setVersion(CURRENT_VERSION);
    profile.setSettings(newSettings);
  }

  private Map<String, String> updateAndComplete(Map<String, String> oldSettings, int version) {
    final Map<String, String> newSettings = DartToolsPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(
        CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS).getMap();

    switch (version) {
      case VERSION_1:
        updateFrom1To2(oldSettings);
        //$FALL-THROUGH$
      default:
        for (final Iterator<String> iter = oldSettings.keySet().iterator(); iter.hasNext();) {
          final String key = iter.next();
          if (!newSettings.containsKey(key)) {
            continue;
          }

          final String value = oldSettings.get(key);
          if (value != null) {
            newSettings.put(key, value);
          }
        }

    }
    return newSettings;
  }

}
