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

import com.google.dart.tools.debug.core.internal.util.BrowserConfigManager;
import com.google.dart.tools.debug.core.internal.util.BrowserManager;

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

import java.util.List;

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

  /**
   * If true, causes the Debug plugin to log connection events to the Eclipse .log. This is very
   * verbose. Start the vm with -Ddart.debug.logging.connection=true.
   */
  public static final boolean CONNECTION_LOGGING = Boolean.getBoolean("dart.debug.logging.connection");

  public static final String REMOTE_LAUNCH_CONFIG_ID = "com.google.dart.tools.debug.core.remoteLaunchConfig";

  public static final String SERVER_LAUNCH_CONFIG_ID = "com.google.dart.tools.debug.core.serverLaunchConfig";

  public static final String CHROME_LAUNCH_CONFIG_ID = "com.google.dart.tools.debug.core.chromeLaunchConfig";

  private static IDebugEventSetListener debugEventListener;

  private static DartDebugCorePlugin plugin;

  private static final String PREFS_JRE_PATH = "jrePath";

  private static final String PREFS_NODE_PATH = "nodePath";

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
    if (debugEventListener != null) {
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
   * @return the browser configuration given a name
   */
  public ChromeBrowserConfig getChromeBrowserConfig(String browserName) {
    return BrowserConfigManager.getManager().getBrowserConfig(browserName);
  }

  /**
   * @return the list of configured browsers
   */
  public List<ChromeBrowserConfig> getConfiguredBrowsers() {
    return BrowserConfigManager.getManager().getConfiguredBrowsers();
  }

  /**
   * Returns the path to the JRE executable, if it has been set. Otherwise, this method returns the
   * empty string.
   * 
   * @return the path to the JRE executable
   */
  public String getJreExecutablePath() {
    return getPrefs().get(PREFS_JRE_PATH, "");
  }

  /**
   * Returns the path to the Node executable, if it has been set. Otherwise, this method returns the
   * empty string.
   * 
   * @return the path to the Node executable
   */
  public String getNodeExecutablePath() {
    return getPrefs().get(PREFS_NODE_PATH, "");
  }

  public IEclipsePreferences getPrefs() {
    if (prefs == null) {
      prefs = new InstanceScope().getNode(PLUGIN_ID);
    }

    return prefs;
  }

  /**
   * Set the list of configured browsers.
   * 
   * @param browsers the list of configured browsers
   */
  public void setConfiguredBrowsers(List<ChromeBrowserConfig> browsers) {
    BrowserConfigManager.getManager().setConfiguredBrowsers(browsers);
  }

  /**
   * Set the path to the JRE executable. This is used to invoke a Java process by the Rhino launch
   * configuration.
   * 
   * @param value the path to the JRE executable.
   */
  public void setJreExecutablePath(String value) {
    getPrefs().put(PREFS_JRE_PATH, value);

    try {
      getPrefs().flush();
    } catch (BackingStoreException exception) {
      logError(exception);
    }
  }

  /**
   * Set the path to the Node executable. This is used to invoke a process running node by the Node
   * launch configuration.
   * 
   * @param value the path to the Node executable.
   */
  public void setNodeExecutablePath(String value) {
    getPrefs().put(PREFS_NODE_PATH, value);

    try {
      getPrefs().flush();
    } catch (BackingStoreException exception) {
      logError(exception);
    }
  }

  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;

    super.start(context);

    if (LOGGING) {
      logDebuggerEvents();
    }

    // Initialize the com.google.dart.tools.debug.ui plugin.
    DebugUIHelperFactory.getDebugUIHelper();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    BrowserManager.getManager().dispose();

    if (debugEventListener != null) {
      DebugPlugin.getDefault().removeDebugEventListener(debugEventListener);

      debugEventListener = null;
    }

    super.stop(context);

    plugin = null;
  }

}
