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

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Platform;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug/Tracing options for the {@link DartCore} plugin.
 * 
 * @coverage dart.tools.core
 */
public class DartCoreDebug {

  // Sparse map of all settings automatically built by calls to isOptionTrue()

  public static final Map<String, String> SPARSE_OPTION_MAP = new HashMap<String, String>(30);

  // Debugging / Tracing options  

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
  public static final boolean PERF_THREAD_CONTENTION_MONIOR = isOptionTrue("perf/threadContentionMonitor");

  // Experimental functionality options.

  public static final boolean EXPERIMENTAL = isOptionTrue("experimental")
      || CmdLineOptions.getOptions().getExperimental();

  public static final boolean ENABLE_ALT_KEY_BINDINGS = isOptionTrue("experimental/altKeyBindings");
  public static final boolean ENABLE_TESTS_VIEW = isOptionTrue("experimental/testsView");
  public static final boolean ENABLE_FORMATTER = isOptionTrue("experimental/formatter");
  public static final boolean ENABLE_NEW_FEEDBACK = isOptionTrue("experimental/feedback");
  public static final boolean ENABLE_THEMES = true; //isOptionTrue("experimental/themes");
  public static final boolean ENABLE_TAB_COLORING = isOptionTrue("experimental/tabColors");
  public static final boolean ENABLE_HTML_VALIDATION = isOptionTrue("experimental/validateHtml");
  public static final boolean ENABLE_COVERAGE = isOptionTrue("experimental/coverage");
  public static final boolean ENABLE_ANALYSIS_SERVER = isOptionTrue("experimental/analysisServer");
  public static final boolean ANALYSIS_SERVER_DEBUG = isOptionTrue("experimental/analysisServer/debug");
  public static final String ANALYSIS_SERVER_LOG_FILE = getOptionValue("experimental/analysisServer/logFile");
  public static final boolean NO_PUB_PACKAGES = isOptionTrue("experimental/noPubPackages");

  // Persistent developer settings

  public static final boolean DISABLE_MARK_OCCURRENCES = isOptionTrue("dev/disableMarkOccurrences");

  // User settings

  public static final boolean DISABLE_DARTIUM_DEBUGGER = isOptionTrue("user/disableDartiumDebugger");
  public static final boolean DISABLE_CLI_DEBUGGER = isOptionTrue("user/disableCommandLineDebugger");

  /**
   * Report each of these parameters to the provided instrumentation builder
   */
  public static void record(InstrumentationBuilder instrumentation) {
    instrumentation.metric("DEBUG_INDEX_CONTRIBUTOR", DEBUG_INDEX_CONTRIBUTOR);
    instrumentation.metric("METRICS", METRICS);
    instrumentation.metric("WARMUP", WARMUP);
    instrumentation.metric("VERBOSE", VERBOSE);
    instrumentation.metric("LOGGING_DEBUGGER", LOGGING_DEBUGGER);

    instrumentation.metric("TRACE_ARTIFACT_PROVIDER", TRACE_ARTIFACT_PROVIDER);
    instrumentation.metric("TRACE_INDEX_CONTRIBUTOR", TRACE_INDEX_CONTRIBUTOR);
    instrumentation.metric("TRACE_INDEX_PROCESSOR", TRACE_INDEX_PROCESSOR);
    instrumentation.metric("TRACE_INDEX_STATISTICS", TRACE_INDEX_STATISTICS);
    instrumentation.metric("TRACE_UPDATE", TRACE_UPDATE);

    instrumentation.metric("ENABLE_CONTENT_ASSIST_TIMING", ENABLE_CONTENT_ASSIST_TIMING);

    instrumentation.metric("PERF_TIMER", PERF_TIMER);
    instrumentation.metric("PERF_INDEX", PERF_INDEX);
    instrumentation.metric("PERF_OS_RESOURCES", PERF_OS_RESOURCES);

    instrumentation.metric("EXPERIMENTAL", EXPERIMENTAL);

    instrumentation.metric("ENABLE_ALT_KEY_BINDINGS", ENABLE_ALT_KEY_BINDINGS);
    instrumentation.metric("ENABLE_TESTS_VIEW", ENABLE_TESTS_VIEW);
    instrumentation.metric("ENABLE_FORMATTER", ENABLE_FORMATTER);
    instrumentation.metric("ENABLE_THEMES", ENABLE_THEMES);
    instrumentation.metric("ENABLE_TAB_COLORING", ENABLE_TAB_COLORING);
    instrumentation.metric("ENABLE_HTML_VALIDATION", ENABLE_HTML_VALIDATION);
    instrumentation.metric("NO_PUB_PACKAGES", NO_PUB_PACKAGES);

    instrumentation.metric("DISABLE_MARK_OCCURRENCES", DISABLE_MARK_OCCURRENCES);

    instrumentation.metric("DISABLE_DARTIUM_DEBUGGER", DISABLE_DARTIUM_DEBUGGER);
    instrumentation.metric("DISABLE_CLI_DEBUGGER", DISABLE_CLI_DEBUGGER);
  }

  /**
   * Returns a value of the option, {@code null} if not set.
   */
  private static String getOptionValue(String optionSuffix) {
    String option = DartCore.PLUGIN_ID + "/" + optionSuffix;
    String value = Platform.getDebugOption(option);
    if (value == null) {
      value = DartCore.getUserDefinedProperty(option);
    }
    return value;
  }

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
    String value = getOptionValue(optionSuffix);
    if (value != null && !"false".equals(value)) {
      SPARSE_OPTION_MAP.put(optionSuffix, value);
    }
    return StringUtils.equalsIgnoreCase(value, expected);
  }

}
