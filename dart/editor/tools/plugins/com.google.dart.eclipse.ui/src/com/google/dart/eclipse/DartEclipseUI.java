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
package com.google.dart.eclipse;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole.MessageStream;
import com.google.dart.tools.ui.actions.DeployConsolePatternMatcher;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The plugin activator for the com.google.dart.eclipse.ui plugin.
 */
public class DartEclipseUI extends AbstractUIPlugin {

  /**
   * The Dart Eclipse UI plugin id.
   */
  public static final String PLUGIN_ID = "com.google.dart.eclipse.ui";

  private static DartEclipseUI plugin;

  /**
   * Get a image from this plugin's icons directory.
   * 
   * @param imagePath the image path, relative to the icons directory.
   * @return the specified image
   */
  public static Image getImage(String imagePath) {
    return getPlugin().getPluginImage(imagePath);
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + path);
  }

  /**
   * @return the plugin singleton instance
   */
  public static DartEclipseUI getPlugin() {
    return plugin;
  }

  /**
   * Log the given message as an error to the Eclipse log.
   * 
   * @param message the message
   */
  public static void logError(String message) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param message the message
   * @param exception the exception
   */
  public static void logError(String message, Throwable exception) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param exception the exception to log
   */
  public static void logError(Throwable exception) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(
          new Status(IStatus.ERROR, PLUGIN_ID, exception.getMessage(), exception));
    }
  }

  /**
   * Log the given message as a warning to the Eclipse log.
   * 
   * @param message the message to log
   */
  public static void logWarning(String message) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception as a warning in the Eclipse log.
   * 
   * @param message the message
   * @param exception the exception
   */
  public static void logWarning(String message, Throwable exception) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, exception));
    }
  }

  private Map<String, Image> imageMap = new HashMap<String, Image>();

  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;

    super.start(context);

    initConsole();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    plugin = null;
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

  private void initConsole() {
    final MessageConsole console = new MessageConsole("Dart Console", null);
    console.addPatternMatchListener(new DeployConsolePatternMatcher());
    ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] {console});

    final MessageConsoleStream stream = console.newMessageStream();
    stream.setActivateOnWrite(true);

    DartCore.getConsole().addStream(new MessageStream() {
      @Override
      public void clear() {
        console.clearConsole();
      }

      @Override
      public void print(String s) {
        stream.print(s);
      }

      @Override
      public void println() {
        stream.println();
      }

      @Override
      public void println(String s) {
        stream.println(s);
      }
    });
  }

}
