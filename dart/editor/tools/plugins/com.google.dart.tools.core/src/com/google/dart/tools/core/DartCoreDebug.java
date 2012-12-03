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
package com.google.dart.tools.core;

import com.google.dart.compiler.util.apache.StringUtils;

import org.eclipse.core.runtime.Platform;

/**
 * Debug/Tracing options for the {@link DartCore} plugin.
 */
public class DartCoreDebug {
  // Debugging / Tracing options

  public static final boolean DEBUG_ANALYSIS = isOptionTrue("debug/analysis/server");
  public static final boolean DEBUG_INDEX_CONTRIBUTOR = isOptionTrue("debug/index/contributor");
  public static final boolean METRICS = isOptionTrue("debug/metrics");
  public static final boolean WARMUP = isOptionTrue("debug/warmup");
  public static final boolean VERBOSE = isOptionTrue("debug/verbose");
  public static final boolean LOGGING_DEBUGGER = isOptionTrue("logging/debugger");

  public static final boolean TRACE_ARTIFACT_PROVIDER = isOptionTrue("trace/artifactProvider");
  public static final boolean TRACE_INDEX_CONTRIBUTOR = isOptionTrue("trace/index/contributor");
  public static final boolean TRACE_INDEX_PROCESSOR = isOptionTrue("trace/index/processor");
  public static final boolean TRACE_INDEX_STATISTICS = isOptionTrue("trace/index/statistics");
  public static final boolean TRACE_UPDATE = isOptionTrue("trace/update");

  public static final boolean ENABLE_CONTENT_ASSIST_TIMING = isOptionTrue("debug/ResultCollector");

  // Performance measurement and reporting options.

  public static final boolean PERF_TIMER = isOptionTrue("perf/timer");
  public static final boolean PERF_INDEX = isOptionTrue("perf/index");
  public static final boolean PERF_OS_RESOURCES = isOptionTrue("perf/osResources");

  // Experimental functionality options.

  public static final boolean ENABLE_UPDATE = true; //isOptionTrue("experimental/update");
  public static final boolean ENABLE_ALT_KEY_BINDINGS = isOptionTrue("experimental/altKeyBindings");
  public static final boolean ENABLE_TESTS_VIEW = isOptionTrue("experimental/testsView");
  public static final boolean ENABLE_FORMATTER = isOptionTrue("experimental/formatter");
  public static final boolean ENABLE_THEMES = true; //isOptionTrue("experimental/themes");
  public static final boolean ENABLE_HTML_VALIDATION = isOptionTrue("experimental/validateHtml");
  public static final boolean ENABLE_NEW_ANALYSIS = isOptionTrue("experimental/analysis/engine");

  // Pub

  public static final boolean ENABLE_PUB_IN_WINXP = isOptionTrue("experimental/pubInWinXP");
  public static final boolean ENABLE_PUB = !DartCore.isWindowsXp() || ENABLE_PUB_IN_WINXP;

  // Persistent developer settings

  public static final boolean DISABLE_MARK_OCCURRENCES = isOptionTrue("dev/disableMarkOccurrences");

  /**
   * @return <code>true</code> if option has value "true".
   */
  private static boolean isOptionTrue(String optionSuffix) {
    return isOptionValue(optionSuffix, "true");
  }

  /**
   * @return <code>true</code> if option has "expected" value.
   */
  private static boolean isOptionValue(String optionSuffix, String expected) {
    String option = DartCore.PLUGIN_ID + "/" + optionSuffix;
    String value = Platform.getDebugOption(option);
    if (value == null) {
      value = DartCore.getUserDefinedProperty(option);
    }
    return StringUtils.equalsIgnoreCase(value, expected);
  }

}
