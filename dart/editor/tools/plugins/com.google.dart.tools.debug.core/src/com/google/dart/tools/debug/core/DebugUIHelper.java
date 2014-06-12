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

package com.google.dart.tools.debug.core;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.dartium.DartiumDebugTarget;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.io.File;

/**
 * A helper class to allow core classes to communicate with the UI.
 */
public abstract class DebugUIHelper {
  private static class DefaultDebugUIHelper extends DebugUIHelper {
    @Override
    public void activateApplication(File application, String name) {
      // no-op

    }

    @Override
    public void handleDevtoolsDisconnect(DartiumDebugTarget target) {
      // no-op

    }

    @Override
    public void openBrowserTab(String url) {
      // no-op
    }

    @Override
    public void showError(String title, String message) {
      // no-op

    }

    @Override
    public void showStatusLineMessage(String message) {
      // no-op

    }
  }

  private static final String EXTENSION_POINT_ID = DartDebugCorePlugin.PLUGIN_ID + ".debugUIHelper";
  private static boolean initialized;

  private static DebugUIHelper helper;

  public static DebugUIHelper getHelper() {
    if (!initialized) {
      initialize();
    }

    return helper;
  }

  /**
   * Scan the plugin registry for a contributed working copy owner. Note that even if the buffer
   * provider is a working copy owner, only its <code>createBuffer(CompilationUnit)</code> method is
   * used by the primary working copy owner. It doesn't replace the internal primary working owner.
   */
  private static void initialize() {
    initialized = true;

    IExtensionRegistry registery = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registery.getExtensionPoint(EXTENSION_POINT_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

    if (elements.length > 0) {
      if (elements.length > 1) {
        DartCore.logError("Error, more then one debug UI helper contributed", null);
      }

      IConfigurationElement element = elements[0];

      try {
        DebugUIHelper workingCopy = (DebugUIHelper) element.createExecutableExtension("class");

        helper = workingCopy;
      } catch (Throwable t) {
        DartDebugCorePlugin.logError(t);
      }
    } else {
      helper = new DefaultDebugUIHelper();
    }
  }

  protected DebugUIHelper() {

  }

  public abstract void activateApplication(File application, String name);

  public abstract void handleDevtoolsDisconnect(DartiumDebugTarget target);

  public abstract void openBrowserTab(String url);

  public void showError(String title, CoreException e) {
    showError(title, e.getMessage());
  }

  public abstract void showError(String title, String message);

  public abstract void showStatusLineMessage(String message);

}
