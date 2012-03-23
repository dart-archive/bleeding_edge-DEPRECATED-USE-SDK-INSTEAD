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

import com.google.dart.tools.debug.core.util.BrowserManager;
import com.google.dart.tools.debug.core.util.ResourceChangeManager;
import com.google.dart.tools.debug.core.util.ResourceServerManager;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * The plugin activator for the com.google.dart.tools.debug.core plugin.
 */
public class DartDebugCorePlugin extends Plugin {

  /**
   * The Dart Debug Core plug-in ID.
   */
  public static final String PLUGIN_ID = "com.google.dart.tools.debug.core"; //$NON-NLS-1$

  /**
   * The Dart debug marker ID.
   */
  public static final String DEBUG_MARKER_ID = "com.google.dart.tools.debug.core.breakpointMarker"; //$NON-NLS-1$

  /**
   * The debug model ID.
   */
  public static final String DEBUG_MODEL_ID = "com.google.dart.tools.debug.core"; //$NON-NLS-1$

  /**
   * If true, causes the Debug plugin to log to the Eclipse .log. Start the vm with
   * -Ddart.debug.logging=true.
   */
  public static final boolean LOGGING = Boolean.getBoolean("dart.debug.logging");

  // TODO(devoncarew): remove this when the debugger supports pausing
  public static boolean VM_SUPPORTS_PAUSING = false;

  // TODO(devoncarew): remove this when the debugger supports value modification
  public static boolean VM_SUPPORTS_VALUE_MODIFICATION = false;

  public static boolean SEND_MODIFIED_CSS = false;

  // TODO(devoncarew): the vm/webkit protocol claims to support source modification, but it does
  // not yet do anything
  public static boolean SEND_MODIFIED_DART = false;

  public static final String BROWSER_LAUNCH_CONFIG_ID = "com.google.dart.tools.debug.core.browserLaunchConfig";

  public static final String REMOTE_LAUNCH_CONFIG_ID = "com.google.dart.tools.debug.core.remoteLaunchConfig";

  public static final String SERVER_LAUNCH_CONFIG_ID = "com.google.dart.tools.debug.core.serverLaunchConfig";

  public static final String DARTIUM_LAUNCH_CONFIG_ID = "com.google.dart.tools.debug.core.dartiumLaunchConfig";

  private static IDebugEventSetListener debugEventListener;

  private static DartDebugCorePlugin plugin;

  public static final String PREFS_DART_VM_PATH = "vmPath";

  public static final String PREFS_BROWSER_NAME = "browserName";

  public static final String PREFS_DEFAULT_BROWSER = "defaultBrowser";

  /**
   * Create a Status object with the given message and this plugin's ID.
   * 
   * @param message
   * @return
   */
  public static Status createErrorStatus(String message) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message);
  }

  /**
   * @return the plugin singleton instance
   */
  public static DartDebugCorePlugin getPlugin() {
    return plugin;
  }

  /**
   * For use during development - this method listens to and logs all Eclipse debugger events.
   */
  public static void logDebuggerEvents() {
    if (debugEventListener == null) {
      debugEventListener = new IDebugEventSetListener() {
        @Override
        public void handleDebugEvents(DebugEvent[] events) {
          for (DebugEvent event : events) {
            logInfo(event.toString());
          }
        }
      };

      DebugPlugin.getDefault().addDebugEventListener(debugEventListener);
    }
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

  /**
   * Log the given message as an info to the Eclipse log.
   * 
   * @param message
   */
  public static void logInfo(String message) {
    if (LOGGING && getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given message as a warning to the Eclipse log.
   * 
   * @param message
   */
  public static void logWarning(String message) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }
  }

  private IEclipsePreferences prefs;

  /**
   * Returns the path to the Browser executable, if it has been set. Otherwise, this method returns
   * the empty string.
   * 
   * @return the path to the Browser executable
   */
  public String getBrowserExecutablePath() {
    return getPrefs().get(PREFS_BROWSER_NAME, "");
  }

  /**
   * Returns the path to the Dart VM executable, if it has been set. Otherwise, this method returns
   * the empty string.
   * 
   * @return the path to the Dart VM executable
   */
  public String getDartVmExecutablePath() {
    return getPrefs().get(PREFS_DART_VM_PATH, "");
  }

  public boolean getIsDefaultBrowser() {
    return getPrefs().getBoolean(PREFS_DEFAULT_BROWSER, true);
  }

  public IEclipsePreferences getPrefs() {
    if (prefs == null) {
      prefs = new InstanceScope().getNode(PLUGIN_ID);
    }

    return prefs;
  }

  /**
   * Set the path to the Browser executable.
   * 
   * @param value the path to the Browser executable.
   */
  public void setBrowserExecutablePath(String value) {
    getPrefs().put(PREFS_BROWSER_NAME, value);

    try {
      getPrefs().flush();
    } catch (BackingStoreException exception) {
      logError(exception);
    }
  }

  /**
   * Set the path to the Dart VM executable.
   * 
   * @param value the path to the Dart VM executable.
   */
  public void setDartVmExecutablePath(String value) {
    getPrefs().put(PREFS_DART_VM_PATH, value);

    try {
      getPrefs().flush();
    } catch (BackingStoreException exception) {
      logError(exception);
    }
  }

  public void setDefaultBrowser(boolean value) {
    getPrefs().putBoolean(PREFS_DEFAULT_BROWSER, value);
  }

  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;

    super.start(context);

//    if (LOGGING) {
//      logDebuggerEvents();
//    }
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    ResourceChangeManager.shutdown();
    ResourceServerManager.shutdown();

    BrowserManager.getManager().dispose();

    if (debugEventListener != null) {
      DebugPlugin.getDefault().removeDebugEventListener(debugEventListener);

      debugEventListener = null;
    }

    super.stop(context);

    plugin = null;
  }

}
