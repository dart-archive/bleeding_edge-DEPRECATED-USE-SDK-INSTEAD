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
package com.google.dart.tools.debug.ui.internal;

import com.google.dart.tools.debug.ui.internal.presentation.DebugElementAdapterFactory;
import com.google.dart.tools.debug.ui.internal.view.DebuggerViewManager;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The activator class controls the plug-in life cycle
 */
public class DartDebugUIPlugin extends AbstractUIPlugin {
  /**
   * The Dart Debug UI plug-in ID
   */
  public static final String PLUGIN_ID = "com.google.dart.tools.debug.ui"; //$NON-NLS-1$

  public static final String CHECK_MODE_DESC_URL = "http://www.dartlang.org/articles/optional-types/#checked-mode";

  private static Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

  public static Image getImage(ImageDescriptor imageDescriptor) {
    Image image = imageCache.get(imageDescriptor);

    if (image == null) {
      image = imageDescriptor.createImage();

      imageCache.put(imageDescriptor, image);
    }

    return image;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + path);
  }

  private Map<String, Image> imageMap;

  /**
   * The shared instance
   */
  private static DartDebugUIPlugin plugin;

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static DartDebugUIPlugin getDefault() {
    return plugin;
  }

  /**
   * Get a image from this plugin's icons directory.
   * 
   * @param imagePath the image path, relative to the icons directory.
   * @return the specified image
   */
  public static Image getImage(String imagePath) {
    return getDefault().getPluginImage(imagePath);
  }

  /**
   * Called when the bundle is first started
   */
  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;

    imageMap = new HashMap<String, Image>();

    super.start(context);

    DebugElementAdapterFactory.init();

    // Initialize the DebuggerViewManager.
    DebuggerViewManager.getDefault();

    // Install our user agent manager.
    DartDebugUserAgentManager.install();
  }

  /**
   * Called when the bundle is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    DebuggerViewManager.dispose();

    super.stop(context);

    disposeImageCache();

    plugin = null;
  }

  private void disposeImageCache() {
    for (Image image : imageMap.values()) {
      image.dispose();
    }

    imageMap = null;
  }

  private Image getPluginImage(String imagePath) {
    if (imageMap.get(imagePath) == null) {
      ImageDescriptor imageDescriptor = imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + imagePath);

      if (imageDescriptor != null) {
        imageMap.put(imagePath, imageDescriptor.createImage());
      }
    }

    return imageMap.get(imagePath);
  }

}
