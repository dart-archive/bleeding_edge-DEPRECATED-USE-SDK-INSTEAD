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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Utility methods for the Dart Debug UI
 */
public class DartUtil {

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
    DartDebugUIPlugin.getDefault().getLog().log(
        new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID, e.toString(), e));
  }

}
