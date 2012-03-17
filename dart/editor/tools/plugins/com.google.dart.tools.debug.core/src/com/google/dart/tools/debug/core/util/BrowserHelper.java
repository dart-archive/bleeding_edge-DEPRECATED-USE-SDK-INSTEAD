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

package com.google.dart.tools.debug.core.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import java.io.File;
import java.io.IOException;

/**
 * A utility class to perform OS specific browser functionality.
 */
public class BrowserHelper {

  public static void activateApplication(File application) {
    if (DartCore.isMac()) {
      activateApplicationMac(application);
    } else if (DartCore.isLinux()) {
      // This is not necessary on Linux.

    } else if (DartCore.isWindows()) {
      // This is not necessary on Windows.

    }
  }

  private static void activateApplicationMac(File application) {
    String appleScript = "tell application \"" + application.getName() + "\" to activate";

    ProcessBuilder builder = new ProcessBuilder("osascript", "-e", appleScript);

    try {
      builder.start();
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

  private BrowserHelper() {

  }

}
