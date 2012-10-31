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

import com.google.dart.tools.ui.theme.ColorThemeMapping;
import com.google.dart.tools.ui.theme.ColorThemeSemanticHighlightingMapping;
import com.google.dart.tools.ui.theme.ColorThemeSetting;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

// TODO(devoncarew): Adapt this to the new editor(s).

/**
 * Maps color themes to preferences for Eclipse's XML, HTML and CSS editors.
 */
public class WebEditorMapper extends GenericMapper {

  private class Mapping extends ColorThemeMapping {

    public Mapping(String pluginKey, String themeKey) {
      super(pluginKey, themeKey);
    }

    @Override
    public void putPreferences(IEclipsePreferences preferences, ColorThemeSetting setting) {
      String value = setting.getColor().asHex() + " | null | " + setting.isBoldEnabled() + " | " // $NON-NLS-1$ // $NON-NLS-2$
          + setting.isItalicEnabled() + " | " + setting.isStrikethroughEnabled() + " | " // $NON-NLS-1$ // $NON-NLS-2$
          + setting.isUnderlineEnabled();
      preferences.put(pluginKey, value);
    }

  }

  private class SemanticMapping extends ColorThemeSemanticHighlightingMapping {

    public SemanticMapping(String pluginKey, String themeKey) {
      super(pluginKey, themeKey);
    }

    @Override
    public void putPreferences(IEclipsePreferences preferences, ColorThemeSetting setting) {
      super.putPreferences(preferences, setting);
      preferences.put(pluginKey + separator + "color", setting.getColor().asHex()); // $NON-NLS-1$
    }

  }

  @Override
  protected ColorThemeMapping createMapping(String pluginKey, String themeKey) {
    return new Mapping(pluginKey, themeKey);
  }

  @Override
  protected ColorThemeSemanticHighlightingMapping createSemanticHighlightingMapping(
      String pluginKey, String themeKey) {
    return new SemanticMapping(pluginKey, themeKey);
  }

}
