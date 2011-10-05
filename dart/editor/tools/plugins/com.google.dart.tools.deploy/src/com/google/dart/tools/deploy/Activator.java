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
package com.google.dart.tools.deploy;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.dart.tools.deploy"; //$NON-NLS-1$

  // The shared instance
  private static Activator plugin;

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

  /**
   * The constructor
   */
  public Activator() {
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

}
