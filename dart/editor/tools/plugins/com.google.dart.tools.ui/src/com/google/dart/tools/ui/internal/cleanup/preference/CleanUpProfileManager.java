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
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import java.util.ArrayList;
import java.util.List;

public class CleanUpProfileManager extends ProfileManager {

  public static KeySet[] KEY_SETS = {new KeySet(DartUI.ID_PLUGIN, new ArrayList<String>(
      DartToolsPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(
          CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS).getKeys()))};

  private final PreferencesAccess fPreferencesAccess;

  public CleanUpProfileManager(List<Profile> profiles, IScopeContext context,
      PreferencesAccess preferencesAccess, IProfileVersioner profileVersioner) {
    super(profiles, context, preferencesAccess, profileVersioner, KEY_SETS,
        CleanUpConstants.CLEANUP_PROFILE, CleanUpConstants.CLEANUP_SETTINGS_VERSION_KEY);
    fPreferencesAccess = preferencesAccess;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jdt.internal.ui.preferences.cleanup.ProfileManager#getDefaultProfile()
   */
  @Override
  public Profile getDefaultProfile() {
    return getProfile(CleanUpConstants.DEFAULT_PROFILE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateProfilesWithName(String oldName, Profile newProfile, boolean applySettings) {
    super.updateProfilesWithName(oldName, newProfile, applySettings);

    IEclipsePreferences node = fPreferencesAccess.getInstanceScope().getNode(DartUI.ID_PLUGIN);
    String name = node.get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
    if (name != null && name.equals(oldName)) {
      if (newProfile == null) {
        node.remove(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE);
      } else {
        node.put(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, newProfile.getID());
      }
    }
  }

}
