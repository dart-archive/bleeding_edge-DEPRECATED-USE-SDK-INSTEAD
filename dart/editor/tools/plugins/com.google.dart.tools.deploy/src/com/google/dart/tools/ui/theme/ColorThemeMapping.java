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

public class ColorThemeMapping {

  protected String pluginKey;
  protected String themeKey;

  public ColorThemeMapping(String pluginKey, String themeKey) {
    this.pluginKey = pluginKey;
    this.themeKey = themeKey;
  }

  public Object getThemeKey() {
    return themeKey;
  }

  public void putPreferences(IEclipsePreferences preferences, ColorThemeSetting setting) {
    Color color = setting.getColor();
    preferences.put(pluginKey, color.toString());
  }

  public void removePreferences(IEclipsePreferences preferences) {
    preferences.remove(pluginKey);
  }

}
