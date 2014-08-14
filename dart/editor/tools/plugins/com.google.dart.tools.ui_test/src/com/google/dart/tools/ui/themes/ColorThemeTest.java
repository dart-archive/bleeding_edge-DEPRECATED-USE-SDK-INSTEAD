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
package com.google.dart.tools.ui.themes;

import com.google.dart.tools.ui.AbstractDartEditorTest;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.theme.ColorTheme;
import com.google.dart.tools.ui.theme.ColorThemeManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ColorThemeTest extends AbstractDartEditorTest {

  public void testThemeChange() throws Exception {
    // Creating the theme manager reads all theme files (22 at present), parses the xml, and
    // initializes the object model. Then it reads all the mapping files, parses the xml, and
    // initializes the object model for them, too. There are currently 7 mappings but some may
    // be deleted. Basically, this test exercises most of the theme manager code.
    ColorThemeManager colorThemeManager = new ColorThemeManager();
    IPreferenceStore store = PreferenceConstants.getPreferenceStore();
    PreferenceConstants.initializeDefaultValues(store);

    //openTestEditor("");
    fail("Open test editor");

    IPreferenceStore prefs = testEditor.getPreferences();
    Display display = testEditor.getViewer().getTextWidget().getDisplay();
    Color ebg = DartUI.getEditorBackground(prefs, display);
    Color efg = DartUI.getEditorForeground(prefs, display);
    assertNull(ebg);
    assertNull(efg);
    List<String> themeNames = getThemeList(colorThemeManager);
    assertNotNull(themeNames);
    assertFalse(themeNames.isEmpty());
    String selectedThemeName = themeNames.get(4);
    assertEquals("Havenjark", selectedThemeName);
    store.setValue("colorTheme", selectedThemeName);
    // change the world to match the new theme
    colorThemeManager.applyTheme(selectedThemeName);
    // verify that the open editor got updated with new colors
    ebg = DartUI.getEditorBackground(prefs, display);
    efg = DartUI.getEditorForeground(prefs, display);
    assertNotNull(ebg);
    assertNotNull(efg);
    assertTrue(ebg.toString().indexOf("45, 54, 57") > 0);
    assertTrue(efg.toString().indexOf("192, 182, 168") > 0);
  }

  // Build the theme list as shown in the preference page.
  private List<String> getThemeList(ColorThemeManager colorThemeManager) {
    Set<ColorTheme> themes = colorThemeManager.getThemes();
    List<String> themeNames = new LinkedList<String>();
    for (ColorTheme theme : themes) {
      themeNames.add(theme.getName());
    }
    Collections.sort(themeNames, String.CASE_INSENSITIVE_ORDER);
    themeNames.add(0, "Default");
    return themeNames;
  }
}
