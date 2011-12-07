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
  public static final boolean DARTLIB = isOptionTrue("debug/dartlib");
  public static final boolean WARMUP = isOptionTrue("debug/warmup");

  public static final boolean BLEEDING_EDGE = isOptionTrue("bleedingEdge");

  public static String getLibrariesPath() {
    return getOptionValue("libraries/path", "libraries");
  }

  public static String getPlatformName() {
    return getOptionValue("platform/name", "compiler");
  }

  private static String getOptionValue(String optionSuffix, String defaultValue) {
    String value = Platform.getDebugOption(DartCore.PLUGIN_ID + "/" + optionSuffix);
    if (value != null) {
      value = value.trim();
      if (value.length() > 0) {
        return value;
      }
    }
    return defaultValue;
  }

  private static boolean isOptionTrue(String optionSuffix) {
    return "true".equalsIgnoreCase(Platform.getDebugOption(DartCore.PLUGIN_ID + "/" + optionSuffix));
  }
}
