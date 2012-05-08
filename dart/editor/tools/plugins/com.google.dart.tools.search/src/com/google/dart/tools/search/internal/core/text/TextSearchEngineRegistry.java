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
package com.google.dart.tools.search.internal.core.text;

import com.google.dart.tools.search.core.text.TextSearchEngine;
import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.internal.ui.SearchPreferencePage;

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.SafeRunnable;

public class TextSearchEngineRegistry {

  private static final String EXTENSION_POINT_ID = "com.google.dart.tools.search.textSearchEngine"; //$NON-NLS-1$
  private static final String ENGINE_NODE_NAME = "textSearchEngine"; //$NON-NLS-1$
  private static final String ATTRIB_ID = "id"; //$NON-NLS-1$
  private static final String ATTRIB_LABEL = "label"; //$NON-NLS-1$
  private static final String ATTRIB_CLASS = "class"; //$NON-NLS-1$

  private TextSearchEngine fPreferredEngine;
  private String fPreferredEngineId;

  public TextSearchEngineRegistry() {
    fPreferredEngineId = null; // only null when not initialized
    fPreferredEngine = null;
  }

  public TextSearchEngine getPreferred() {
    String preferredId = getPreferredEngineID();
    if (!preferredId.equals(fPreferredEngineId)) {
      updateEngine(preferredId);
    }
    return fPreferredEngine;
  }

  private void updateEngine(String preferredId) {
    if (preferredId.length() != 0) { // empty string: default engine
      TextSearchEngine engine = createFromExtension(preferredId);
      if (engine != null) {
        fPreferredEngineId = preferredId;
        fPreferredEngine = engine;
        return;
      }
      // creation failed, clear preference
      setPreferredEngineID(""); // set to default //$NON-NLS-1$
    }
    fPreferredEngineId = ""; //$NON-NLS-1$
    fPreferredEngine = TextSearchEngine.createDefault();
  }

  private String getPreferredEngineID() {
    IPreferenceStore prefs = SearchPlugin.getDefault().getPreferenceStore();
    String preferedEngine = prefs.getString(SearchPreferencePage.TEXT_SEARCH_ENGINE);
    return preferedEngine;
  }

  private void setPreferredEngineID(String id) {
    IPreferenceStore prefs = SearchPlugin.getDefault().getPreferenceStore();
    prefs.setValue(SearchPreferencePage.TEXT_SEARCH_ENGINE, id);
  }

  private TextSearchEngine createFromExtension(final String id) {
    final TextSearchEngine[] res = new TextSearchEngine[] {null};

    SafeRunnable safe = new SafeRunnable() {
      public void run() throws Exception {
        IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(
            EXTENSION_POINT_ID);
        for (int i = 0; i < extensions.length; i++) {
          IConfigurationElement curr = extensions[i];
          if (ENGINE_NODE_NAME.equals(curr.getName()) && id.equals(curr.getAttribute(ATTRIB_ID))) {
            res[0] = (TextSearchEngine) curr.createExecutableExtension(ATTRIB_CLASS);
            return;
          }
        }
      }

      public void handleException(Throwable e) {
        SearchPlugin.log(e);
      }
    };
    SafeRunnable.run(safe);
    return res[0];
  }

  public String[][] getAvailableEngines() {
    ArrayList<String[]> res = new ArrayList<String[]>();
    res.add(new String[] {SearchMessages.TextSearchEngineRegistry_defaulttextsearch_label, ""}); //$NON-NLS-1$

    IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(
        EXTENSION_POINT_ID);
    for (int i = 0; i < extensions.length; i++) {
      IConfigurationElement engine = extensions[i];
      if (ENGINE_NODE_NAME.equals(engine.getName())) {
        res.add(new String[] {engine.getAttribute(ATTRIB_LABEL), engine.getAttribute(ATTRIB_ID)});
      }
    }
    return res.toArray(new String[res.size()][]);
  }
}
