/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;

public class DartUIPreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = PreferenceConstants.getPreferenceStore();

    EditorsUI.useAnnotationsPreferencePage(store);
    EditorsUI.useQuickDiffPreferencePage(store);
    PreferenceConstants.initializeDefaultValues(store);
    @SuppressWarnings("deprecation")
    IEclipsePreferences defaultPreferences = new DefaultScope().getNode(DartCore.PLUGIN_ID);
    defaultPreferences.put(JavaScriptCore.COMPILER_TASK_TAGS, JavaScriptCore.DEFAULT_TASK_TAGS);
    defaultPreferences.put(
        JavaScriptCore.COMPILER_TASK_PRIORITIES,
        JavaScriptCore.DEFAULT_TASK_PRIORITIES);
    defaultPreferences.put(JavaScriptCore.COMPILER_TASK_CASE_SENSITIVE, JavaScriptCore.ENABLED);

    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      store.setValue(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY, 0);
    } else {
      store.setValue(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY, 80);
    }
  }
}
