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
package com.google.dart.tools.ui.theme;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class ColorThemeSemanticHighlightingMapping extends ColorThemeMapping {

  protected String separator = "."; // $NON-NLS-1$

  public ColorThemeSemanticHighlightingMapping(String pluginKey, String themeKey) {
    super(pluginKey, themeKey);
  }

  @Override
  public void putPreferences(IEclipsePreferences preferences, ColorThemeSetting setting) {
    preferences.putBoolean(pluginKey + separator + "enabled", true); // $NON-NLS-1$
    preferences.put(pluginKey + separator + "color", setting.getColor().asRGB());
    if (setting.isBoldEnabled() != null) {
      preferences.putBoolean(pluginKey + separator + "bold", setting.isBoldEnabled()); // $NON-NLS-1$
    }
    if (setting.isItalicEnabled() != null) {
      preferences.putBoolean(pluginKey + separator + "italic", setting.isItalicEnabled()); // $NON-NLS-1$
    }
    if (setting.isUnderlineEnabled() != null) {
      preferences.putBoolean(pluginKey + separator + "underline", setting.isUnderlineEnabled()); // $NON-NLS-1$
    }
    if (setting.isStrikethroughEnabled() != null) {
      preferences.putBoolean(pluginKey + separator + "strikethrough", // $NON-NLS-1$
          setting.isStrikethroughEnabled());
    }
  }

  @Override
  public void removePreferences(IEclipsePreferences preferences) {
    preferences.remove(pluginKey + separator + "enabled"); // $NON-NLS-1$
    preferences.remove(pluginKey + separator + "color"); // $NON-NLS-1$
    preferences.remove(pluginKey + separator + "bold"); // $NON-NLS-1$
    preferences.remove(pluginKey + separator + "italic"); // $NON-NLS-1$
    preferences.remove(pluginKey + separator + "underline"); // $NON-NLS-1$
    preferences.remove(pluginKey + separator + "strikethrough"); // $NON-NLS-1$
  }

  public void setSeparator(String separator) {
    this.separator = separator;
  }

}
