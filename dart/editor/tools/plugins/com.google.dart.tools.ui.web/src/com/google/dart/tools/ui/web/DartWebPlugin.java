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
package com.google.dart.tools.ui.web;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The activator class controls the plug-in life cycle
 */
public class DartWebPlugin extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.dart.tools.ui.web"; //$NON-NLS-1$

  // The shared instance
  private static DartWebPlugin plugin;

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static DartWebPlugin getPlugin() {
    return plugin;
  }

  private Map<String, Color> colors = new HashMap<String, Color>();

  public static final String COLOR_COMMENTS = "org.eclipse.wst.jsdt.ui.java_multi_line_comment";
  public static final String COLOR_STRING = "org.eclipse.wst.jsdt.ui.java_string";
  public static final String COLOR_KEYWORD = "org.eclipse.wst.jsdt.ui.java_keyword";
  public static final String COLOR_STATIC_FIELD = "org.eclipse.wst.jsdt.ui.fieldHighlighting";
  public static final String COLOR_ALT_COMMENTS = "org.eclipse.wst.jsdt.ui.annotationHighlighting";

  /**
   * Get a image from this plugin's icons directory.
   * 
   * @param imagePath the image path, relative to the icons directory.
   * @return the specified image
   */
  public static Image getImage(String imagePath) {
    return getPlugin().getPluginImage(imagePath);
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

  private Map<String, Image> imageMap = new HashMap<String, Image>();

  public DartWebPlugin() {

  }

  public Color getEditorColor(String id) {
    if (colors.get(id) == null) {
      IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
      ITheme theme = themeManager.getCurrentTheme();

      RGB rgb = theme.getColorRegistry().getRGB(id);

      colors.put(id, new Color(Display.getDefault(), rgb));
    }

    return colors.get(id);
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
