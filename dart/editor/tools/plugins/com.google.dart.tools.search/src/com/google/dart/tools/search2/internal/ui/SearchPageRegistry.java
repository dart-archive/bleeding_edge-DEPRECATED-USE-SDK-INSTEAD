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
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.ISearchResultPage;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;

import java.util.HashMap;
import java.util.Map;

public class SearchPageRegistry {

  public static final String ID_EXTENSION_POINT = "com.google.dart.tools.search.searchResultViewPages"; //$NON-NLS-1$
  public static final String ATTRIB_SEARCH_RESULT_CLASS = "searchResultClass"; //$NON-NLS-1$
  public static final String ATTRIB_ID = "id"; //$NON-NLS-1$

  public static final String ATTRIB_LABEL = "label"; //$NON-NLS-1$
  public static final String ATTRIB_ICON = "icon"; //$NON-NLS-1$

  public static final String ATTRIB_HELP_CONTEXT = "helpContextId"; //$NON-NLS-1$

  private final Map<String, IConfigurationElement> fResultClassNameToExtension;
  private final Map<IConfigurationElement, ISearchResultPage> fExtensionToInstance;
  private final IConfigurationElement[] fExtensions;

  public SearchPageRegistry() {
    fExtensionToInstance = new HashMap<IConfigurationElement, ISearchResultPage>();
    fResultClassNameToExtension = new HashMap<String, IConfigurationElement>();
    fExtensions = Platform.getExtensionRegistry().getConfigurationElementsFor(ID_EXTENSION_POINT);
    for (int i = 0; i < fExtensions.length; i++) {
      fResultClassNameToExtension.put(
          fExtensions[i].getAttribute(ATTRIB_SEARCH_RESULT_CLASS),
          fExtensions[i]);
    }
  }

  public String findLabelForPageId(String pageId) {
    IConfigurationElement configElement = findConfigurationElement(pageId);
    if (configElement != null) {
      return configElement.getAttribute(ATTRIB_LABEL);
    }
    return null;
  }

  public ISearchResultPage findPageForPageId(String pageId, boolean create) {
    IConfigurationElement configElement = findConfigurationElement(pageId);
    if (configElement != null) {
      return getSearchResultPage(configElement, create);
    }
    return null;
  }

  public ISearchResultPage findPageForSearchResult(ISearchResult result, boolean create) {
    Class<? extends ISearchResult> resultClass = result.getClass();
    IConfigurationElement configElement = findConfigurationElement(resultClass);
    if (configElement != null) {
      return getSearchResultPage(configElement, create);
    }
    return null;
  }

  public String getHelpContextId(String pageId) {
    IConfigurationElement configElement = findConfigurationElement(pageId);
    if (configElement != null) {
      return configElement.getAttribute(ATTRIB_HELP_CONTEXT);
    }
    return null;
  }

  private IConfigurationElement findConfigurationElement(Class<?> resultClass) {
    String className = resultClass.getName();
    IConfigurationElement configElement = fResultClassNameToExtension.get(className);
    if (configElement != null) {
      return configElement;
    }
    Class<?> superclass = resultClass.getSuperclass();
    if (superclass != null) {
      IConfigurationElement foundExtension = findConfigurationElement(superclass);
      if (foundExtension != null) {
        fResultClassNameToExtension.put(className, configElement);
        return foundExtension;
      }
    }

    Class<?>[] interfaces = resultClass.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      IConfigurationElement foundExtension = findConfigurationElement(interfaces[i]);
      if (foundExtension != null) {
        fResultClassNameToExtension.put(className, configElement);
        return foundExtension;
      }
    }
    return null;
  }

  private IConfigurationElement findConfigurationElement(String pageId) {
    for (int i = 0; i < fExtensions.length; i++) {
      IConfigurationElement curr = fExtensions[i];
      if (pageId.equals(curr.getAttribute(ATTRIB_ID))) {
        return curr;
      }
    }
    return null;
  }

  private ISearchResultPage getSearchResultPage(final IConfigurationElement configElement,
      boolean create) {
    ISearchResultPage instance = fExtensionToInstance.get(configElement);
    if (instance == null && create) {
      final Object[] result = new Object[1];

      ISafeRunnable safeRunnable = new SafeRunnable(
          SearchMessages.SearchPageRegistry_error_creating_extensionpoint) {
        @Override
        public void handleException(Throwable e) {
          // invalid contribution
          SearchPlugin.log(e);
        }

        @Override
        public void run() throws Exception {
          result[0] = configElement.createExecutableExtension("class"); //$NON-NLS-1$
        }
      };
      SafeRunner.run(safeRunnable);
      if (result[0] instanceof ISearchResultPage) {
        instance = (ISearchResultPage) result[0];
        instance.setID(configElement.getAttribute(ATTRIB_ID));
        fExtensionToInstance.put(configElement, instance);
      }
    }
    return instance;
  }

}
