/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core;

import org.eclipse.core.runtime.Platform;

/**
 * Debug/Tracing options for the {@link DartCore} plugin
 */
public class DartCoreDebug {

  // Debugging / Tracing options

  public static final boolean BUILD = isOptionTrue("debug/build");
  public static final boolean WARMUP = isOptionTrue("debug/warmup");

  /**
   * Echo the specified message to the log
   */
  public static void log(String message) {
    DartCore.logInformation(message, null);
  }

  private static boolean isOptionTrue(String optionSuffix) {
    return "true".equalsIgnoreCase(Platform.getDebugOption(DartCore.PLUGIN_ID + "/" + optionSuffix));
  }

  /**
   * Log the specified message if the condition is true
   * 
   * @param condition <code>true</code> if the message should be logged
   * @param message the message
   */
  public static void log(boolean condition, String message) {
    if (condition) {
      log(message);
    }
  }
}
