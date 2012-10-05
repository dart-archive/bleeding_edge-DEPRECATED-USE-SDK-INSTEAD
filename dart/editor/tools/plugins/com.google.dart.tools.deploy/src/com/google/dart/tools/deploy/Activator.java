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
import com.google.dart.tools.update.core.UpdateAdapter;
import com.google.dart.tools.update.core.UpdateListener;
import com.google.dart.tools.update.core.UpdateManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.dart.tools.deploy"; //$NON-NLS-1$

  // The shared instance
  private static Activator plugin;

  private static Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

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

  //a hook into the update lifecycle
  private final UpdateListener updateListener = new UpdateAdapter() {

    @Override
    public void installing() {

      //terminate all running dart launches 
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      for (ILaunch launch : launchManager.getLaunches()) {
        if (!launch.isTerminated() && isDartLaunch(launch) && launch.canTerminate()) {
          terminate(launch);
        }
      }

    }

    public boolean isDartLaunch(ILaunch launch) {
      try {
        return launch.getLaunchConfiguration().getType().getIdentifier().startsWith("com.google");
      } catch (CoreException e) {
        logError(e);
      }
      return false;
    }

    public void terminate(ILaunch launch) {
      try {
        launch.terminate();
      } catch (DebugException e) {
        logError(e);
      }
    }
  };

  /**
   * The constructor
   */
  public Activator() {
  };

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    plugin = this;

    DartConsoleManager.initialize();

    UpdateManager.getInstance().addListener(updateListener);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    DartConsoleManager.shutdown();

    UpdateManager.getInstance().removeListener(updateListener);

    plugin = null;

    super.stop(context);
  }

}
