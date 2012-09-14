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
package com.google.dart.tools.update.core;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

/**
 * The plugin activator for the com.google.dart.tools.update.core plugin.
 */
public class UpdateCore extends Plugin {

  /**
   * A temporary flag used to determine where update and install dirs should live when debugging
   * locally.
   */
  public static final boolean DEBUGGING_IN_RUNTIME_WS = System.getProperty("DEBUGGING_IN_RUNTIME_WS") != null;

  /**
   * The update dir name.
   */
  private static final String UPDATES_DIR_NAME = "updates";

  /**
   * Auto download default in case unspecified by the user.
   */
  private static final boolean AUTO_DOWNLOAD_DEFAULT = false;

  /**
   * Auto update checking default in case unspecified by the user.
   */
  private static final boolean AUTO_UPDATE_CHECK_DEFAULT = true;

  /**
   * Changelog URL.
   */
  //TODO(pquitslund): move to props file
  private static final String CHANGELOG_URL = "http://commondatastorage.googleapis.com/dart-editor-archive-integration/latest/changelog.html";

  /**
   * Key to fetch the update URL.
   */
  private static final String UPDATE_URL_PROP_KEY = "updateUrl";

  /**
   * Environment variable key for user-specified update URLs.
   */
  private static final String UPDATE_URL_ENV_VAR = "com.dart.tools.update.core.url";

  /**
   * The Update Core plugin id.
   */
  private static final String PLUGIN_ID = "com.google.dart.tools.update.core";

  /**
   * Preference key for enabling auto download of updates.
   */
  private static final String PREFS_AUTO_DOWNLOAD = "autoDownloadUpdates";

  /**
   * Preference key for auto checking for updates.
   */
  private static final String PREFS_AUTO_UPDATE_CHECK = "autoCheckUpdates";

  /**
   * Delay for initialization of the update manager post startup,
   */
  private static final long UPDATE_MANAGER_INIT_DELAY = TimeUnit.MINUTES.toMillis(5);

  /**
   * Delay for installation cleanup post startup,
   */
  //private static final long INSTALLATION_CLEANUP_INIT_DELAY = TimeUnit.MINUTES.toMillis(3);

  /**
   * Default update check interval.
   */
  private static final long DEFAULT_UPDATE_CHECK_INTERVAL = TimeUnit.HOURS.toMillis(1);

  //The activated plugin
  private static UpdateCore PLUGIN;

  /**
   * Create a Status object with the given message and this plugin's ID.
   */
  public static IStatus createCancelStatus(String message) {
    return new Status(IStatus.CANCEL, PLUGIN_ID, message);
  }

