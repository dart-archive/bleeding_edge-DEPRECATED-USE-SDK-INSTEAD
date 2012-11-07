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
package com.google.dart.tools.ui.theme.mapper;

import com.google.dart.tools.deploy.Activator;
import com.google.dart.tools.ui.theme.ColorThemeSetting;
import com.google.dart.tools.ui.theme.preferences.PreviewPreferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

import java.util.Map;

/**
 * Maps color themes to Eclipse preferences.
 * 
 * @see com.github.eclipsecolortheme.mapper.ThemePreferenceMapper
 */
public abstract class ThemePreferenceMapper {

  /** The associated Eclipse preferences. */
  protected IEclipsePreferences preferences;

  /** Copy of the preferences used to update the preview. */
  private IEclipsePreferences previewPreferences;

  /**
   * Creates a new mapper.
   */
  public ThemePreferenceMapper() {
  }

  /**
   * Clears the associated Eclipse preferences. This resets every preference to its default value.
   */
  public abstract void clear();

  /**
   * Writes and applies the modified preferences.
   * 
   * @throws BackingStoreException
   */
  public void flush() throws BackingStoreException {
    preferences.flush();
  }

  public IEclipsePreferences getPreviewPreferences() {
    return previewPreferences;
  }

  /**
   * Maps the {@code theme} to the associated Eclipse preferences.
   * 
   * @param theme The color theme to map.
   */
  public abstract void map(Map<String, ColorThemeSetting> theme);

  public void previewRun(Runnable runnable) {
    IEclipsePreferences prefs = preferences;
    try {
      preferences = previewPreferences;
      runnable.run();
    } catch (Throwable ex) {
    // TODO(messick): Add proper error handling.
      ex.printStackTrace();
    } finally {
      preferences = prefs;
    }
  }

  /**
   * Sets the plugin ID and loads preferences.
   * 
   * @param plugin The ID of the Eclipse plugin whose preferences should be altered.
   */
  public void setPluginId(String plugin, WorkingCopyManager manager) {
    preferences = InstanceScope.INSTANCE.getNode(plugin);
    previewPreferences = new PreviewPreferences();
    try {
      String[] keys = preferences.keys();
      for (String key : keys) {
        previewPreferences.put(key, preferences.get(key, null));
      }
    } catch (BackingStoreException ex) {
      Activator.logError(ex);
    }
    preferences = manager.getWorkingCopy(preferences);
  }

  /**
   * Converts a hexadecimal color value to an RGB string.
   * 
   * @param hexColor Hexadecimal color value starting with a hash, as common in CSS.
   * @return Comma-separated list of red, green and blue decimal values.
   */
  protected String hexToRGB(String hexColor) {
    String hexR = hexColor.substring(1, 3);
    String hexG = hexColor.substring(3, 5);
    String hexB = hexColor.substring(5, 7);
    return hexToDec(hexR) + ", " + hexToDec(hexG) + ", " + hexToDec(hexB); // $NON-NLS-1$ // $NON-NLS-2$
  }

  private String hexToDec(String hex) {
    return String.valueOf(Integer.parseInt(hex, 16));
  }
}
