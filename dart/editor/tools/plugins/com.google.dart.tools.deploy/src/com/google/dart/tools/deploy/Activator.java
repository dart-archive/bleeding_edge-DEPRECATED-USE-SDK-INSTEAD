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
package com.google.dart.tools.deploy;

import com.google.dart.tools.ui.console.DartConsoleManager;
import com.google.dart.tools.update.core.UpdateCore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.dart.tools.deploy"; //$NON-NLS-1$
  public static final String EXTENSION_POINT_ID_MAPPER = "com.google.dart.tools.ui.theme.mapper";
  public static final String EXTENSION_POINT_ID_THEME = "com.google.dart.tools.ui.theme.theme";

  // The shared instance
  private static Activator plugin;

  private static Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

  /**
   * Delay for initialization of the update manager post startup,
   */
  private static final long UPDATE_MANAGER_INIT_DELAY = TimeUnit.MINUTES.toMillis(5);

  /**
   * Delay for installation cleanup post startup,
   */
  //private static final long INSTALLATION_CLEANUP_INIT_DELAY = TimeUnit.MINUTES.toMillis(3);

  /**
   * Create an error Status object with the given message and this plugin's ID.
   * 
   * @param message the error message
   * @return the created error status object
   */
  public static Status createErrorStatus(String message) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message);
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static Activator getDefault() {
    return plugin;
  }

  public static Image getImage(ImageDescriptor imageDescriptor) {
    Image image = imageCache.get(imageDescriptor);

    if (image == null) {
      image = imageDescriptor.createImage();

      imageCache.put(imageDescriptor, image);
    }

    return image;
  }

  public static Image getImage(String path) {
    return getImage(getImageDescriptor(path));
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in relative path
   * 
   * @param path the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  /**
   * Get the associated bundle's version.
   * 
   * @return the version string
   */
  public static String getVersionString() {
    Bundle bundle = getDefault().getBundle();
    if (bundle == null) {
      return null;
    }
    return bundle.getHeaders().get(Constants.BUNDLE_VERSION);
  }

  public static void log(String message) {
    getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
  }

  public static void logError(String message) {
    getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
  }

  public static void logError(String message, Throwable exception) {
    getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
  }

  public static void logError(Throwable exception) {
    getDefault().getLog().log(
        new Status(IStatus.ERROR, PLUGIN_ID, exception.getMessage(), exception));
  }

  //update jobs
  private Job installationCleanupJob;
  private Job managerInitializationJob;

  /**
   * The constructor
   */
  public Activator() {
  };

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    plugin = this;

    scheduleInstallationCleanup();
//    scheduleManagerStart();

    DartConsoleManager.initialize();

  }

  @Override
  public void stop(BundleContext context) throws Exception {

    try {

      DartConsoleManager.shutdown();
//      stopUpdateManager();

    } finally {

      plugin = null;
      super.stop(context);

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
          UpdateCore.getUpdateManager().start();
        } else {
          schedule(500);
        }
        return Status.OK_STATUS;
      }
    };
    managerInitializationJob.setSystem(true);

    managerInitializationJob.schedule(UPDATE_MANAGER_INIT_DELAY);
  }

  private void stopUpdateManager() {
    Job initJob = managerInitializationJob;

    if (initJob != null) {
      initJob.cancel();
    }

    Job cleanupJob = installationCleanupJob;

    if (cleanupJob != null) {
      cleanupJob.cancel();
    }

    UpdateCore.stopUpdateManager();

  }

}
