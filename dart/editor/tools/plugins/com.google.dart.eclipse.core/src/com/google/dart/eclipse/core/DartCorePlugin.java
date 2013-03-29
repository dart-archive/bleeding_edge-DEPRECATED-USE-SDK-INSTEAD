/*
 * Copyright 2013 Dart project authors.
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

package com.google.dart.eclipse.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator for the com.google.dart.eclipse.core plugin.
 */
public class DartCorePlugin extends Plugin {
  /**
   * The Dart Core plug-in ID.
   */
  public static final String PLUGIN_ID = "com.google.dart.eclipse.core";

  private static DartCorePlugin plugin;

  /**
   * @return the plugin singleton instance
   */
  public static DartCorePlugin getPlugin() {
    return plugin;
  }

  /**
   * Log the given message as an error to the Eclipse log.
   * 
   * @param message
   */
  public static void logError(String message) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param message
   * @param exception
   */
  public static void logError(String message, Throwable exception) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param exception
   */
  public static void logError(Throwable exception) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(
          new Status(IStatus.ERROR, PLUGIN_ID, exception.getMessage(), exception));
    }
  }

  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;

    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    plugin = null;
  }

}
