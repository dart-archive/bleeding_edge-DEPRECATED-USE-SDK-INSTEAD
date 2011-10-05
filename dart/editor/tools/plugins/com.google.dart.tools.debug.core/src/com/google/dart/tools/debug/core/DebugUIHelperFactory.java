/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import java.util.List;

/**
 * A factory to create new ChromeTabChoosers.
 */
public class DebugUIHelperFactory {

  private static class DefaultDebugUIHelper implements DebugUIHelper {
    @Override
    public void displayError(String title, String message) {
      // nothing to do

    }

    @Override
    public String getPlatform() {
      return "";
    }

    @Override
    public int select(List<String> availableTabs) {
      if (availableTabs.size() == 0) {
        return -1;
      }

      return availableTabs.size() - 1;
    }
  }

  public static DebugUIHelper getDebugUIHelper() {
    IConfigurationElement[] elements = Platform.getExtensionRegistry().getExtensionPoint(
        DartDebugCorePlugin.PLUGIN_ID + ".debugUIFactory").getConfigurationElements();
    for (IConfigurationElement elem : elements) {
      //String pluginId = elem.getDeclaringExtension().getNamespaceIdentifier();

      if ("uiFactory".equals(elem.getName())) {
        String className = elem.getAttribute("class");

        if (className != null && className.length() > 0) {
          try {
            Object tabChooser = elem.createExecutableExtension("class");

            if (tabChooser instanceof DebugUIHelper) {
              return (DebugUIHelper) tabChooser;
            }
          } catch (CoreException ce) {
            DartDebugCorePlugin.logError(ce);
          }
        }
      }
    }

    return new DefaultDebugUIHelper();
  }

}
