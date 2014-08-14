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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ColorPreferenceTest extends AbstractDartEditorTest {

  public void testColorPrefs() throws Exception {
    IPreferenceStore store = PreferenceConstants.getPreferenceStore();
    PreferenceConstants.initializeDefaultValues(store);

    //openTestEditor("");
    fail("Open test editor");

    IPreferenceStore prefs = testEditor.getPreferences();
    Display display = testEditor.getViewer().getTextWidget().getDisplay();
    Color ebg = DartUI.getEditorBackground(prefs, display);
    Color efg = DartUI.getEditorForeground(prefs, display);
    Color esbg = DartUI.getEditorSelectionBackground(prefs, display);
    Color esfg = DartUI.getEditorSelectionForeground(prefs, display);
    assertNull(ebg);
    assertNull(efg);
    assertNull(esbg);
    assertNull(esfg);
    // simulate changing theme
    store.putValue("AbstractTextEditor.Color.Foreground.SystemDefault", "false");
    store.putValue("AbstractTextEditor.Color.Background.SystemDefault", "false");
    store.putValue("AbstractTextEditor.Color.SelectionBackground.SystemDefault", "false");
    store.putValue("AbstractTextEditor.Color.SelectionForeground.SystemDefault", "false");
    store.putValue("AbstractTextEditor.Color.Foreground", "0,0,0");
    store.putValue("AbstractTextEditor.Color.Background", "1,1,1");
    store.putValue("AbstractTextEditor.Color.SelectionForeground", "10,10,10");
    store.putValue("AbstractTextEditor.Color.SelectionBackground", "11,11,11");
    ebg = DartUI.getEditorBackground(prefs, display);
    efg = DartUI.getEditorForeground(prefs, display);
    esbg = DartUI.getEditorSelectionBackground(prefs, display);
    esfg = DartUI.getEditorSelectionForeground(prefs, display);
    assertNotNull(ebg);
    assertNotNull(efg);
    assertNotNull(esbg);
    assertNotNull(esfg);
    // simulate restoring defaults
    store.setToDefault("AbstractTextEditor.Color.Foreground.SystemDefault");
    store.setToDefault("AbstractTextEditor.Color.Background.SystemDefault");
    store.setToDefault("AbstractTextEditor.Color.SelectionBackground.SystemDefault");
    store.setToDefault("AbstractTextEditor.Color.SelectionForeground.SystemDefault");
    store.setToDefault("AbstractTextEditor.Color.Foreground");
    store.setToDefault("AbstractTextEditor.Color.Background");
    store.setToDefault("AbstractTextEditor.Color.SelectionForeground");
    store.setToDefault("AbstractTextEditor.Color.SelectionBackground");
    ebg = DartUI.getEditorBackground(prefs, display);
    efg = DartUI.getEditorForeground(prefs, display);
    esbg = DartUI.getEditorSelectionBackground(prefs, display);
    esfg = DartUI.getEditorSelectionForeground(prefs, display);
    assertNull(ebg);
    assertNull(efg);
    assertNull(esbg);
    assertNull(esfg);
  }

}