  /**
   * Create a Status object with the given message and this plugin's ID.
   */
  public static Status createErrorStatus(String message) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message);
  }

  /**
   * Enable/disable auto download of updates.
   * 
   * @param enable <code>true</code> to enable, <code>false</code> to disable
   */
  public static void enableAutoDownload(boolean enable) {
    try {
      IEclipsePreferences preferences = PLUGIN.getPreferences();
      preferences.putBoolean(PREFS_AUTO_DOWNLOAD, enable);
      preferences.flush();
    } catch (BackingStoreException exception) {
      logError(exception);
    }
  }

  /**
   * Get the URL for the integration change log.
   * 
   * @return the changelog url
   */
  public static String getChangeLogUrl() {
    return CHANGELOG_URL;
  }

  /**
   * Fetch the current editor initial revision.
   * 
   * @return the current revision
   */
  public static Revision getCurrentRevision() {
    return Revision.forValue(DartCore.getBuildId());
  }

  /**
   * Get the shared singleton instance.
   */
  public static UpdateCore getInstance() {
    return PLUGIN;
  }

  /**
   * Get the interval for automatic update checks.
   */
  public static long getUpdateCheckInterval() {
    return DEFAULT_UPDATE_CHECK_INTERVAL;
  }

  /**
   * Get the directory location for locally staging updates.
   */
  public static IPath getUpdateDirPath() {
    URL installLocation = Platform.getInstallLocation().getURL();
    try {
      return URIUtil.toPath(org.eclipse.core.runtime.URIUtil.toURI(installLocation)).append(
          UPDATES_DIR_NAME);
    } catch (URISyntaxException e) {
      //not possible because we know the above URL is valid
      return null;
    }
  }

  /**
   * Get the singleton update manager.
   */
  public static UpdateManager getUpdateManager() {
    return UpdateManager.getInstance();
  }

  /**
   * Get the root URL for update checks. Custom URLs can be set via the
   * "com.dart.tools.update.core.url" environment variable. If the is no custom URL defined, lookup
   * will default to a URL defined in plugin .options.
   * 
   * @return the URL (or <code>null</code> if unset).
   */
  public static String getUpdateUrl() {

    String userSpecifiedURL = System.getProperty(UPDATE_URL_ENV_VAR);
    if (userSpecifiedURL != null) {
      return userSpecifiedURL;
    }

    ResourceBundle resourceBundle = getResourceBundle();
    return (String) resourceBundle.getObject(UPDATE_URL_PROP_KEY);
  }

  /**
   * Check if auto download of updates is enabled.
   * 
   * @return <code>true</code> if enabled, <code>false</code> otherwise
   */
  public static boolean isAutoDownloadEnabled() {
    return DartCoreDebug.ENABLE_UPDATE
        && PLUGIN.getPreferences().getBoolean(PREFS_AUTO_DOWNLOAD, AUTO_DOWNLOAD_DEFAULT);
  }

  /**
   * Check if automatic checking for updates is enabled.
   * 
   * @return <code>true</code> if enabled, <code>false</code> otherwise
   */
  public static boolean isAutoUpdateCheckingEnabled() {
    return DartCoreDebug.ENABLE_UPDATE
        && PLUGIN.getPreferences().getBoolean(PREFS_AUTO_UPDATE_CHECK, AUTO_UPDATE_CHECK_DEFAULT);
  }

  /**
   * Log the given message as an error to the Eclipse log.
   * 
   * @param message the message
   */
  public static void logError(String message) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param message the message
   * @param exception the exception
   */
  public static void logError(String message, Throwable exception) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param exception the exception to log
   */
  public static void logError(Throwable exception) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, exception.getMessage(), exception));
    }
  }

  /**
   * Log the given informational message.
   * 
   * @param message an informational message
   */
  public static void logInfo(String message) {
    logInfo(message, null);
  }

  /**
   * Log the given exception as one representing an informational message.
   * 
   * @param message an explanation of why the error occurred or what it means
   * @param exception the exception being logged
   */
  public static void logInfo(String message, Throwable exception) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(Status.INFO, PLUGIN_ID, "INFO: " + message, exception));
    }
  }

  /**
   * Log the given message as a warning to the Eclipse log.
   * 
   * @param message the message to log
   */
  public static void logWarning(String message) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception as a warning in the Eclipse log.
   * 
   * @param message the message
   * @param exception the exception
   */
  public static void logWarning(String message, Throwable exception) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, exception));
    }
  }

  private static ResourceBundle getResourceBundle() {
    if (PLUGIN == null) {
      throw new IllegalStateException("update checks are only valid post bundle activation");
    }
    return Platform.getResourceBundle(PLUGIN.getBundle());
  }

  private IEclipsePreferences preferences;

  private Job installationCleanupJob;
  private Job managerInitializationJob;

  /**
   * Return the preferences node that contains the preferences for the Update Core plugin.
   * 
   * @return the node containing the plug-in preferences or <code>null</code>
   */
  public IEclipsePreferences getPreferences() {
    if (preferences == null) {
      preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
    }
    return preferences;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    PLUGIN = this;

    scheduleInstallationCleanup();
    scheduleManagerStart();

    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    try {
      Job initJob = managerInitializationJob;

      if (initJob != null) {
        initJob.cancel();
      }

      Job cleanupJob = installationCleanupJob;

      if (cleanupJob != null) {
        cleanupJob.cancel();
      }

      getUpdateManager().stop();
    } finally {
      super.stop(context);
      PLUGIN = null;
    }
  }

  private void scheduleInstallationCleanup() {
//TODO(pquitslund): enable after testing
//    installationCleanupJob = new CleanupInstallationJob() {
//      @Override
//      protected IStatus run(IProgressMonitor monitor) {
//        try {
//          return super.run(monitor);
//        } finally {
//          installationCleanupJob = null;
//        }
//      }
//    };
//
//    installationCleanupJob.schedule(INSTALLATION_CLEANUP_INIT_DELAY);
  }

  private void scheduleManagerStart() {
    //wait a bit before checking for updates to avoid competing for resources at startup
    //note that update checks can still be manually initiated
    managerInitializationJob = new Job("Update manager initialization") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        //make doubly sure that the workbench is up and running 
        if (PlatformUI.isWorkbenchRunning()) {
          managerInitializationJob = null;
          getUpdateManager().start();
        } else {
          schedule(500);
        }
        return Status.OK_STATUS;
      }
    };
    managerInitializationJob.setSystem(true);

    managerInitializationJob.schedule(UPDATE_MANAGER_INIT_DELAY);
  }

}
