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
package com.google.dart.dev.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The Dart Dev plugin.
 */
public class DartDevPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.google.dart.dev_util"; //$NON-NLS-1$

  private static Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

  /**
   * The shared instance.
   */
  private static DartDevPlugin PLUGIN;

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

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + path);
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

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    PLUGIN = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    PLUGIN = null;
    super.stop(context);
  }

}
