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
package com.google.dart.tools.search2.internal.ui.text2;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.internal.ui.SearchPreferencePage;
import com.google.dart.tools.search.ui.text.TextSearchQueryProvider;
import com.google.dart.tools.search2.internal.ui.SearchMessages;

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.SafeRunnable;

public class TextSearchQueryProviderRegistry {

  private static final String EXTENSION_POINT_ID = "com.google.dart.tools.search.textSearchQueryProvider"; //$NON-NLS-1$
  private static final String PROVIDER_NODE_NAME = "textSearchQueryProvider"; //$NON-NLS-1$
  private static final String ATTRIB_ID = "id"; //$NON-NLS-1$
  private static final String ATTRIB_LABEL = "label"; //$NON-NLS-1$
  private static final String ATTRIB_CLASS = "class"; //$NON-NLS-1$

  private TextSearchQueryProvider fPreferredProvider;
  private String fPreferredProviderId;

  public TextSearchQueryProviderRegistry() {
    fPreferredProviderId = null; // only null when not initialized
    fPreferredProvider = null;
  }

  public TextSearchQueryProvider getPreferred() {
    String preferredId = getPreferredEngineID();
    if (!preferredId.equals(fPreferredProviderId)) {
      updateProvider(preferredId);
    }
    return fPreferredProvider;
  }

  private void updateProvider(String preferredId) {
    fPreferredProviderId = preferredId;
    fPreferredProvider = null;
    if (preferredId.length() != 0) { // empty string: default engine
      fPreferredProvider = createFromExtension(preferredId);
    }
    if (fPreferredProvider == null) {
      fPreferredProvider = new DefaultTextSearchQueryProvider();
    }
  }

  private String getPreferredEngineID() {
    IPreferenceStore prefs = SearchPlugin.getDefault().getPreferenceStore();
    String preferedEngine = prefs.getString(SearchPreferencePage.TEXT_SEARCH_QUERY_PROVIDER);
    return preferedEngine;
  }

  private TextSearchQueryProvider createFromExtension(final String id) {
    final TextSearchQueryProvider[] res = new TextSearchQueryProvider[] {null};

    SafeRunnable safe = new SafeRunnable() {
      public void run() throws Exception {
        IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(
            EXTENSION_POINT_ID);
        for (int i = 0; i < extensions.length; i++) {
          IConfigurationElement curr = extensions[i];
          if (PROVIDER_NODE_NAME.equals(curr.getName()) && id.equals(curr.getAttribute(ATTRIB_ID))) {
            res[0] = (TextSearchQueryProvider) curr.createExecutableExtension(ATTRIB_CLASS);
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

  public String[][] getAvailableProviders() {
    ArrayList<String[]> res = new ArrayList<String[]>();
    res.add(new String[] {SearchMessages.TextSearchQueryProviderRegistry_defaultProviderLabel, ""}); //$NON-NLS-1$

    IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(
        EXTENSION_POINT_ID);
    for (int i = 0; i < extensions.length; i++) {
      IConfigurationElement engine = extensions[i];
      if (PROVIDER_NODE_NAME.equals(engine.getName())) {
        res.add(new String[] {engine.getAttribute(ATTRIB_LABEL), engine.getAttribute(ATTRIB_ID)});
      }
    }
    return res.toArray(new String[res.size()][]);
  }
}
