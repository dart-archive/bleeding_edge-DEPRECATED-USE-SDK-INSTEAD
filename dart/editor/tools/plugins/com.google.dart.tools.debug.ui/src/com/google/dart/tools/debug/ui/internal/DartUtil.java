/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.debug.ui.internal.client.ILaunchConstants;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Utility methods for the Dart Debug UI
 */
public class DartUtil {

  /**
   * Answer the launch type specified in the given configuration
   * 
   * @param config the configuration (not <code>null</code>)
   * @return the launch type
   */
  public static int getLaunchType(ILaunchConfiguration config) {
    try {
      return config.getAttribute(ILaunchConstants.ATTR_LAUNCH_TYPE,
          ILaunchConstants.LAUNCH_TYPE_WEB_CLIENT);
    } catch (CoreException e) {
      logError(e);
      return ILaunchConstants.LAUNCH_TYPE_WEB_CLIENT;
    }
  }

  /**
   * Answer the path of the resource to be launched/opened
   * 
   * @param config the configuration (not <code>null</code>)
   * @return the resource path (not <code>null</code>)
   */
  public static String getResourcePath(ILaunchConfiguration config) {
    try {
      return config.getAttribute(ILaunchConstants.ATTR_RESOURCE_PATH, "");
    } catch (CoreException e) {
      logError(e);
      return "";
    }
  }

  /**
   * Return <code>true</code> if the given element is a Dart application file.
   * 
   * @param element the element being tested
   * @return <code>true</code> if the element is a Dart application file
   */
  public static boolean isDartApp(DartElement element) {
    // TODO(brianwilkerson) Rename this to isDartLibrary
    if (element == null) {
      return false;
    }
    return element.getElementType() == DartElement.COMPILATION_UNIT
        && ((CompilationUnit) element).definesLibrary();
  }

  /**
   * Determine if the resource is a dart application file
   * 
   * @param resource the resource (not <code>null</code>)
   * @return <code>true</code> if the resource is a Dart application
   */
  public static boolean isDartApp(IResource resource) {
    // TODO(brianwilkerson) Rename this to isDartLibrary
    if (resource == null || !resource.exists()) {
      return false;
    }
    return isDartApp(DartCore.create(resource));
  }

  /**
   * Determine if the web client should be displayed in an external web browser
   * 
   * @param config the configuration (not <code>null</code>)
   * @return <code>true</code> if the page should be displayed in an external web browser or
   *         <code>false</code> if the page should be displayed in a web browser embedded in
   *         Eclipse.
   */
  public static boolean isExternalBrowser(ILaunchConfiguration config) {
    try {
      return config.getAttribute(ILaunchConstants.ATTR_EXTERNAL_BROWSER, false);
    } catch (CoreException e) {
      logError(e);
      return false;
    }
  }

  /**
   * Determine if the resource is a web page
   * 
   * @param resource the resource (not <code>null</code>)
   * @return <code>true</code> if the resource is a web page
   */
  public static boolean isWebPage(IResource resource) {
    if (resource == null) {
      return false;
    }
    String fileExt = resource.getFileExtension();
    if (fileExt == null) {
      return false;
    }
    return fileExt.equalsIgnoreCase("html") || fileExt.equalsIgnoreCase("htm");
  }

  /**
   * Log an error message
   * 
   * @param message the error messsage
   */
  public static void logError(String message) {
    logError(new CoreException(new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID, message)));
  }

  /**
   * Log the specified error to the Eclipse error log
   * 
   * @param e the exception
   */
  public static void logError(Throwable e) {
    // TODO (danrubel) show error to user and log
    DartDebugUIPlugin.getDefault().getLog().log(
        new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID, "Exception Occurred", e));
  }

  /**
   * Simple debugging utility that echos the arguments and a stack trace to the console... used when
   * fleshing out the debug functionality.
   * 
   * @param args the arguments to echo
   */
  // TODO (danrubel) Remove references and delete method
  public static void notYetImplemented(Object... args) {
//    System.err.println("===============================================");
//    if (args != null) {
//      for (Object arg : args) {
//        System.err.println(arg != null ? arg.toString() : "null");
//      }
//    }
//    try {
//      throw new RuntimeException("Tracing...");
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
  }
}
